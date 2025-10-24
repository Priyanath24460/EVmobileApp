package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.QRCodeGenerator;
import com.evcharging.mobile.utils.SharedPreferencesHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorQRScannerActivity extends AppCompatActivity {

    private static final String TAG = "OperatorQRScanner";
    
    private TextView tvScanResult, tvInstructions;
    private Button btnScanQR, btnManualEntry;
    private View progressBar;
    
    private ApiService apiService;
    private SharedPreferencesHelper prefs;
    private String currentOperatorUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_qr_scanner);

        initializeViews();
        setupToolbar();
        initializeServices();
        setupClickListeners();
    }

    private void initializeViews() {
        tvScanResult = findViewById(R.id.tvScanResult);
        tvInstructions = findViewById(R.id.tvInstructions);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnManualEntry = findViewById(R.id.btnManualEntry);
        progressBar = findViewById(R.id.progressBar);
        
        // Set initial instructions
        tvInstructions.setText("Scan customer's booking QR code to view and manage booking details");
        hideProgressBar();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("QR Code Scanner");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeServices() {
        apiService = ApiClient.getClient(this).create(ApiService.class);
        prefs = new SharedPreferencesHelper(this);
        currentOperatorUsername = prefs.getLoggedInUserNIC();
        
        if (currentOperatorUsername == null || currentOperatorUsername.isEmpty()) {
            Toast.makeText(this, "Error: No operator logged in", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupClickListeners() {
        btnScanQR.setOnClickListener(v -> startQRScanner());
        
        btnManualEntry.setOnClickListener(v -> {
            // Create dialog for manual booking ID entry
            createManualEntryDialog();
        });
    }

    private void startQRScanner() {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan booking QR code");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        } catch (Exception e) {
            Log.e(TAG, "Error starting QR scanner: " + e.getMessage());
            Toast.makeText(this, "Error starting camera scanner", Toast.LENGTH_SHORT).show();
        }
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
                Log.d(TAG, "QR Code scanned: " + scannedData);
                processScannedQRCode(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processScannedQRCode(String qrData) {
        showProgressBar();
        tvScanResult.setText("Processing QR code...");
        
        // Parse QR code data
        QRCodeGenerator.BookingQRData qrInfo = QRCodeGenerator.parseQRData(qrData);
        
        if (qrInfo == null) {
            hideProgressBar();
            tvScanResult.setText("Invalid QR code format");
            Toast.makeText(this, "Invalid QR code. Please scan a valid booking QR code.", Toast.LENGTH_LONG).show();
            return;
        }
        
        String bookingId = qrInfo.getBookingId();
        Log.d(TAG, "Extracted booking ID: " + bookingId);
        
        // Fetch booking details from server
        fetchBookingDetails(bookingId);
    }

    private void fetchBookingDetails(String bookingId) {
        tvScanResult.setText("Fetching booking details...");
        
        Call<Booking> call = apiService.getBookingById(bookingId);
        call.enqueue(new Callback<Booking>() {
            @Override
            public void onResponse(Call<Booking> call, Response<Booking> response) {
                hideProgressBar();
                
                if (response.isSuccessful() && response.body() != null) {
                    Booking booking = response.body();
                    Log.d(TAG, "Booking fetched successfully: " + booking.getId());
                    
                    // Show booking management screen
                    showBookingManagementScreen(booking);
                } else {
                    String errorMsg = "Booking not found (Code: " + response.code() + ")";
                    tvScanResult.setText(errorMsg);
                    Toast.makeText(OperatorQRScannerActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.w(TAG, "API error: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Booking> call, Throwable t) {
                hideProgressBar();
                String errorMsg = "Network error: " + t.getMessage();
                tvScanResult.setText(errorMsg);
                Toast.makeText(OperatorQRScannerActivity.this, 
                    "Unable to fetch booking details. Please check your connection.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network error fetching booking: " + t.getMessage(), t);
            }
        });
    }

    private void showBookingManagementScreen(Booking booking) {
        Intent intent = new Intent(this, BookingManagementActivity.class);
        intent.putExtra("booking", booking);
        intent.putExtra("operatorUsername", currentOperatorUsername);
        startActivity(intent);
    }

    private void createManualEntryDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Manual Booking Entry");
        
        // Create EditText for manual entry
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter booking ID or reference");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        
        builder.setPositiveButton("Look Up", (dialog, which) -> {
            String bookingId = input.getText().toString().trim();
            if (!bookingId.isEmpty()) {
                showProgressBar();
                fetchBookingDetails(bookingId);
            } else {
                Toast.makeText(this, "Please enter a booking ID", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        btnScanQR.setEnabled(false);
        btnManualEntry.setEnabled(false);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        btnScanQR.setEnabled(true);
        btnManualEntry.setEnabled(true);
    }
}