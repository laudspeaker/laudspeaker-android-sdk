package com.laudspeaker.android;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.io.File;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Laudspeaker extends FirebaseMessagingService {
    private static int notificationIconResId = com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_dark; // Default icon in the library
    private final ExecutorService queueExecutor = Executors.newSingleThreadScheduledExecutor(new LaudspeakerThreadFactory("LaudspeakerQueueThread"));
    private final Object setupLock = new Object();
    private final Object customerIdLock = new Object();
    private final Object primaryKeyLock = new Object();
    private final Object fcmTokenLock = new Object();
    public volatile boolean enabled = false;
    private LaudspeakerConfig config;
    private LaudspeakerQueue queue;
    private LaudspeakerPreferences memoryPreferences;
    private String customerIdCache;
    private String primaryKeyCache;
    private String fcmTokenCache;
    private String apiKey;


    public static <T extends LaudspeakerConfig> Laudspeaker with(T config) {
        Laudspeaker instance = new Laudspeaker(); // Assuming there's a default constructor or appropriate constructor available
        instance.setup(config);
        instance.getFcmTokenAsync(new FcmTokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                if (token != null && !token.trim().isEmpty() && config != null) {
                    config.getLogger().log("Retrieved FCM token: " + token);
                } else {
                    if (config != null) {
                        config.getLogger().log("getFCMToken called but token was empty.");
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                if (config != null) {
                    config.getLogger().log("Failed to fetch FCM token: " + exception.toString());
                }
            }
        });
        return instance;
    }

    public <T extends LaudspeakerConfig> void setup(T config) {
        synchronized (setupLock) {
            try {
                if (enabled) {
                    config.getLogger().log("Setup called despite already being setup!");
                    return;
                }

                if (apiKey == config.getApiKey()) {
                    config.getLogger().log("API Key: " + config.getApiKey() + " already has a Laudspeaker instance.");
                }

                this.memoryPreferences = config.getCachePreferences();
                LaudspeakerApi api = new LaudspeakerApi(config);
                this.queue = new LaudspeakerQueue(config, api, LaudspeakerApiEndpoint.EVENT, config.getStoragePrefix(), queueExecutor);

                this.config = config;

                if (config.getUpdatedHost()) {
                    this.memoryPreferences.setValue(LaudspeakerPreferences.HOST, config.getHost());
                }
                if (config.getUpdatedKey()) {
                    this.memoryPreferences.setValue(LaudspeakerPreferences.API_KEY, config.getApiKey());
                }
                if (config.getUpdatedClass()) {
                    this.memoryPreferences.setTargetActivityClass(config.getTargetActivityClass());
                }

                this.enabled = true;

                queue.start();

            } catch (Throwable e) {
                config.getLogger().log("Setup failed: " + e);
            }
        }
    }

    public void setNotificationIcon(int resId) {
        notificationIconResId = resId;
    }

    public int getNotificationIconResId() {
        return notificationIconResId;
    }

    public void handlePushOpened(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            Map<String, Object> openMessage = new HashMap<>();
            openMessage.put("customerID", intent.getStringExtra("customerID"));
            openMessage.put("stepID", intent.getStringExtra("stepID"));
            openMessage.put("templateID", intent.getStringExtra("templateID"));
            openMessage.put("messageID", intent.getStringExtra("messageID"));
            openMessage.put("workspaceID", intent.getStringExtra("workspaceID"));
            this.capture("$opened", openMessage);
        }
    }

    public String getCustomerId() {
        synchronized (customerIdLock) {
            if (customerIdCache == null || customerIdCache.isEmpty()) {
                Object value = getPreferences().getValue(LaudspeakerPreferences.CUSTOMER_ID, null);
                if (value instanceof String && !((String) value).isEmpty()) {
                    customerIdCache = (String) value;
                } else {
                    customerIdCache = UUID.randomUUID().toString();
                    setCustomerId(customerIdCache);
                }
            }
            return customerIdCache;
        }
    }

    public void setCustomerId(String value) {
        synchronized (customerIdLock) {
            getPreferences().setValue(LaudspeakerPreferences.CUSTOMER_ID, value);
            this.customerIdCache = value; // Cache the value to avoid fetching it repeatedly
        }
    }


    public String getPrimaryKey() {
        synchronized (primaryKeyLock) {
            if (primaryKeyCache == null || primaryKeyCache.isEmpty() || primaryKeyCache == "") {

                Object value = getPreferences().getValue(LaudspeakerPreferences.PRIMARY_KEY, getCustomerId());

                if (value instanceof String) {
                    primaryKeyCache = (String) value;
                } else {
                    primaryKeyCache = ""; // Default to empty string if value is not a string
                }
            }
        }
        return primaryKeyCache;
    }

    public void setPrimaryKey(String value) {
        synchronized (primaryKeyLock) {
            getPreferences().setValue(LaudspeakerPreferences.PRIMARY_KEY, value);
            this.primaryKeyCache = value; // Update the cache
        }
    }

    // Modify getFcmToken to use the callback
    public void getFcmTokenAsync(FcmTokenCallback callback) {
        synchronized (fcmTokenLock) {
            if (fcmTokenCache == null || fcmTokenCache.isEmpty()) {
                Object value = getPreferences().getValue(LaudspeakerPreferences.FCM_TOKEN, null);
                if (value instanceof String && !((String) value).isEmpty()) {
                    fcmTokenCache = (String) value;
                    callback.onTokenReceived(fcmTokenCache);
                } else {
                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            config.getLogger().log("Fetching FCM registration token failed: " + e);
//                            callback.onError(e);
                        } else {
                            // Assuming the token is successfully retrieved and not null
                            fcmTokenCache = task.getResult();
                            config.getLogger().log("Retrieved FCM Token: " + fcmTokenCache);
                            setFcmToken(fcmTokenCache); // Make sure to implement this method to save the token
                            callback.onTokenReceived(fcmTokenCache);
                        }
                    });
                }
            } else {
                // If the token is already cached, return it immediately
                callback.onTokenReceived(fcmTokenCache);
            }
        }
    }

    public void setFcmToken(String value) {
        synchronized (fcmTokenLock) {
            getPreferences().setValue(LaudspeakerPreferences.FCM_TOKEN, value);
            this.fcmTokenCache = value; // Update the cache
        }
    }

    public LaudspeakerPreferences getPreferences() {
        return memoryPreferences;
    }

    private boolean isEnabled() {
        if (!enabled) {
            if (config != null) config.getLogger().log("Setup isn't called.");
        }
        return enabled;
    }

    public void capture(String event, Map<String, Object> properties) {
        try {
            if (!isEnabled()) {
                config.getLogger().log("capture call not allowed, Laudspeaker instance not enabled.");
                return;
            }

            String customerId = getCustomerId();

            if (customerId == null || customerId.trim().isEmpty()) {
                if (config != null) {
                    config.getLogger().log("capture call not allowed, customer ID is invalid: " + customerId);
                }
                return;
            }

            Map<String, Object> mergedProperties = buildProperties(properties);
            Map<String, Object> sanitizedProperties = config != null && config.getPropertiesSanitizer() != null ? config.getPropertiesSanitizer().sanitize(mergedProperties) : mergedProperties;

            LaudspeakerEvent laudspeakerEvent = new LaudspeakerEvent(event, customerId, sanitizedProperties);


            if (queue != null) {
                queue.add(laudspeakerEvent);
            }

        } catch (Throwable e) {
            if (config != null) {
                config.getLogger().log("Capture failed: " + e);
            }
        }
    }

    public void identify(String primaryKey, Map<String, Object> userProperties) {

        if (!isEnabled()) {
            return;
        }

        Map<String, Object> props = userProperties == null ? new HashMap<>() : userProperties;


        if (primaryKey == null || primaryKey.trim().isEmpty()) {
            if (config != null) {
                config.getLogger().log("identify call not allowed, primary key is invalid: " + primaryKey);
            }
            return;
        } else {
            props.put("distinct_id", primaryKey);
        }

        String previousPrimaryKey = getPrimaryKey();

        capture("$identify", props);

        // Check if primary key being set is the same as previously set
        if (!previousPrimaryKey.equals(primaryKey)) {
            setPrimaryKey(primaryKey);
        }

    }

    public void set(Map<String, Object> userProperties) {

        if (!isEnabled()) {
            return;
        }

        capture("$set", userProperties);
    }

    public void sendFcmTokenAsync() {
        if (!isEnabled()) {
            return;
        }

        getFcmTokenAsync(new FcmTokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                if (token != null && !token.trim().isEmpty()) {
                    Map<String, Object> props = new HashMap<>();
                    props.put("androidDeviceToken", token);
                    capture("$fcm", props);
                } else {
                    if (config != null) {
                        config.getLogger().log("sendFcmToken called but token was empty.");
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                if (config != null) {
                    config.getLogger().log("Failed to fetch FCM token: " + exception.toString());
                }
            }
        });
    }

    private Map<String, Object> buildProperties(Map<String, Object> properties) {

        Map<String, Object> props = new HashMap<>();

        if (properties != null) {
            props.putAll(properties);
        }
        return props;
    }

    public void close() {
        synchronized (setupLock) {
            try {
                enabled = false;

                if (config != null) {
                    apiKey = null;
                }

                if (queue != null) {
                    queue.stop();
                }

            } catch (Throwable e) {
                if (config != null) {
                    config.getLogger().log("Close failed: " + e);
                }
            }
        }
    }

    public void reset() {
        if (!isEnabled()) {
            return;
        }

        List<String> except = Arrays.asList(LaudspeakerPreferences.VERSION, LaudspeakerPreferences.BUILD);
        getPreferences().clear(except);
        if (queue != null) {
            queue.clear();
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            handleDataMessage(data);
        }

        if (remoteMessage.getNotification() != null) {
            String messageBody = remoteMessage.getNotification().getBody();
            handleNotification(messageBody);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Notification Channel";
            String description = "Channel description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /*
    WARNING:DO NOT USE ANY DEFAULT-NULL CLASS VARIABLES HERE
     */
    private void handleDataMessage(Map<String, String> data) {
        boolean isQuietHour = false;

        Gson gson = new Gson();
        QuietHours quietHours = gson.fromJson(data.get("quietHours"), QuietHours.class);

        System.out.println(quietHours);

        if (quietHours != null) {
            String utcStartTime = convertTimeToUTC(quietHours.getStartTime(), 0);
            String utcEndTime = convertTimeToUTC(quietHours.getEndTime(), 0);

            ZonedDateTime now = ZonedDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String utcNowString = now.format(formatter);

            isQuietHour = isWithinInterval(utcStartTime, utcEndTime, utcNowString);
        }

        if (isQuietHour) return;

        Map<String, Object> deliveryMessage = new HashMap<>();
        deliveryMessage.put("customerID", data.get("customerID"));
        deliveryMessage.put("stepID", data.get("stepID"));
        deliveryMessage.put("templateID", data.get("templateID"));
        deliveryMessage.put("messageID", data.get("messageID"));
        deliveryMessage.put("workspaceID", data.get("workspaceID"));

        Context context = this.getApplicationContext();
        LaudspeakerAndroidConfig config = new LaudspeakerAndroidConfig(null);
        config.setLogger(new LaudspeakerLogger(config));
        File path = new File(context.getCacheDir(), "laudspeaker-disk-queue");
        System.out.println("The path for the autoinstance is " + path.toString());
        config.setStoragePrefix(config.getStoragePrefix() == null ? path.getAbsolutePath() : config.getStoragePrefix());
        LaudspeakerPreferences preferences = config.getCachePreferences() == null ? new LaudspeakerPreferences(context) : config.getCachePreferences();
        config.setCachePreferences(preferences);
        config.setNetworkStatus(config.getNetworkStatus() == null ? new LaudspeakerNetworkStatus(context) : config.getNetworkStatus());
        config.setSdkVersion("1");
        config.setSdkName("laudspeaker-android");
        this.setup(config);
        this.capture("$delivered", deliveryMessage);

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID").setSmallIcon(this.getNotificationIconResId()).setContentTitle(data.get("title")).setContentText(data.get("body")).setPriority(NotificationCompat.PRIORITY_MAX);

        Intent intent = new Intent(this, this.config.getCachePreferences().getTargetActivityClass());
        intent.putExtra("customerID", data.get("customerID"));
        intent.putExtra("stepID", data.get("stepID"));
        intent.putExtra("templateID", data.get("templateID"));
        intent.putExtra("messageID", data.get("messageID"));
        intent.putExtra("workspaceID", data.get("workspaceID"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        int notificationId = (int) System.currentTimeMillis();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(notificationId, builder.build());
    }

    private static String convertTimeToUTC(String localTime, int utcOffsetMinutes) {
        // This method should convert local time to UTC based on the utcOffsetMinutes.
        // Placeholder implementation. The real implementation will depend on how the times are represented.
        LocalTime time = LocalTime.parse(localTime);
        return time.minusMinutes(utcOffsetMinutes).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private static boolean isWithinInterval(String startTime, String endTime, String currentTime) {
        // This method checks if currentTime is within the interval [startTime, endTime].
        // Note: This simplistic implementation may not handle over-midnight spans correctly.
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        LocalTime current = LocalTime.parse(currentTime);

        if (start.isBefore(end)) {
            return !current.isBefore(start) && !current.isAfter(end);
        } else { // Handles the over-midnight case
            return !current.isBefore(start) || !current.isAfter(end);
        }
    }

    /*
    WARNING:DO NOT USE ANY DEFAULT-NULL CLASS VARIABLES HERE
     */
    private void handleNotification(String messageBody) {
        System.out.println("Got a notification message:" + messageBody.toString());
    }


    // Define a callback interface
    public interface FcmTokenCallback {
        void onTokenReceived(String token);

        void onError(Exception exception);
    }
}