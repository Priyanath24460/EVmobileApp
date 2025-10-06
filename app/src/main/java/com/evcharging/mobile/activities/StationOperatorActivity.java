package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.evcharging.mobile.R;
import com.evcharging.mobile.utils.SharedPreferencesHelper;

public class StationOperatorActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnScanQR, btnViewBookings, btnLogout;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_operator);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnViewBookings = findViewById(R.id.btnViewBookings);
        btnLogout = findViewById(R.id.btnLogout);

        prefs = new SharedPreferencesHelper(this);

        String username = prefs.getLoggedInUserNIC();
        tvWelcome.setText("Welcome, Station Operator " + username + "!");
    }

    private void setupClickListeners() {
        btnScanQR.setOnClickListener(v -> {
            startActivity(new Intent(this, QRScannerActivity.class));
        });

        btnViewBookings.setOnClickListener(v -> {
            // In real app, this would show bookings for this station
            Toast.makeText(this, "View Bookings - Feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            prefs.clearUserData();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}