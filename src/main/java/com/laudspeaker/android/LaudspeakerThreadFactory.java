package com.laudspeaker.android;

import java.util.concurrent.ThreadFactory;

public class LaudspeakerThreadFactory implements ThreadFactory {
    private final String threadName;

    public LaudspeakerThreadFactory(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName(threadName);
        return thread;
    }
}