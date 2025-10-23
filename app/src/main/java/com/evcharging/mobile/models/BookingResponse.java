package com.evcharging.mobile.models;

public class BookingResponse {
    private String bookingId;
    private String status;
    private String message;
    private String qrCodeData;

    public BookingResponse() {}

    public BookingResponse(String bookingId, String status, String message, String qrCodeData) {
        this.bookingId = bookingId;
        this.status = status;
        this.message = message;
        this.qrCodeData = qrCodeData;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }
}