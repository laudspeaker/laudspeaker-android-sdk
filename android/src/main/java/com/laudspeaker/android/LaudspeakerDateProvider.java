package com.laudspeaker.android;

import java.util.Calendar;
import java.util.Date;

public class LaudspeakerDateProvider {

    public Date currentDate() {
        Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }

    public Date addSecondsToCurrentDate(int seconds) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, seconds);
        return cal.getTime();
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long nanoTime() {
        return System.nanoTime();
    }
}