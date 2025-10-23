package com.evcharging.mobile.models;

public class TimeSlot {
    private int hour;
    private String displayTime;
    private boolean isAvailable;

    public TimeSlot() {}

    public TimeSlot(int hour, String displayTime, boolean isAvailable) {
        this.hour = hour;
        this.displayTime = displayTime;
        this.isAvailable = isAvailable;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public String getDisplayTime() {
        return displayTime;
    }

    public void setDisplayTime(String displayTime) {
        this.displayTime = displayTime;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}
