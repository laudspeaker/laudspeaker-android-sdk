package com.laudspeaker.android;

public class LaudspeakerAndroidConfig extends LaudspeakerConfig {


    public LaudspeakerAndroidConfig(Class<?> targetActivityClass) {
        this(defaultKey, defaultHost, targetActivityClass, false, false, false);
    }

    public LaudspeakerAndroidConfig(String apiKey, Class<?> targetActivityClass) {
        this(apiKey, defaultHost, targetActivityClass, true, true, true);
    }


    public LaudspeakerAndroidConfig(String apiKey, String host, Class<?> targetActivityClass) {
        this(apiKey, host, targetActivityClass, true, true, true);
    }

    public LaudspeakerAndroidConfig(String apiKey, String host, Class<?> targetActivityClass, boolean updatedKey, boolean updatedHost, boolean updatedClass) {
        super(apiKey, host, targetActivityClass, updatedKey, updatedHost, updatedClass);
    }


}