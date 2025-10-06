package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.models.Booking;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManualVerificationActivity extends AppCompatActivity {

    private TextInputEditText etBookingId, etCustomerNIC;
    private Button btnVerify, btnClear;
    
    private String operatorId;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_verification);

        initializeViews();
        getIntentData();
        setupClickListeners();
    }

    private void initializeViews() {
        etBookingId = findViewById(R.id.etBookingId);
        etCustomerNIC = findViewById(R.id.etCustomerNIC);
        btnVerify = findViewById(R.id.btnVerify);
        btnClear = findViewById(R.id.btnClear);

        apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void getIntentData() {
        operatorId = getIntent().getStringExtra("operator_id");
    }

    private void setupClickListeners() {
        btnVerify.setOnClickListener(v -> verifyManually());
        btnClear.setOnClickListener(v -> clearFields());
    }

    private void verifyManually() {
        String bookingId = etBookingId.getText().toString().trim();
        String customerNIC = etCustomerNIC.getText().toString().trim();

        if (bookingId.isEmpty()) {
            etBookingId.setError("Booking ID is required");
            return;
        }

        if (customerNIC.isEmpty()) {
            etCustomerNIC.setError("Customer NIC is required");
            return;
        }

        // Create manual verification request
        String manualQRData = "EVCHARGE:" + bookingId + ":" + customerNIC + ":manual";
        
        // Navigate to verification activity
        Intent intent = new Intent(this, BookingVerificationActivity.class);
        intent.putExtra("qr_content", manualQRData);
        intent.putExtra("operator_id", operatorId);
        startActivity(intent);
        finish();
    }

    private void clearFields() {
        etBookingId.setText("");
        etCustomerNIC.setText("");
        etBookingId.setError(null);
        etCustomerNIC.setError(null);
    }
}