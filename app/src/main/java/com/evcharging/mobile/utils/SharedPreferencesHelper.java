package com.evcharging.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {
    private static final String PREF_NAME = "EVChargingPrefs";
    private static final String KEY_LOGGED_IN_USER_NIC = "logged_in_user_nic";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_API_BASE_URL = "api_base_url";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public SharedPreferencesHelper(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public void setLoggedInUserNIC(String nic) {
        editor.putString(KEY_LOGGED_IN_USER_NIC, nic);
        editor.apply();
    }

    public String getLoggedInUserNIC() {
        return preferences.getString(KEY_LOGGED_IN_USER_NIC, null);
    }

    public void setUserType(String userType) {
        editor.putString(KEY_USER_TYPE, userType);
        editor.apply();
    }

    public String getUserType() {
        return preferences.getString(KEY_USER_TYPE, null);
    }

    public void setLoggedIn(boolean loggedIn) {
        editor.putBoolean(KEY_IS_LOGGED_IN, loggedIn);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setApiBaseUrl(String url) {
        editor.putString(KEY_API_BASE_URL, url);
        editor.apply();
    }

    public String getApiBaseUrl() {
        return preferences.getString(KEY_API_BASE_URL, "https://webserverapi-vc37.onrender.com/");
    }

    public void clearUserData() {
        editor.remove(KEY_LOGGED_IN_USER_NIC);
        editor.remove(KEY_USER_TYPE);
        editor.remove(KEY_IS_LOGGED_IN);
        editor.apply();
    }
}
