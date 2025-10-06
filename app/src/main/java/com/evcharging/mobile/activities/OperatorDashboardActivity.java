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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class OperatorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvOperatorInfo;
    private Button btnScanQR, btnManualVerify, btnViewActiveBookings, btnLogout;
    
    private SharedPreferencesHelper prefs;
    private User currentOperator;
    private ApiService apiService;
    
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_dashboard);

        initializeViews();
        loadOperatorData();
        setupClickListeners();
        checkCameraPermission();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvOperatorInfo = findViewById(R.id.tvOperatorInfo);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnManualVerify = findViewById(R.id.btnManualVerify);
        btnViewActiveBookings = findViewById(R.id.btnViewActiveBookings);
        btnLogout = findViewById(R.id.btnLogout);

        prefs = new SharedPreferencesHelper(this);
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
            tvOperatorInfo.setText("Operator ID: " + currentOperator.getNic());
        }
    }

    private void setupClickListeners() {
        btnScanQR.setOnClickListener(v -> startQRScanner());
        btnManualVerify.setOnClickListener(v -> startManualVerification());
        btnViewActiveBookings.setOnClickListener(v -> viewActiveBookings());
        btnLogout.setOnClickListener(v -> logout());
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
        // TODO: Create ActiveBookingsActivity
        Toast.makeText(this, "Active Bookings feature coming soon", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, ActiveBookingsActivity.class);
        // intent.putExtra("operator_id", currentOperator.getNic());
        // startActivity(intent);
    }

    private void logout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    prefs.clearUserData();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}