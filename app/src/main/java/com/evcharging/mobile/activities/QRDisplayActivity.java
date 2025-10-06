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
    private TextView tvBookingReference, tvStation, tvDateTime;

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
    }

    private void displayQRCode() {
        Booking booking = (Booking) getIntent().getSerializableExtra("booking");

        if (booking != null) {
            // Set booking details
            tvBookingReference.setText(booking.getBookingReference());
            tvStation.setText(booking.getStationName());
            tvDateTime.setText(DateUtils.formatDateTime(booking.getReservationDateTime()));

            // Generate QR Code
            generateQRCode(booking.getQrCodeData());
        } else {
            // Handle case where booking is null
            Toast.makeText(this, "Booking information not available", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Booking object is null in displayQRCode");
        }
    }

    private void generateQRCode(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) {
            Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "QR code data is null or empty");
            return;
        }

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(qrData, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            ivQRCode.setImageBitmap(bitmap);

            Log.i(TAG, "QR code generated successfully for booking");

        } catch (WriterException e) {
            // Log the error with proper logging
            Log.e(TAG, "Failed to generate QR code: " + e.getMessage(), e);

            // Show user-friendly error message
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();

            // Optionally show a placeholder or error image
           // ivQRCode.setImageResource(R.drawable.ic_error);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            Log.e(TAG, "Unexpected error generating QR code: " + e.getMessage(), e);
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show();
        }
    }
}