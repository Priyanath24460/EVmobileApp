package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.models.User;
import com.evcharging.mobile.utils.SharedPreferencesHelper;
import com.evcharging.mobile.utils.StationOperatorManager;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class OperatorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvOperatorInfo, tvScannedCount, tvActiveCount;
    private Button btnScanQR, btnManualVerify, btnViewActiveBookings, btnProfile, btnLogout;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabScanQR;
    
    private SharedPreferencesHelper prefs;
    private User currentOperator;
    private ApiService apiService;
    
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    @Override
    protected void onResume() {
        super.onResume();
        
        // Update activity timestamp when user returns to the app
        if (prefs != null && prefs.isLoggedIn() && prefs.isSessionValid()) {
            prefs.updateLastActivity();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_dashboard);

        // Initialize prefs first for session validation
        prefs = new SharedPreferencesHelper(this);
        
        // Validate session and update activity timestamp
        if (prefs.isLoggedIn() && prefs.isSessionValid()) {
            prefs.updateLastActivity();
            initializeViews();
            loadOperatorData();
            setupClickListeners();
            checkCameraPermission();
        } else {
            // Session expired - redirect to login
            prefs.clearUserData();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvOperatorInfo = findViewById(R.id.tvOperatorInfo);
        tvScannedCount = findViewById(R.id.tvScannedCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnManualVerify = findViewById(R.id.btnManualVerify);
        btnViewActiveBookings = findViewById(R.id.btnViewActiveBookings);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);
        fabScanQR = findViewById(R.id.fabScanQR);

        apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void loadOperatorData() {
        String operatorNIC = prefs.getLoggedInUserNIC();
        if (operatorNIC != null) {
            // For now, create a mock operator. In real app, fetch from database
            currentOperator = new User();
            currentOperator.setNic(operatorNIC);
            currentOperator.setFirstName("Station");
            currentOperator.setLastName("Operator");
            currentOperator.setUserType("StationOperator");
            
            updateUI();
        }
    }

    private void updateUI() {
        if (currentOperator != null) {
            tvWelcome.setText("Welcome, " + currentOperator.getFullName());
            tvOperatorInfo.setText("Station ID: " + currentOperator.getNic());
        }
        
        // Update stats (demo values for now)
        tvScannedCount.setText("0");
        tvActiveCount.setText("0");
    }

    private void setupClickListeners() {
        btnScanQR.setOnClickListener(v -> startNewQRScanner());
        btnManualVerify.setOnClickListener(v -> startManualVerification());
        btnViewActiveBookings.setOnClickListener(v -> viewActiveBookings());
        btnProfile.setOnClickListener(v -> viewProfile());
        btnLogout.setOnClickListener(v -> logout());
        fabScanQR.setOnClickListener(v -> startNewQRScanner());
        
        // Debug: Long press on welcome text to show session info
        tvWelcome.setOnLongClickListener(v -> {
            com.evcharging.mobile.utils.SessionDebugger.showSessionInfo(this);
            return true;
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission required for QR scanning", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startNewQRScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, OperatorQRScannerActivity.class);
            startActivity(intent);
        } else {
            checkCameraPermission();
        }
    }

    private void startQRScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan the EV Charging Booking QR Code");
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            integrator.setCameraId(0);
            integrator.initiateScan();
        } else {
            checkCameraPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                processQRCode(result.getContents());
            }
        }
    }

    private void processQRCode(String qrContent) {
        // Show processing dialog
        Toast.makeText(this, "Processing QR Code...", Toast.LENGTH_SHORT).show();
        
        // Navigate to booking verification activity
        Intent intent = new Intent(this, BookingVerificationActivity.class);
        intent.putExtra("qr_content", qrContent);
        intent.putExtra("operator_id", currentOperator.getNic());
        startActivity(intent);
    }

    private void startManualVerification() {
        Intent intent = new Intent(this, ManualVerificationActivity.class);
        intent.putExtra("operator_id", currentOperator.getNic());
        startActivity(intent);
    }

    private void viewActiveBookings() {
        Intent intent = new Intent(this, OperatorBookingsActivity.class);
        startActivity(intent);
    }

    private void viewProfile() {
        Intent intent = new Intent(this, OperatorProfileActivity.class);
        startActivity(intent);
    }

    private void logout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout Options")
                .setMessage("Choose logout option:")
                .setPositiveButton("Logout Only", (dialog, which) -> {
                    // Simple logout - keep cached credentials for offline access
                    prefs.clearUserData();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Clear Cache & Logout", (dialog, which) -> {
                    // Logout and clear cached credentials (requires internet for next login)
                    String username = prefs.getLoggedInUserNIC();
                    StationOperatorManager operatorManager = new StationOperatorManager(this);
                    operatorManager.clearOperatorCredentials(username);
                    
                    prefs.clearUserData();
                    Toast.makeText(this, "Logged out and cleared cached credentials", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
}