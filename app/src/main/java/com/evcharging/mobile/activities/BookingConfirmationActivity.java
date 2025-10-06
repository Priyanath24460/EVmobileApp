package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.database.AppDatabase;
import com.evcharging.mobile.database.BookingDao;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.QRCodeGenerator;
import com.evcharging.mobile.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingConfirmationActivity extends AppCompatActivity {

    private TextView tvBookingId, tvStationName, tvDateTime, tvDuration, tvStatus;
    private ImageView ivQRCode;
    private Button btnGenerateQR, btnModifyBooking, btnCancelBooking, btnBackToDashboard;
    
    private Booking currentBooking;
    private BookingDao bookingDao;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        initializeViews();
        setupDatabase();
        setupClickListeners();
        loadBookingDetails();
    }

    private void initializeViews() {
        tvBookingId = findViewById(R.id.tvBookingId);
        tvStationName = findViewById(R.id.tvStationName);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvDuration = findViewById(R.id.tvDuration);
        tvStatus = findViewById(R.id.tvStatus);
        ivQRCode = findViewById(R.id.ivQRCode);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);
        btnModifyBooking = findViewById(R.id.btnModifyBooking);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard);

        apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void setupDatabase() {
        AppDatabase database = AppDatabase.getInstance(this);
        bookingDao = database.bookingDao();
    }

    private void setupClickListeners() {
        btnGenerateQR.setOnClickListener(v -> generateQRCode());
        btnModifyBooking.setOnClickListener(v -> modifyBooking());
        btnCancelBooking.setOnClickListener(v -> cancelBooking());
        btnBackToDashboard.setOnClickListener(v -> backToDashboard());
    }

    private void loadBookingDetails() {
        String bookingId = getIntent().getStringExtra("booking_id");
        if (bookingId != null) {
            new Thread(() -> {
                currentBooking = bookingDao.getBookingById(bookingId);
                runOnUiThread(() -> {
                    if (currentBooking != null) {
                        displayBookingDetails();
                        updateButtonStates();
                    } else {
                        Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }).start();
        }
    }

    private void displayBookingDetails() {
        tvBookingId.setText("Booking ID: " + currentBooking.getId());
        tvStationName.setText("Station: " + currentBooking.getStationName());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        tvDateTime.setText("Date & Time: " + dateFormat.format(currentBooking.getReservationDateTime()));
        
        tvDuration.setText("Duration: " + currentBooking.getDurationMinutes() + " minutes");
        tvStatus.setText("Status: " + currentBooking.getStatus());

        // Show QR code if available
        if (currentBooking.getQrCodeData() != null && !currentBooking.getQrCodeData().isEmpty()) {
            displayQRCode(currentBooking.getQrCodeData());
            btnGenerateQR.setText("Regenerate QR Code");
        }
    }

    private void updateButtonStates() {
        String status = currentBooking.getStatus();
        
        // Enable/disable buttons based on booking status and timing
        btnModifyBooking.setEnabled("Pending".equals(status) && currentBooking.canBeCancelled());
        btnCancelBooking.setEnabled(("Pending".equals(status) || "Approved".equals(status)) && 
                                  currentBooking.canBeCancelled());
        btnGenerateQR.setEnabled("Approved".equals(status));
    }

    private void generateQRCode() {
        if ("Approved".equals(currentBooking.getStatus())) {
            // Generate QR code locally
            String qrData = QRCodeGenerator.generateBookingQRData(
                currentBooking.getId(), 
                currentBooking.getEvOwnerNIC(), 
                currentBooking.getChargingStationId()
            );
            
            // Update booking with QR data
            currentBooking.setQrCodeData(qrData);
            new Thread(() -> {
                bookingDao.update(currentBooking);
                runOnUiThread(() -> displayQRCode(qrData));
            }).start();
        } else {
            Toast.makeText(this, "QR Code can only be generated for approved bookings", 
                         Toast.LENGTH_SHORT).show();
        }
    }

    private void displayQRCode(String qrData) {
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrData, 300, 300);
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
            ivQRCode.setVisibility(View.VISIBLE);
        }
    }

    private void modifyBooking() {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("booking_id", currentBooking.getId());
        startActivity(intent);
    }

    private void cancelBooking() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Yes", (dialog, which) -> performCancellation())
                .setNegativeButton("No", null)
                .show();
    }

    private void performCancellation() {
        // Update local database
        new Thread(() -> {
            currentBooking.setStatus("Cancelled");
            bookingDao.update(currentBooking);
            
            // Sync with server
            runOnUiThread(() -> {
                Call<Void> call = apiService.cancelBooking(currentBooking.getId());
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(BookingConfirmationActivity.this, 
                                         "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                            displayBookingDetails();
                            updateButtonStates();
                        } else {
                            Toast.makeText(BookingConfirmationActivity.this, 
                                         "Failed to cancel booking on server", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(BookingConfirmationActivity.this, 
                                     "Network error while cancelling", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }).start();
    }

    private void backToDashboard() {
        Intent intent = new Intent(this, EVOwnerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}