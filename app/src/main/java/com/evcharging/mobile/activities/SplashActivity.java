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

        if (prefs.isLoggedIn()) {
            String userType = prefs.getUserType();
            if ("EVOwner".equals(userType)) {
                startActivity(new Intent(this, EVOwnerDashboardActivity.class));
            } else if ("StationOperator".equals(userType)) {
                startActivity(new Intent(this, StationOperatorActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }

        finish();
    }
}