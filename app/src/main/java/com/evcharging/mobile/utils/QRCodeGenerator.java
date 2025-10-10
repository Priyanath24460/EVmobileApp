package com.evcharging.mobile.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeGenerator {

    public static String generateBookingQRData(String bookingId, String customerNIC, String stationId) {
        // Handle null values
        bookingId = bookingId != null ? bookingId : "";
        customerNIC = customerNIC != null ? customerNIC : "";
        stationId = stationId != null ? stationId : "";
        
        return "EVCHARGE:" + bookingId + ":" + customerNIC + ":" + stationId;
    }

    public static Bitmap generateQRCode(String data, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }

    public static BookingQRData parseQRData(String qrContent) {
        if (qrContent == null || !qrContent.startsWith("EVCHARGE:")) {
            return null;
        }
        
        String[] parts = qrContent.split(":");
        if (parts.length >= 4) {
            return new BookingQRData(parts[1], parts[2], parts[3]);
        }
        return null;
    }

    public static class BookingQRData {
        private String bookingId;
        private String customerNIC;
        private String stationId;

        public BookingQRData(String bookingId, String customerNIC, String stationId) {
            this.bookingId = bookingId;
            this.customerNIC = customerNIC;
            this.stationId = stationId;
        }

        public String getBookingId() { return bookingId; }
        public String getCustomerNIC() { return customerNIC; }
        public String getStationId() { return stationId; }
    }
}