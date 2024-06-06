package com.laudspeaker.android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class LaudspeakerQueue {
    private final LaudspeakerConfig config;
    private final LaudspeakerApi api;
    private final LaudspeakerApiEndpoint endpoint;
    private final String storagePrefix;
    private final ExecutorService executor;
    private final Deque<File> deque = new ArrayDeque<>();
    private final Object dequeLock = new Object();
    private final Object timerLock = new Object();
    private Date pausedUntil = null;
    private int retryCount = 0;
    private final int retryDelaySeconds = 5;
    private final int maxRetryDelaySeconds = 30;
    private volatile Timer timer = null;
    private volatile TimerTask timerTask = null;
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);
    private boolean dirCreated = false;

    private long getDelay() {
        return (long) config.getFlushIntervalSeconds() * 1000;
    }

    public LaudspeakerQueue(LaudspeakerConfig config, LaudspeakerApi api, LaudspeakerApiEndpoint endpoint, String storagePrefix, ExecutorService executor) {
        this.config = config;
        this.api = api;
        this.endpoint = endpoint;
        this.storagePrefix = storagePrefix;
        this.executor = executor;
    }

    public void add(LaudspeakerEvent event) {
        executor.execute(() -> {
            boolean removeFirst = false;
            if (deque.size() >= config.getMaxQueueSize()) {
                removeFirst = true;
            }

            if (removeFirst) {
                try {
                    File first;
                    synchronized (dequeLock) {
                        first = deque.removeFirst();
                    }
                    first.delete();
                    config.getLogger().log("Queue is full, the oldest event " + first.getName() + " is dropped.");
                } catch (NoSuchElementException ignored) {
                }
            }


            if (storagePrefix != null) {
                File dir = new File(storagePrefix, config.getApiKey());

                if (!dirCreated) {
                    dir.mkdirs();
                    dirCreated = true;
                }

                File file = new File(dir, UUID.randomUUID().toString() + ".event");
                synchronized (dequeLock) {
                    deque.add(file);
                }


                try {
                    OutputStream os = new FileOutputStream(file);

                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
                        config.getSerializer().toJson(event, writer);
                        writer.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    config.getLogger().log("Queued event " + file.getName() + ".");
                    flushIfOverThreshold();
                } catch (Throwable e) {
                    config.getLogger().log("Event " + event.getEvent() + " failed to parse: " + e + ".");
                }
            }
        });
    }

    private void flushIfOverThreshold() {
        if (isAboveThreshold(config.getFlushAt())) {
            flushBatch();
        }
    }

    private boolean isAboveThreshold(int flushAt) {
        return deque.size() >= flushAt;
    }


    private boolean canFlushBatch() {
        synchronized (config) {
            if (pausedUntil != null && pausedUntil.after(config.getDateProvider().currentDate())) {
                config.getLogger().log("Queue is paused until " + pausedUntil);
                return false;
            }
        }
        return true;
    }

    private List<File> takeFiles() {
        List<File> events = new ArrayList<>();
        synchronized (dequeLock) {
            int count = 0;
            while (!deque.isEmpty() && count < config.getMaxBatchSize()) {
                events.add(deque.removeFirst());
                count++;
            }
        }
        return events;
    }

    private void flushBatch() {
        if (!canFlushBatch()) {
            config.getLogger().log("Cannot flush the Queue.");
            return;
        }

        if (isFlushing.getAndSet(true)) {
            config.getLogger().log("Queue is flushing.");
            return;
        }

        try {
            executeBatch();
        } finally {
            isFlushing.set(false);
        }
    }

    private void executeBatch() {
        if (!isConnected()) {
            isFlushing.set(false);
            return;
        }

        boolean retry = false;
        try {
            batchEvents();
            retryCount = 0;
        } catch (Exception e) {
            config.getLogger().log("Flushing failed: " + e.getMessage());
            retry = true;
            retryCount++;
        } finally {
            calculateDelay(retry);
            isFlushing.set(false);
        }
    }


    private void batchEvents() throws LaudspeakerApiError, IOException {
        List<File> files = takeFiles();
        List<LaudspeakerEvent> events = new ArrayList<>();
        for (File file : files) {
            try (FileReader fileReader = new FileReader(file)) {
                LaudspeakerEvent event = config.getSerializer().fromJson(fileReader, LaudspeakerEvent.class);
                if (event != null) {
                    event.setFCMToken((String) config.getCachePreferences().getValue(LaudspeakerPreferences.FCM_TOKEN, null));
                    if (Objects.equals(event.getEvent(), "$delivered") || Objects.equals(event.getEvent(), "$opened")) {
                        event.setSource("message");
                    } else {
                        event.setSource("mobile");
                    }
                    events.add(event);
                }
            } catch (Exception e) {
                config.getLogger().log("Failed to add event: " + e.getMessage());
                synchronized (dequeLock) {
                    deque.remove(file);
                }
                boolean deleted = file.delete();
                if (!deleted) {
                    config.getLogger().log("Failed to delete file: " + file.getName());
                }
                config.getLogger().log("Failed to parse file: " + file.getName() + ", Error: " + e.getMessage());
            }
        }

        boolean deleteFiles = true;
        if (!events.isEmpty()) {
            try {
                api.send(events);
            } catch (LaudspeakerApiError e) {
                if (e.getStatusCode() < 400) {
                    deleteFiles = false;
                }
                throw e;
            } catch (IOException e) {
                deleteFiles = false;
                throw e;
            } finally {
                if (deleteFiles) {
                    synchronized (dequeLock) {
                        deque.removeAll(files);
                    }
                    files.forEach(file -> {
                        file.delete();
                    });
                }
            }
        }
    }

    public void flush() {
        // only flushes if the queue is above the threshold (not empty in this case)
        if (!isAboveThreshold(1)) {
            return;
        }

        if (isFlushing.getAndSet(true)) {
            config.getLogger().log("Queue is flushing.");
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!isConnected()) {
                    isFlushing.set(false);
                    return;
                }

                boolean retry = false;
                try {
                    while (!deque.isEmpty()) {
                        batchEvents();
                    }
                    retryCount = 0;
                } catch (Throwable e) {
                    config.getLogger().log("Flushing failed: " + e.getMessage());
                    retry = true;
                    retryCount++;
                } finally {
                    calculateDelay(retry);
                    isFlushing.set(false);
                }
            }
        });
    }

    private boolean isConnected() {
        LaudspeakerNetworkStatus networkStatus = config.getNetworkStatus();
        if (networkStatus != null && !networkStatus.isConnected()) {
            config.getLogger().log("Network isn't connected.");
            return false;
        }
        return true;
    }

    private void calculateDelay(boolean retry) {
        if (retry) {
            int delay = Math.min(retryCount * retryDelaySeconds, maxRetryDelaySeconds);
            pausedUntil = config.getDateProvider().addSecondsToCurrentDate(delay);
        }
    }

    public void start() {
        synchronized (timerLock) {
            stopTimer();
            timer = new Timer(true);

            timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (isFlushing.get()) {
                        config.getLogger().log("Queue is flushing.");
                        return;
                    }
                    flush();
                }
            };

            timer.schedule(this.timerTask, this.getDelay(), this.getDelay());
        }
    }

    private void stopTimer() {
        if (timerTask != null) timerTask.cancel();
        if (timer != null) timer.cancel();
        ;
    }

    public void stop() {
        synchronized (timerLock) {
            stopTimer();
        }
    }

    public void clear() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<File> tempFiles;
                synchronized (dequeLock) {
                    tempFiles = new ArrayList<>(deque);
                    deque.clear();
                }
                for (File file : tempFiles) {
                    file.delete();
                }
            }
        });
    }

    public List<File> getDequeList() {
        synchronized (dequeLock) {
            return new ArrayList<>(deque);
        }
    }
}