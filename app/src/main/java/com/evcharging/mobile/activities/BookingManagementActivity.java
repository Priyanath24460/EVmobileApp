package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.api.StatusUpdateRequest;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingManagementActivity extends AppCompatActivity {

    private static final String TAG = "BookingManagement";
    
    // UI Elements
    private TextView tvBookingId, tvCustomerNIC, tvStationInfo, tvDateTime, tvDuration, tvCurrentStatus;
    private MaterialCardView cvCustomerInfo, cvBookingDetails, cvStatusActions;
    private MaterialButton btnApprove, btnStartCharging, btnComplete, btnCancel, btnRefresh;
    private View progressBar;
    
    // Data
    private Booking currentBooking;
    private String operatorUsername;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_management);

        initializeViews();
        setupToolbar();
        initializeData();
        setupClickListeners();
        displayBookingInfo();
        updateStatusButtons();
    }

    private void initializeViews() {
        // Text views
        tvBookingId = findViewById(R.id.tvBookingId);
        tvCustomerNIC = findViewById(R.id.tvCustomerNIC);
        tvStationInfo = findViewById(R.id.tvStationInfo);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvDuration = findViewById(R.id.tvDuration);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        
        // Cards
        cvCustomerInfo = findViewById(R.id.cvCustomerInfo);
        cvBookingDetails = findViewById(R.id.cvBookingDetails);
        cvStatusActions = findViewById(R.id.cvStatusActions);
        
        // Buttons
        btnApprove = findViewById(R.id.btnApprove);
        btnStartCharging = findViewById(R.id.btnStartCharging);
        btnComplete = findViewById(R.id.btnComplete);
        btnCancel = findViewById(R.id.btnCancel);
        btnRefresh = findViewById(R.id.btnRefresh);
        
        // Progress bar
        progressBar = findViewById(R.id.progressBar);
        hideProgressBar();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Booking Management");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeData() {
        currentBooking = (Booking) getIntent().getSerializableExtra("booking");
        operatorUsername = getIntent().getStringExtra("operatorUsername");
        apiService = ApiClient.getClient(this).create(ApiService.class);
        
        if (currentBooking == null) {
            Toast.makeText(this, "Error: No booking data received", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        Log.d(TAG, "Managing booking: " + currentBooking.getId() + " by operator: " + operatorUsername);
    }

    private void setupClickListeners() {
        btnApprove.setOnClickListener(v -> {
            showConfirmationDialog("Approve Booking", 
                "Are you sure you want to approve this booking?", 
                () -> updateBookingStatus("Approved"));
        });
        
        btnStartCharging.setOnClickListener(v -> {
            showConfirmationDialog("Start Charging Session", 
                "Start charging for this booking? Customer will be notified.", 
                () -> updateBookingStatus("Started"));
        });
        
        btnComplete.setOnClickListener(v -> {
            showConfirmationDialog("Complete Booking", 
                "Mark this booking as completed? This action cannot be undone.", 
                () -> updateBookingStatus("Completed"));
        });
        
        btnCancel.setOnClickListener(v -> {
            showConfirmationDialog("Cancel Booking", 
                "Are you sure you want to cancel this booking? Customer will be notified.", 
                () -> updateBookingStatus("Cancelled"));
        });
        
        btnRefresh.setOnClickListener(v -> refreshBookingData());
    }

    private void displayBookingInfo() {
        if (currentBooking == null) return;
        
        // Basic booking info
        tvBookingId.setText("Booking ID: " + currentBooking.getId());
        tvCustomerNIC.setText("Customer: " + currentBooking.getEvOwnerNIC());
        
        // Station and time info
        String stationInfo = "Station: " + (currentBooking.getStationName() != null ? 
            currentBooking.getStationName() : currentBooking.getChargingStationId());
        if (currentBooking.getSlotId() != null) {
            stationInfo += "\nCharging Point: " + currentBooking.getSlotId();
        }
        tvStationInfo.setText(stationInfo);
        
        tvDateTime.setText("Date & Time: " + DateUtils.formatDateTime(currentBooking.getReservationDateTime()));
        tvDuration.setText("Duration: " + currentBooking.getDurationMinutes() + " minutes");
        
        // Status with color coding
        updateStatusDisplay();
    }

    private void updateStatusDisplay() {
        String status = currentBooking.getStatus();
        tvCurrentStatus.setText("Status: " + status);
        
        // Color coding for status
        int statusColor;
        switch (status.toLowerCase()) {
            case "pending":
                statusColor = getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "approved":
                statusColor = getResources().getColor(android.R.color.holo_green_dark);
                break;
            case "started":
                statusColor = getResources().getColor(R.color.status_started);
                break;
            case "completed":
                statusColor = getResources().getColor(android.R.color.holo_blue_dark);
                break;
            case "cancelled":
                statusColor = getResources().getColor(android.R.color.holo_red_dark);
                break;
            default:
                statusColor = getResources().getColor(android.R.color.black);
        }
        tvCurrentStatus.setTextColor(statusColor);
    }

    private void updateStatusButtons() {
        String currentStatus = currentBooking.getStatus().toLowerCase();
        
        // Reset all buttons
        btnApprove.setVisibility(View.GONE);
        btnStartCharging.setVisibility(View.GONE);
        btnComplete.setVisibility(View.GONE);
        btnCancel.setVisibility(View.VISIBLE);
        
        // Show relevant buttons based on current status
        switch (currentStatus) {
            case "pending":
                btnApprove.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                break;
            case "approved":
                btnStartCharging.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                break;
            case "started":
                btnComplete.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                break;
            case "completed":
            case "cancelled":
                btnCancel.setVisibility(View.GONE);
                break;
        }
    }

    private void updateBookingStatus(String newStatus) {
        Log.d(TAG, "Starting status update for booking " + currentBooking.getId() + " to status: " + newStatus);
        showProgressBar();
        
        Call<Booking> call;
        
        // Use the PATCH endpoint for status updates (bypasses time restrictions)
        StatusUpdateRequest statusUpdate = new StatusUpdateRequest(newStatus, "operator");
        Log.d(TAG, "Updating booking " + currentBooking.getId() + " from " + currentBooking.getStatus() + " to " + newStatus);
        
        // Use the status update endpoint that bypasses business rules
        call = apiService.updateBookingStatus(currentBooking.getId(), statusUpdate);
        
        call.enqueue(new Callback<Booking>() {
            @Override
            public void onResponse(Call<Booking> call, Response<Booking> response) {
                hideProgressBar();
                
                if (response.isSuccessful() && response.body() != null) {
                    currentBooking = response.body();
                    Log.d(TAG, "Booking status updated to: " + newStatus);
                    
                    displayBookingInfo();
                    updateStatusButtons();
                    
                    Toast.makeText(BookingManagementActivity.this, 
                        "Booking " + newStatus.toLowerCase() + " successfully", Toast.LENGTH_SHORT).show();
                } else {
                    // Enhanced error logging
                    String errorMsg = "Failed to update booking status";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg = "Error " + response.code() + ": " + errorBody;
                            Log.e(TAG, "API Error - Code: " + response.code() + ", Body: " + errorBody);
                        } else {
                            errorMsg = "Error " + response.code() + ": " + response.message();
                            Log.e(TAG, "API Error - Code: " + response.code() + ", Message: " + response.message());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                        errorMsg = "Error " + response.code() + " - Unable to read response";
                    }
                    
                    Toast.makeText(BookingManagementActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Booking> call, Throwable t) {
                hideProgressBar();
                Log.e(TAG, "Network error updating booking status: " + t.getMessage(), t);
                
                String errorMessage;
                if (t instanceof java.net.SocketTimeoutException) {
                    errorMessage = "Request timed out. Please check your connection and try again.";
                } else if (t instanceof java.net.ConnectException) {
                    errorMessage = "Cannot connect to server. Please check server is running.";
                } else if (t instanceof java.net.UnknownHostException) {
                    errorMessage = "Cannot find server. Please check your network connection.";
                } else {
                    errorMessage = "Network error: " + t.getMessage() + ". Please try again.";
                }
                
                Toast.makeText(BookingManagementActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void refreshBookingData() {
        showProgressBar();
        
        Call<Booking> call = apiService.getBookingById(currentBooking.getId());
        call.enqueue(new Callback<Booking>() {
            @Override
            public void onResponse(Call<Booking> call, Response<Booking> response) {
                hideProgressBar();
                
                if (response.isSuccessful() && response.body() != null) {
                    currentBooking = response.body();
                    displayBookingInfo();
                    updateStatusButtons();
                    Toast.makeText(BookingManagementActivity.this, "Booking data refreshed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BookingManagementActivity.this, "Failed to refresh booking data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Booking> call, Throwable t) {
                hideProgressBar();
                Toast.makeText(BookingManagementActivity.this, "Network error refreshing data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm", (dialog, which) -> onConfirm.run())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        btnApprove.setEnabled(false);
        btnStartCharging.setEnabled(false);
        btnComplete.setEnabled(false);
        btnCancel.setEnabled(false);
        btnRefresh.setEnabled(false);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        btnApprove.setEnabled(true);
        btnStartCharging.setEnabled(true);
        btnComplete.setEnabled(true);
        btnCancel.setEnabled(true);
        btnRefresh.setEnabled(true);
    }
}