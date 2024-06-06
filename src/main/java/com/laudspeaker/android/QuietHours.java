package com.laudspeaker.android;

public class QuietHours {
    private boolean enabled;
    private String endTime;
    private String startTime;
    private String fallbackBehavior;

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getFallbackBehavior() {
        return fallbackBehavior;
    }

    public void setFallbackBehavior(String fallbackBehavior) {
        this.fallbackBehavior = fallbackBehavior;
    }
}
