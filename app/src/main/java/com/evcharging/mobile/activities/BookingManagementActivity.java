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
    private boolean isAuthorizedStation = true; // Default to true, will be validated
    private String operatorStationId = null;
    private String operatorStationName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_management);

        initializeViews();
        setupToolbar();
        initializeData();
        setupClickListeners();
        
        // Show loading state while validation is in progress
        showValidationInProgress();
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
        
        // Ensure booking station name is available
        if ((currentBooking.getStationName() == null || currentBooking.getStationName().isEmpty()) 
            && currentBooking.getChargingStationId() != null) {
            fetchBookingStationName();
        } else {
            // Validate if this booking belongs to the operator's station
            validateStationAccess();
        }
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
        
        // Basic booking info with authorization warning
        String bookingIdText = "Booking ID: " + currentBooking.getId();
        if (!isAuthorizedStation) {
            bookingIdText += "\n⚠️ DIFFERENT STATION - READ ONLY";
        }
        tvBookingId.setText(bookingIdText);
        tvBookingId.setTextColor(isAuthorizedStation ? 
            getResources().getColor(R.color.on_surface, null) : 
            getResources().getColor(R.color.error, null));
        
        tvCustomerNIC.setText("Customer: " + currentBooking.getEvOwnerNIC());
        
        // Station info with emphasis on station mismatch
        String stationInfo = "Station: " + (currentBooking.getStationName() != null ? 
            currentBooking.getStationName() : currentBooking.getChargingStationId());
        stationInfo += "\nStation ID: " + currentBooking.getChargingStationId();
        
        if (currentBooking.getSlotId() != null) {
            stationInfo += "\nCharging Point: " + currentBooking.getSlotId();
        }
        
        if (!isAuthorizedStation) {
            stationInfo += "\n\n🚫 NOT YOUR ASSIGNED STATION";
            if (operatorStationId != null) {
                String yourStation = "✓ Your Station: ";
                if (operatorStationName != null && !operatorStationName.isEmpty()) {
                    yourStation += operatorStationName + " (ID: " + operatorStationId + ")";
                } else {
                    yourStation += "ID: " + operatorStationId;
                }
                stationInfo += "\n" + yourStation;
            }
        }
        
        tvStationInfo.setText(stationInfo);
        tvStationInfo.setTextColor(isAuthorizedStation ? 
            getResources().getColor(R.color.on_surface, null) : 
            getResources().getColor(R.color.error, null));
        
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
        btnCancel.setVisibility(View.GONE);
        
        // Only show buttons if operator is authorized for this station
        if (!isAuthorizedStation) {
            // Hide all action buttons for unauthorized access
            Log.d(TAG, "Hiding all action buttons - unauthorized station access");
            return;
        }
        
        // Show relevant buttons based on current status (only for authorized stations)
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
                // No action buttons for completed/cancelled bookings
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
        if (isAuthorizedStation) {
            btnApprove.setEnabled(true);
            btnStartCharging.setEnabled(true);
            btnComplete.setEnabled(true);
            btnCancel.setEnabled(true);
        }
        btnRefresh.setEnabled(true);
    }
    
    /**
     * Show loading state while station validation is in progress
     */
    private void showValidationInProgress() {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        
        // Hide all content until validation is complete
        cvCustomerInfo.setVisibility(View.GONE);
        cvBookingDetails.setVisibility(View.GONE);
        cvStatusActions.setVisibility(View.GONE);
        
        // Show loading message in booking ID field
        tvBookingId.setText("Validating station access...");
        tvBookingId.setVisibility(View.VISIBLE);
    }
    
    /**
     * Show content after validation is complete
     */
    private void showValidationComplete() {
        // Hide progress bar
        progressBar.setVisibility(View.GONE);
        
        // Show all content
        cvCustomerInfo.setVisibility(View.VISIBLE);
        cvBookingDetails.setVisibility(View.VISIBLE);
        cvStatusActions.setVisibility(View.VISIBLE);
        
        // Now display the booking information and update buttons
        displayBookingInfo();
        updateStatusButtons();
    }
    
    /**
     * Validate if the current operator can manage this booking
     * Checks if the booking belongs to the operator's assigned station
     */
    private void validateStationAccess() {
        Log.d(TAG, "Validating station access for operator: " + operatorUsername);
        
        // Fetch operator's bookings to determine their assigned station
        Call<java.util.List<Booking>> call = apiService.getOperatorBookings(operatorUsername);
        call.enqueue(new Callback<java.util.List<Booking>>() {
            @Override
            public void onResponse(Call<java.util.List<Booking>> call, Response<java.util.List<Booking>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Get the first booking to determine operator's station
                    Booking operatorBooking = response.body().get(0);
                    operatorStationId = operatorBooking.getChargingStationId();
                    operatorStationName = operatorBooking.getStationName();
                    
                    Log.d(TAG, "Operator assigned to station: " + operatorStationId + 
                          " (" + operatorStationName + ")");
                    Log.d(TAG, "Booking belongs to station: " + currentBooking.getChargingStationId() + 
                          " (" + currentBooking.getStationName() + ")");
                    
                    // Check if the current booking belongs to the operator's station
                    isAuthorizedStation = operatorStationId.equals(currentBooking.getChargingStationId());
                    
                    // If operator station name is missing, try to fetch it
                    if (operatorStationName == null || operatorStationName.isEmpty()) {
                        fetchOperatorStationName(operatorStationId);
                    } else {
                        runOnUiThread(() -> {
                            updateUIBasedOnAuthorization();
                            showValidationComplete();
                        });
                    }
                } else {
                    Log.w(TAG, "Could not determine operator's assigned station");
                    // If we can't determine the station, assume unauthorized for security
                    isAuthorizedStation = false;
                    runOnUiThread(() -> {
                        updateUIBasedOnAuthorization();
                        showValidationComplete();
                    });
                }
            }

            @Override
            public void onFailure(Call<java.util.List<Booking>> call, Throwable t) {
                Log.e(TAG, "Failed to validate station access: " + t.getMessage());
                // If validation fails, assume unauthorized for security
                isAuthorizedStation = false;
                runOnUiThread(() -> {
                    updateUIBasedOnAuthorization();
                    showValidationComplete();
                    Toast.makeText(BookingManagementActivity.this, 
                        "Unable to validate station access. Limited view only.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * Update UI based on whether operator is authorized to manage this booking
     */
    private void updateUIBasedOnAuthorization() {
        if (!isAuthorizedStation) {
            // Apply red theme for unauthorized access
            cvCustomerInfo.setCardBackgroundColor(getResources().getColor(R.color.error_light, null));
            cvBookingDetails.setCardBackgroundColor(getResources().getColor(R.color.error_light, null));
            cvStatusActions.setCardBackgroundColor(getResources().getColor(R.color.error_light, null));
            
            // Hide all action buttons for unauthorized access
            btnApprove.setVisibility(View.GONE);
            btnStartCharging.setVisibility(View.GONE);
            btnComplete.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
            
            // Show warning message immediately (this will be called after showValidationComplete)
            // We'll show it in a post to ensure UI is fully loaded first
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                showUnauthorizedAccessWarning();
            });
        } else {
            // Keep normal theme for authorized access
            cvCustomerInfo.setCardBackgroundColor(getResources().getColor(R.color.surface, null));
            cvBookingDetails.setCardBackgroundColor(getResources().getColor(R.color.surface, null));
            cvStatusActions.setCardBackgroundColor(getResources().getColor(R.color.surface, null));
        }
    }
    
    /**
     * Show warning dialog for unauthorized station access
     */
    private void showUnauthorizedAccessWarning() {
        // Build booking station info with name and ID
        String bookingStationInfo;
        if (currentBooking.getStationName() != null && !currentBooking.getStationName().isEmpty()) {
            bookingStationInfo = currentBooking.getStationName() + "\n   ID: " + currentBooking.getChargingStationId();
        } else {
            bookingStationInfo = "ID: " + currentBooking.getChargingStationId();
        }
        
        // Build operator station info with name and ID
        String operatorStationInfo;
        if (operatorStationName != null && !operatorStationName.isEmpty()) {
            operatorStationInfo = operatorStationName + "\n   ID: " + operatorStationId;
        } else if (operatorStationId != null) {
            operatorStationInfo = "ID: " + operatorStationId;
        } else {
            operatorStationInfo = "Unable to determine";
        }
        
        String warningMessage = "⚠️ UNAUTHORIZED STATION ACCESS ⚠️\n\n" +
            "This booking belongs to a different charging station.\n\n" +
            "📍 BOOKING STATION:\n" + bookingStationInfo + "\n\n" +
            "✓ YOUR ASSIGNED STATION:\n" + operatorStationInfo + "\n\n" +
            "🔒 SECURITY POLICY:\n" +
            "You can only manage bookings from your assigned station.\n\n" +
            "📖 This is a read-only view for security purposes.";
            
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Station Access Denied")
            .setMessage(warningMessage)
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("I Understand", null)
            .setCancelable(false)
            .show();
    }
    
    /**
     * Fetch operator's station name if it's missing
     */
    private void fetchOperatorStationName(String stationId) {
        Log.d(TAG, "Fetching operator station name for ID: " + stationId);
        
        Call<com.evcharging.mobile.models.ChargingStation> call = apiService.getStationById(stationId);
        call.enqueue(new Callback<com.evcharging.mobile.models.ChargingStation>() {
            @Override
            public void onResponse(Call<com.evcharging.mobile.models.ChargingStation> call, 
                                 Response<com.evcharging.mobile.models.ChargingStation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    operatorStationName = response.body().getName();
                    Log.d(TAG, "Operator station name fetched: " + operatorStationName);
                } else {
                    Log.w(TAG, "Could not fetch operator station name, using ID only");
                }
                
                runOnUiThread(() -> {
                    updateUIBasedOnAuthorization();
                    showValidationComplete();
                });
            }

            @Override
            public void onFailure(Call<com.evcharging.mobile.models.ChargingStation> call, Throwable t) {
                Log.w(TAG, "Failed to fetch operator station name: " + t.getMessage());
                
                runOnUiThread(() -> {
                    updateUIBasedOnAuthorization();
                    showValidationComplete();
                });
            }
        });
    }
    
    /**
     * Fetch booking's station name if it's missing
     */
    private void fetchBookingStationName() {
        String stationId = currentBooking.getChargingStationId();
        Log.d(TAG, "Fetching booking station name for ID: " + stationId);
        
        Call<com.evcharging.mobile.models.ChargingStation> call = apiService.getStationById(stationId);
        call.enqueue(new Callback<com.evcharging.mobile.models.ChargingStation>() {
            @Override
            public void onResponse(Call<com.evcharging.mobile.models.ChargingStation> call, 
                                 Response<com.evcharging.mobile.models.ChargingStation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentBooking.setStationName(response.body().getName());
                    if (response.body().getLocation() != null) {
                        currentBooking.setStationAddress(response.body().getLocation().getFullAddress());
                    }
                    Log.d(TAG, "Booking station name fetched: " + currentBooking.getStationName());
                } else {
                    Log.w(TAG, "Could not fetch booking station name, using ID only");
                }
                
                // Now proceed with station validation
                validateStationAccess();
            }

            @Override
            public void onFailure(Call<com.evcharging.mobile.models.ChargingStation> call, Throwable t) {
                Log.w(TAG, "Failed to fetch booking station name: " + t.getMessage());
                
                // Proceed with validation even if station name fetch fails
                validateStationAccess();
            }
        });
    }
}