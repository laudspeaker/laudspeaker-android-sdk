package com.laudspeaker.android;


import android.content.Context;

import java.io.File;


public class LaudspeakerAndroid {
    public static <T extends LaudspeakerAndroidConfig> Laudspeaker with(Context context, T config) {
        setAndroidConfig(context.getApplicationContext(), config);
        return Laudspeaker.with(config);
    }

    private static <T extends LaudspeakerAndroidConfig> void setAndroidConfig(Context context, T config) {
        if (config.getLogger() instanceof LaudspeakerLogger) {
            config.setLogger(new LaudspeakerLogger(config));
        }

        File path = new File(context.getCacheDir(), "laudspeaker-disk-queue");
        config.setStoragePrefix(config.getStoragePrefix() == null ? path.getAbsolutePath() : config.getStoragePrefix());
        LaudspeakerPreferences preferences = config.getCachePreferences() == null ? new LaudspeakerPreferences(context) : config.getCachePreferences();
        config.setCachePreferences(preferences);
        config.setNetworkStatus(config.getNetworkStatus() == null ? new LaudspeakerNetworkStatus(context) : config.getNetworkStatus());
        config.setSdkVersion("1");
        config.setSdkName("laudspeaker-android");
    }
}
