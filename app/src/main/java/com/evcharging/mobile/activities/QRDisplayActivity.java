package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.evcharging.mobile.R;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.DateUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRDisplayActivity extends AppCompatActivity {

    private ImageView ivQRCode;
    private TextView tvBookingReference, tvStation, tvDateTime, tvStatus;

    // Android logging with tag
    private static final String TAG = "QRDisplayActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_display);

        initializeViews();
        displayQRCode();
    }

    private void initializeViews() {
        ivQRCode = findViewById(R.id.ivQRCode);
        tvBookingReference = findViewById(R.id.tvBookingReference);
        tvStation = findViewById(R.id.tvStation);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvStatus = findViewById(R.id.tvStatus);
        
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    private void displayQRCode() {
        Booking booking = (Booking) getIntent().getSerializableExtra("booking");

        if (booking != null && booking.getId() != null && !booking.getId().isEmpty()) {
            // Set booking details
            tvBookingReference.setText("Booking ID: " + booking.getId());
            tvStation.setText("Station: " + (booking.getStationName() != null ? booking.getStationName() : booking.getChargingStationId()));
            tvDateTime.setText("Date: " + DateUtils.formatDateTime(booking.getReservationDateTime()));
            tvStatus.setText("Status: " + booking.getStatus());

            // Generate QR Code - always use booking ID for consistency
            String qrData = com.evcharging.mobile.utils.QRCodeGenerator.generateBookingQRData(
                booking.getId(), 
                booking.getEvOwnerNIC(), 
                booking.getChargingStationId()
            );
            generateQRCode(qrData);
            Log.i(TAG, "Generated QR code for booking: " + booking.getId());
        } else {
            Toast.makeText(this, "Invalid booking information", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Booking object is null or has invalid ID");
            finish();
        }
    }

    private void generateQRCode(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) {
            Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "QR code data is null or empty");
            return;
        }

        try {
            Bitmap bitmap = com.evcharging.mobile.utils.QRCodeGenerator.generateQRCode(qrData, 500, 500);
            if (bitmap != null) {
                ivQRCode.setImageBitmap(bitmap);
                Log.i(TAG, "QR code generated successfully for booking");
            } else {
                Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error generating QR code: " + e.getMessage(), e);
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show();
        }
    }
}