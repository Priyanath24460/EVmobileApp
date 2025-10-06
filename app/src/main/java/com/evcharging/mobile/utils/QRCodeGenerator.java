package com.evcharging.mobile.utils;

import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCodeGenerator {

    public static Bitmap generateQRCode(String data, int width, int height) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generateBookingQRData(String bookingId, String nic, String stationId) {
        return "EVBOOKING:" + bookingId + ":" + nic + ":" + stationId;
    }

    public static String parseBookingIdFromQR(String qrData) {
        if (qrData.startsWith("EVBOOKING:")) {
            String[] parts = qrData.split(":");
            return parts.length > 1 ? parts[1] : null;
        }
        return null;
    }

    public static BookingQRData parseQRData(String qrData) {
        if (qrData == null) {
            return null;
        }
        
        // Handle both old format "EVBOOKING:" and new format "EVCHARGE:"
        String[] parts;
        if (qrData.startsWith("EVBOOKING:")) {
            parts = qrData.split(":");
            if (parts.length >= 4) {
                return new BookingQRData(parts[1], parts[2], parts[3]);
            }
        } else if (qrData.startsWith("EVCHARGE:")) {
            parts = qrData.split(":");
            if (parts.length >= 4) {
                return new BookingQRData(parts[1], parts[2], parts[3]);
            }
        }
        return null;
    }

    public static class BookingQRData {
        private String bookingId;
        private String evOwnerNIC;
        private String stationId;
        
        public BookingQRData(String bookingId, String evOwnerNIC, String stationId) {
            this.bookingId = bookingId;
            this.evOwnerNIC = evOwnerNIC;
            this.stationId = stationId;
        }
        
        public String getBookingId() { return bookingId; }
        public String getEvOwnerNIC() { return evOwnerNIC; }
        public String getStationId() { return stationId; }
    }
}
