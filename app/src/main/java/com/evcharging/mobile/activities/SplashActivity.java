package com.evcharging.mobile.activities;


import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.evcharging.mobile.R;
import com.evcharging.mobile.utils.SharedPreferencesHelper;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            navigateToAppropriateScreen();
        }, SPLASH_DELAY);
    }

    private void navigateToAppropriateScreen() {
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);

        // Check if user is logged in AND session is still valid
        if (prefs.isLoggedIn() && prefs.isSessionValid()) {
            String userType = prefs.getUserType();
            String userNIC = prefs.getLoggedInUserNIC();
            
            // Validate that we have required session data
            if (userType != null && userNIC != null && !userNIC.isEmpty()) {
                // Update last activity timestamp
                prefs.updateLastActivity();
                
                if ("EVOwner".equals(userType)) {
                    startActivity(new Intent(this, EVOwnerDashboardActivity.class));
                } else if ("StationOperator".equals(userType)) {
                    // Fixed: Redirect to correct OperatorDashboardActivity
                    startActivity(new Intent(this, OperatorDashboardActivity.class));
                } else {
                    // Unknown user type - clear session and go to login
                    prefs.clearUserData();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            } else {
                // Invalid session data - clear and go to login
                prefs.clearUserData();
                startActivity(new Intent(this, LoginActivity.class));
            }
        } else {
            // Session expired or user not logged in - clear any stale data and go to login
            if (prefs.isLoggedIn() && !prefs.isSessionValid()) {
                prefs.clearUserData(); // Clear expired session
            }
            startActivity(new Intent(this, LoginActivity.class));
        }

        finish();
    }
}