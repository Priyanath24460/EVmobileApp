package com.evcharging.mobile.models;

import java.util.List;

public class ChargingPointSlots {
    private int chargingPointNumber;
    private List<TimeSlot> timeSlots;

    public ChargingPointSlots() {}

    public ChargingPointSlots(int chargingPointNumber, List<TimeSlot> timeSlots) {
        this.chargingPointNumber = chargingPointNumber;
        this.timeSlots = timeSlots;
    }

    public int getChargingPointNumber() {
        return chargingPointNumber;
    }

    public void setChargingPointNumber(int chargingPointNumber) {
        this.chargingPointNumber = chargingPointNumber;
    }

    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<TimeSlot> timeSlots) {
        this.timeSlots = timeSlots;
    }
}