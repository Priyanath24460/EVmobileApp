package com.evcharging.mobile.utils;

import android.app.AlertDialog;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for session debugging and management
 */
public class SessionDebugger {
    
    /**
     * Show session information dialog for debugging
     */
    public static void showSessionInfo(Context context) {
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
        
        StringBuilder info = new StringBuilder();
        info.append("Session Information:\n\n");
        info.append("Logged In: ").append(prefs.isLoggedIn()).append("\n");
        info.append("User NIC: ").append(prefs.getLoggedInUserNIC()).append("\n");
        info.append("User Type: ").append(prefs.getUserType()).append("\n");
        info.append("Session Valid: ").append(prefs.isSessionValid()).append("\n");
        
        long remainingTime = prefs.getSessionRemainingTime();
        if (remainingTime > 0) {
            long hours = remainingTime / (1000 * 60 * 60);
            long minutes = (remainingTime % (1000 * 60 * 60)) / (1000 * 60);
            info.append("Time Remaining: ").append(hours).append("h ").append(minutes).append("m\n");
        } else {
            info.append("Session Expired\n");
        }
        
        new AlertDialog.Builder(context)
                .setTitle("Session Debug Info")
                .setMessage(info.toString())
                .setPositiveButton("OK", null)
                .setNegativeButton("Clear Session", (dialog, which) -> {
                    prefs.clearUserData();
                })
                .show();
    }
    
    /**
     * Check if session is about to expire (within 1 hour)
     */
    public static boolean isSessionNearExpiry(SharedPreferencesHelper prefs) {
        if (!prefs.isLoggedIn() || !prefs.isSessionValid()) {
            return false;
        }
        
        long remainingTime = prefs.getSessionRemainingTime();
        long oneHourMs = 60 * 60 * 1000L; // 1 hour in milliseconds
        
        return remainingTime > 0 && remainingTime < oneHourMs;
    }
}