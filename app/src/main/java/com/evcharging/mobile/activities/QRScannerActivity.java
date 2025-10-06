package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRScannerActivity extends AppCompatActivity {

    private TextView tvScanResult;
    private Button btnRescan, btnManualEntry;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner); // Updated to match your filename

        initializeViews();
        setupClickListeners();
        startQRScanner();
    }

    private void initializeViews() {
        tvScanResult = findViewById(R.id.tvScanResult);
        btnRescan = findViewById(R.id.btnRescan);
        btnManualEntry = findViewById(R.id.btnManualEntry);
    apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void setupClickListeners() {
        btnRescan.setOnClickListener(v -> startQRScanner());

        btnManualEntry.setOnClickListener(v -> {
            // Optional: Implement manual booking ID entry
            Toast.makeText(this, "Manual entry feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a booking QR code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                tvScanResult.setText("Scan cancelled");
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String scannedData = result.getContents();
                processScannedData(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processScannedData(String scannedData) {
        tvScanResult.setText("Scanned: " + scannedData);

        // Parse the scanned data - in real app, this would be a booking ID or reference
        String bookingId = extractBookingIdFromQR(scannedData);

        if (bookingId != null) {
            verifyBooking(bookingId);
        } else {
            Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
            tvScanResult.setText("Invalid QR format: " + scannedData);
        }
    }

    private String extractBookingIdFromQR(String qrData) {
        // Simple extraction - in real app, use proper parsing
        if (qrData.startsWith("EVBOOKING:")) {
            return qrData.substring(10); // Remove prefix
        }
        return null;
    }

    private void verifyBooking(String bookingId) {
        tvScanResult.setText("Verifying booking: " + bookingId);
        Toast.makeText(this, "Verifying booking: " + bookingId, Toast.LENGTH_SHORT).show();

        // Simulate API call delay
        new android.os.Handler().postDelayed(() -> {
            // Simulate successful verification
            showBookingConfirmation(bookingId);
        }, 2000);
    }

    private void showBookingConfirmation(String bookingId) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Booking Verified")
                .setMessage("Booking ID: " + bookingId + "\n\nStatus: Approved\n\nProceed with charging?")
                .setPositiveButton("Start Charging", (dialog, which) -> {
                    completeChargingProcess(bookingId);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void completeChargingProcess(String bookingId) {
        tvScanResult.setText("Charging started for: " + bookingId);
        Toast.makeText(this, "Charging started for booking: " + bookingId, Toast.LENGTH_LONG).show();

        // Simulate charging completion after delay
        new android.os.Handler().postDelayed(() -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Charging Complete")
                    .setMessage("Charging session completed successfully for booking: " + bookingId)
                    .setPositiveButton("OK", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }, 3000);
    }
}