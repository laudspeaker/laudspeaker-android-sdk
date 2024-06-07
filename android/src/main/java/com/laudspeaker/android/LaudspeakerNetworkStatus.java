package com.laudspeaker.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class LaudspeakerNetworkStatus {
    private final Context context;

    public LaudspeakerNetworkStatus(Context context) {
        this.context = context;
    }

    public boolean isConnected() {
        return isConnected(context);
    }

    private static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }
}