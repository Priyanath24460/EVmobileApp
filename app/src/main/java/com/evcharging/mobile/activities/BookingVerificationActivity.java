package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.models.User;
import com.evcharging.mobile.utils.QRCodeGenerator;

import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingVerificationActivity extends AppCompatActivity {

    private TextView tvVerificationStatus, tvBookingDetails, tvCustomerDetails, tvStationInfo;
    private LinearLayout layoutBookingInfo, layoutActions;
    private Button btnApprove, btnReject, btnComplete, btnBackToDashboard;

    private String qrContent;
    private String operatorId;
    private Booking verifiedBooking;
    private User customerDetails;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_verification);

        initializeViews();
        getIntentData();
        setupClickListeners();
        verifyQRCode();
    }

    private void initializeViews() {
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvBookingDetails = findViewById(R.id.tvBookingDetails);
        tvCustomerDetails = findViewById(R.id.tvCustomerDetails);
        tvStationInfo = findViewById(R.id.tvStationInfo);
        layoutBookingInfo = findViewById(R.id.layoutBookingInfo);
        layoutActions = findViewById(R.id.layoutActions);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnComplete = findViewById(R.id.btnComplete);
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard);

        apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void getIntentData() {
        qrContent = getIntent().getStringExtra("qr_content");
        operatorId = getIntent().getStringExtra("operator_id");

        if (qrContent == null || operatorId == null) {
            showError("Invalid verification data");
            return;
        }
    }

    private void setupClickListeners() {
        btnApprove.setOnClickListener(v -> approveBooking());
        btnReject.setOnClickListener(v -> rejectBooking());
        btnComplete.setOnClickListener(v -> completeBooking());
        btnBackToDashboard.setOnClickListener(v -> finish());
    }

    private void verifyQRCode() {
        tvVerificationStatus.setText("Verifying QR Code...");
        layoutBookingInfo.setVisibility(View.GONE);
        layoutActions.setVisibility(View.GONE);

        // Parse QR code to extract booking information
        QRCodeGenerator.BookingQRData qrData = QRCodeGenerator.parseQRData(qrContent);
        
        if (qrData == null) {
            showError("Invalid QR Code format");
            return;
        }

        // Verify with server
        com.evcharging.mobile.api.QRVerificationRequest request = new com.evcharging.mobile.api.QRVerificationRequest(qrContent, operatorId);
        Call<com.evcharging.mobile.api.BookingVerificationResponse> call = apiService.verifyBooking(qrData.getBookingId(), request);
        
        call.enqueue(new Callback<com.evcharging.mobile.api.BookingVerificationResponse>() {
            @Override
            public void onResponse(Call<com.evcharging.mobile.api.BookingVerificationResponse> call, 
                                 Response<com.evcharging.mobile.api.BookingVerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.evcharging.mobile.api.BookingVerificationResponse verificationResponse = response.body();
                    
                    if (verificationResponse.isSuccess()) {
                        verifiedBooking = verificationResponse.getBooking();
                        customerDetails = verificationResponse.getEvOwner();
                        showVerificationSuccess();
                    } else {
                        showError(verificationResponse.getMessage());
                    }
                } else {
                    showError("Failed to verify booking with server");
                }
            }

            @Override
            public void onFailure(Call<com.evcharging.mobile.api.BookingVerificationResponse> call, Throwable t) {
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void showVerificationSuccess() {
        tvVerificationStatus.setText("✅ Booking Verified Successfully");
        tvVerificationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        
        displayBookingDetails();
        displayCustomerDetails();
        
        layoutBookingInfo.setVisibility(View.VISIBLE);
        layoutActions.setVisibility(View.VISIBLE);
        
        updateActionButtons();
    }

    private void showError(String message) {
        tvVerificationStatus.setText("❌ Verification Failed: " + message);
        tvVerificationStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        
        layoutBookingInfo.setVisibility(View.GONE);
        layoutActions.setVisibility(View.GONE);
    }

    private void displayBookingDetails() {
        if (verifiedBooking == null) return;
        
        StringBuilder details = new StringBuilder();
        details.append("Booking ID: ").append(verifiedBooking.getId()).append("\n\n");
        details.append("Reference: ").append(verifiedBooking.getBookingReference()).append("\n\n");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        details.append("Reservation Time: ").append(dateFormat.format(verifiedBooking.getReservationDateTime())).append("\n\n");
        
        details.append("Duration: ").append(verifiedBooking.getDurationMinutes()).append(" minutes\n\n");
        details.append("Status: ").append(verifiedBooking.getStatus()).append("\n\n");
        details.append("Station: ").append(verifiedBooking.getStationName()).append("\n");
        
        tvBookingDetails.setText(details.toString());
    }

    private void displayCustomerDetails() {
        if (customerDetails == null) return;
        
        StringBuilder details = new StringBuilder();
        details.append("Customer: ").append(customerDetails.getFullName()).append("\n\n");
        details.append("NIC: ").append(customerDetails.getNic()).append("\n\n");
        details.append("Phone: ").append(customerDetails.getPhoneNumber()).append("\n\n");
        details.append("Vehicle: ").append(customerDetails.getVehicleModel()).append("\n");
        details.append("Plate: ").append(customerDetails.getVehiclePlateNumber()).append("\n");
        
        tvCustomerDetails.setText(details.toString());
    }

    private void updateActionButtons() {
        if (verifiedBooking == null) return;
        
        String status = verifiedBooking.getStatus();
        
        switch (status) {
            case "Pending":
                btnApprove.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
                btnComplete.setVisibility(View.GONE);
                break;
                
            case "Approved":
                btnApprove.setVisibility(View.GONE);
                btnReject.setVisibility(View.VISIBLE);
                btnComplete.setVisibility(View.VISIBLE);
                break;
                
            case "Completed":
            case "Cancelled":
                btnApprove.setVisibility(View.GONE);
                btnReject.setVisibility(View.GONE);
                btnComplete.setVisibility(View.GONE);
                break;
        }
    }

    private void approveBooking() {
        updateBookingStatus("Approved", "Approving booking...");
    }

    private void rejectBooking() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reject Booking")
                .setMessage("Are you sure you want to reject this booking?")
                .setPositiveButton("Yes", (dialog, which) -> 
                    updateBookingStatus("Cancelled", "Rejecting booking..."))
                .setNegativeButton("No", null)
                .show();
    }

    private void completeBooking() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Complete Charging")
                .setMessage("Has the EV charging been completed successfully?")
                .setPositiveButton("Yes", (dialog, which) -> 
                    updateBookingStatus("Completed", "Completing booking..."))
                .setNegativeButton("No", null)
                .show();
    }

    private void updateBookingStatus(String newStatus, String progressMessage) {
        Toast.makeText(this, progressMessage, Toast.LENGTH_SHORT).show();
        
        if ("Cancelled".equals(newStatus)) {
            // Handle cancellation separately since it returns Call<Void>
            Call<Void> cancelCall = apiService.cancelBooking(verifiedBooking.getId());
            cancelCall.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        verifiedBooking.setStatus(newStatus);
                        displayBookingDetails();
                        updateActionButtons();
                        
                        String message = getStatusUpdateMessage(newStatus);
                        Toast.makeText(BookingVerificationActivity.this, message, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(BookingVerificationActivity.this, 
                                     "Failed to update booking status", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(BookingVerificationActivity.this, 
                                 "Network error while updating status", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        
        Call<Booking> call;
        
        switch (newStatus) {
            case "Approved":
                call = apiService.approveBooking(verifiedBooking.getId());
                break;
            case "Completed":
                call = apiService.completeBooking(verifiedBooking.getId());
                break;
            default:
                return;
        }
        
        call.enqueue(new Callback<Booking>() {
            @Override
            public void onResponse(Call<Booking> call, Response<Booking> response) {
                if (response.isSuccessful()) {
                    verifiedBooking.setStatus(newStatus);
                    displayBookingDetails();
                    updateActionButtons();
                    
                    String message = getStatusUpdateMessage(newStatus);
                    Toast.makeText(BookingVerificationActivity.this, message, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(BookingVerificationActivity.this, 
                                 "Failed to update booking status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Booking> call, Throwable t) {
                Toast.makeText(BookingVerificationActivity.this, 
                             "Network error while updating status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStatusUpdateMessage(String status) {
        switch (status) {
            case "Approved":
                return "Booking approved successfully! Customer can now proceed with charging.";
            case "Completed":
                return "Charging session completed successfully!";
            case "Cancelled":
                return "Booking has been cancelled.";
            default:
                return "Status updated successfully.";
        }
    }
}