package com.evcharging.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {
    private static final String PREF_NAME = "EVChargingPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_NIC = "user_nic";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_API_BASE_URL = "api_base_url";

    private SharedPreferences prefs;

    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setLoggedIn(boolean isLoggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
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

    public void clearUserData() {
        prefs.edit().clear().apply();
    }
}