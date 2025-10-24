package com.evcharging.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {
    private static final String PREF_NAME = "EVChargingPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_NIC = "user_nic";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final String KEY_LAST_ACTIVITY_TIMESTAMP = "last_activity_timestamp";
    
    // Session timeout: 24 hours (for offline capability)
    private static final long SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L;

    private SharedPreferences prefs;

    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setLoggedIn(boolean isLoggedIn) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        if (isLoggedIn) {
            long currentTime = System.currentTimeMillis();
            editor.putLong(KEY_LOGIN_TIMESTAMP, currentTime);
            editor.putLong(KEY_LAST_ACTIVITY_TIMESTAMP, currentTime);
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setLoggedInUserNIC(String nic) {
        prefs.edit().putString(KEY_USER_NIC, nic).apply();
    }

    public String getLoggedInUserNIC() {
        return prefs.getString(KEY_USER_NIC, "");
    }

    public void setUserType(String userType) {
        prefs.edit().putString(KEY_USER_TYPE, userType).apply();
    }

    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, "");
    }

    public void setApiBaseUrl(String url) {
        prefs.edit().putString(KEY_API_BASE_URL, url).apply();
    }

    public String getApiBaseUrl() {
        return prefs.getString(KEY_API_BASE_URL, null);
    }

    public void updateLastActivity() {
        prefs.edit().putLong(KEY_LAST_ACTIVITY_TIMESTAMP, System.currentTimeMillis()).apply();
    }

    public boolean isSessionValid() {
        if (!isLoggedIn()) {
            return false;
        }

        long lastActivity = prefs.getLong(KEY_LAST_ACTIVITY_TIMESTAMP, 0);
        if (lastActivity == 0) {
            return false; // No timestamp recorded
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastActivity) < SESSION_TIMEOUT_MS;
    }

    public long getSessionRemainingTime() {
        if (!isLoggedIn()) {
            return 0;
        }

        long lastActivity = prefs.getLong(KEY_LAST_ACTIVITY_TIMESTAMP, 0);
        if (lastActivity == 0) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastActivity;
        return Math.max(0, SESSION_TIMEOUT_MS - elapsed);
    }

    public void clearUserData() {
        prefs.edit().clear().apply();
    }
}