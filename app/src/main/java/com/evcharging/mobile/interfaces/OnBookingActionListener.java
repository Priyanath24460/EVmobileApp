package com.evcharging.mobile.interfaces;

import com.evcharging.mobile.models.Booking;

public interface OnBookingActionListener {
    void onBookingSelected(Booking booking);
    void onBookingCancelled(Booking booking);
    void onQRCodeRequested(Booking booking);
}
