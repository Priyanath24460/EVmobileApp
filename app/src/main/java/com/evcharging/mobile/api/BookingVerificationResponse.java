package com.evcharging.mobile.api;

import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.models.User;

public class BookingVerificationResponse {
    private boolean success;
    private String message;
    private Booking booking;
    private User evOwner;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public User getEvOwner() { return evOwner; }
    public void setEvOwner(User evOwner) { this.evOwner = evOwner; }
}