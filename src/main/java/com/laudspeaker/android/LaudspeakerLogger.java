package com.laudspeaker.android;

import com.laudspeaker.android.LaudspeakerConfig;

/**
 * Logs the messages using System.out only if config.debug is enabled
 */
public class LaudspeakerLogger {
    private final LaudspeakerConfig config;

    public LaudspeakerLogger(LaudspeakerConfig config) {
        this.config = config;
    }
    public void log(String message) {
        if (isEnabled()) {
            System.out.println("[Laudspeaker SDK]: " + message);
        }
    }
    public boolean isEnabled() {
        return config.isDebug();
    }
}