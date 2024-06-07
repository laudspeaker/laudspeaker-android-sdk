package com.laudspeaker.android;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LaudspeakerPreferences {
    // Constants
    public static final String PREFERENCES_FILE_KEY = "com.laudspeaker.android.PREFERENCES";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String PRIMARY_KEY = "primary_key";
    public static final String FCM_TOKEN = "fcm_token";
    public static final String VERSION = "version";
    public static final String BUILD = "build";
    public static final String HOST = "host";
    public static final String API_KEY = "api_key";
    public static final String ACTIVITY_CLASS = "activity_class";
    public static final Set<String> ALL_INTERNAL_KEYS = Set.of(CUSTOMER_ID, PRIMARY_KEY, FCM_TOKEN, VERSION, BUILD, HOST, API_KEY, ACTIVITY_CLASS);
    private final SharedPreferences preferences;

    public LaudspeakerPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE);
    }

    public Object getValue(String key, Object defaultValue) {
        // Since SharedPreferences does not directly support null, we need to handle it.
        // Checking if the preference exists. If it does not, return the default value (which can be null).
        if (!preferences.contains(key)) {
            return defaultValue;
        }

        // Assuming all values are stored as strings, given the limitations of SharedPreferences.
        // Implement type-specific logic if needed.
        return preferences.getString(key, defaultValue != null ? defaultValue.toString() : null);
    }

    public void setValue(String key, Object value) {
        SharedPreferences.Editor editor = preferences.edit();
        // Similar to the getValue method, checks for specific data types can be implemented.
        // For simplicity, let's convert everything to String.
        editor.putString(key, value.toString());
        editor.apply();
    }

    public void clear(List<String> exceptKeys) {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (!exceptKeys.contains(key)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public void remove(String key) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public Map<String, ?> getAll() {
        // Filter out internal keys
        Map<String, ?> allEntries = preferences.getAll();
        allEntries.keySet().removeAll(ALL_INTERNAL_KEYS);
        return allEntries;
    }

    public void setTargetActivityClass(Class<?> targetActivityClass) {
        if (targetActivityClass != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ACTIVITY_CLASS, targetActivityClass.getName());
            editor.apply();
        }
    }

    // Method to retrieve the target activity class
    public Class<?> getTargetActivityClass() {
        String className = preferences.getString(ACTIVITY_CLASS, null);
        if (className != null) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                // Handle the error or return null
                return null;
            }
        }
        return null;
    }
}