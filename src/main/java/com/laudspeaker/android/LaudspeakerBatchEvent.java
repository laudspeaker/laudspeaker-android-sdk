package com.laudspeaker.android;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

public class LaudspeakerBatchEvent {
    private final List<LaudspeakerEvent> batch;

    @SerializedName("sent_at")
    private Date sentAt;

    public LaudspeakerBatchEvent(List<LaudspeakerEvent> batch) {
        this.batch = batch;
        this.sentAt = null;
    }

    public List<LaudspeakerEvent> getBatch() {
        return batch;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }
}
