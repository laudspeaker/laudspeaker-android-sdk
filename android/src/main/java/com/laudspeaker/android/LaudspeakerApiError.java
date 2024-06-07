package com.laudspeaker.android;

import okhttp3.ResponseBody;

public class LaudspeakerApiError extends RuntimeException {
    private final int statusCode;
    private final ResponseBody body;

    public LaudspeakerApiError(int statusCode, String message, ResponseBody body) {
        super(message);
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ResponseBody getBody() {
        return body;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    public String toString() {
        return "LaudspeakerApiError(statusCode=" + statusCode + ", message='" + getMessage() + "')";
    }
}