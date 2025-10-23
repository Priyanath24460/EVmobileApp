package com.evcharging.mobile.models;

import java.util.List;

public class TimeSlotAvailability {
    private String stationId;
    private String date;
    private List<ChargingPointSlots> chargingPoints;

    public TimeSlotAvailability() {}

    public TimeSlotAvailability(String stationId, String date, List<ChargingPointSlots> chargingPoints) {
        this.stationId = stationId;
        this.date = date;
        this.chargingPoints = chargingPoints;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<ChargingPointSlots> getChargingPoints() {
        return chargingPoints;
    }

    public void setChargingPoints(List<ChargingPointSlots> chargingPoints) {
        this.chargingPoints = chargingPoints;
    }
}