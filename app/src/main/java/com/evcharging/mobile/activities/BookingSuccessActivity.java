package com.evcharging.mobile.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.evcharging.mobile.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingSuccessActivity extends AppCompatActivity {
    
    private static final String TAG = "BookingSuccessActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private TextView tvBookingDetails;
    private ImageView ivQRCode;
    private Button btnDownloadQR, btnDone;
    
    private String bookingId;
    private String stationName;
    private String bookingDate;
    private String bookingTime;
    private int chargingPoint;
    private String qrCodeData;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_success);
        
        initializeViews();
        getBookingDetails();
        setupClickListeners();
        generateQRCode();
    }

    private void initializeViews() {
        tvBookingDetails = findViewById(R.id.tvBookingDetails);
        ivQRCode = findViewById(R.id.ivQRCode);
        btnDownloadQR = findViewById(R.id.btnDownloadQR);
        btnDone = findViewById(R.id.btnDone);
    }

    private void getBookingDetails() {
        Intent intent = getIntent();
        bookingId = intent.getStringExtra("bookingId");
        stationName = intent.getStringExtra("stationName");
        bookingDate = intent.getStringExtra("bookingDate");
        bookingTime = intent.getStringExtra("bookingTime");
        chargingPoint = intent.getIntExtra("chargingPoint", 1);
        qrCodeData = intent.getStringExtra("qrCodeData");
        
        // Display booking details
        String details = String.format(
            "🎉 Booking Confirmed!\n\n" +
            "📍 Station: %s\n" +
            "📅 Date: %s\n" +
            "⏰ Time: %s\n" +
            "🔌 Charging Point: %d\n" +
            "🎫 Booking ID: %s\n\n" +
            "Show this QR code at the charging station:",
            stationName, bookingDate, bookingTime, chargingPoint, bookingId
        );
        
        tvBookingDetails.setText(details);
    }

    private void setupClickListeners() {
        btnDownloadQR.setOnClickListener(v -> downloadQRCode());
        btnDone.setOnClickListener(v -> finish());
    }

    private void generateQRCode() {
        if (qrCodeData == null || qrCodeData.trim().isEmpty()) {
            // Fallback: create QR data from booking details
            qrCodeData = String.format(
                "BOOKING:%s|STATION:%s|DATE:%s|TIME:%s|POINT:%d",
                bookingId, stationName, bookingDate, bookingTime, chargingPoint
            );
        }
        
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrCodeData, BarcodeFormat.QR_CODE, 512, 512);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            ivQRCode.setImageBitmap(qrBitmap);
            
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadQRCode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CODE);
            return;
        }
        
        saveQRCodeToGallery();
    }

    private void saveQRCodeToGallery() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR code not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Create enhanced QR code with booking details
            Bitmap enhancedBitmap = createEnhancedQRCode();
            
            // Save to Pictures directory
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File evChargingDir = new File(picturesDir, "EV_Charging_Bookings");
            if (!evChargingDir.exists()) {
                evChargingDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = String.format("EV_Booking_%s_%s.png", bookingId, timestamp);
            File file = new File(evChargingDir, filename);
            
            FileOutputStream fos = new FileOutputStream(file);
            enhancedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            
            // Notify media scanner
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(android.net.Uri.fromFile(file));
            sendBroadcast(mediaScanIntent);
            
            Toast.makeText(this, "QR code saved to Pictures/EV_Charging_Bookings/", Toast.LENGTH_LONG).show();
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving QR code", e);
            Toast.makeText(this, "Failed to save QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap createEnhancedQRCode() {
        // Create a larger bitmap with booking details
        int qrSize = 512;
        int padding = 40;
        int textHeight = 200;
        int totalWidth = qrSize + (padding * 2);
        int totalHeight = qrSize + textHeight + (padding * 3);
        
        Bitmap enhancedBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(enhancedBitmap);
        
        // Fill background
        canvas.drawColor(Color.WHITE);
        
        // Draw QR code
        canvas.drawBitmap(qrBitmap, padding, padding, null);
        
        // Setup text paint
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24);
        textPaint.setAntiAlias(true);
        
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(32);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);
        
        // Draw title
        String title = "EV Charging Booking";
        Rect titleBounds = new Rect();
        titlePaint.getTextBounds(title, 0, title.length(), titleBounds);
        float titleX = (totalWidth - titleBounds.width()) / 2f;
        canvas.drawText(title, titleX, qrSize + padding + 40, titlePaint);
        
        // Draw booking details
        int textY = qrSize + padding + 80;
        int lineHeight = 35;
        
        canvas.drawText("Station: " + stationName, padding, textY, textPaint);
        textY += lineHeight;
        canvas.drawText("Date: " + bookingDate + " at " + bookingTime, padding, textY, textPaint);
        textY += lineHeight;
        canvas.drawText("Charging Point: " + chargingPoint, padding, textY, textPaint);
        textY += lineHeight;
        canvas.drawText("Booking ID: " + bookingId, padding, textY, textPaint);
        
        return enhancedBitmap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveQRCodeToGallery();
            } else {
                Toast.makeText(this, "Permission required to save QR code", Toast.LENGTH_SHORT).show();
            }
        }
    }
}