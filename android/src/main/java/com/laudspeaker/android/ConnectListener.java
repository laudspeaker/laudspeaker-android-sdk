package com.laudspeaker.android;

public interface ConnectListener {
    void onConnected();
    void onDisconnected();
    void onError();
}
