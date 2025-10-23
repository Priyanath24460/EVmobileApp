package com.evcharging.mobile.models;

public class BookingRequest {
    private String chargingStationId; // Changed from stationId to match server
    private String evOwnerNIC; // Added missing field
    private String bookingDate;
    private int timeSlot;
    private int chargingPointNumber;
    private int durationMinutes; // Keep for compatibility but server doesn't use it

    public BookingRequest() {}

    public String getChargingStationId() {
        return chargingStationId;
    }

    public void setChargingStationId(String chargingStationId) {
        this.chargingStationId = chargingStationId;
    }

    public String getEvOwnerNIC() {
        return evOwnerNIC;
    }

    public void setEvOwnerNIC(String evOwnerNIC) {
        this.evOwnerNIC = evOwnerNIC;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public int getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(int timeSlot) {
        this.timeSlot = timeSlot;
    }

    public int getChargingPointNumber() {
        return chargingPointNumber;
    }

    public void setChargingPointNumber(int chargingPointNumber) {
        this.chargingPointNumber = chargingPointNumber;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    // Keep old methods for backward compatibility
    public String getStationId() {
        return chargingStationId;
    }

    public void setStationId(String stationId) {
        this.chargingStationId = stationId;
    }
}