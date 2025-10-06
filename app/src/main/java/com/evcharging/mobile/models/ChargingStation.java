package com.evcharging.mobile.models;

import com.google.gson.annotations.SerializedName;

public class ChargingStation {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("stationType")
    private String stationType; // AC or DC

    @SerializedName("totalSlots")
    private int totalSlots;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("location")
    private Location location;

    // Constructors
    public ChargingStation() {}

    public ChargingStation(String id, String name, String stationType, int totalSlots,
                           boolean isActive, Location location) {
        this.id = id;
        this.name = name;
        this.stationType = stationType;
        this.totalSlots = totalSlots;
        this.isActive = isActive;
        this.location = location;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStationType() { return stationType; }
    public void setStationType(String stationType) { this.stationType = stationType; }

    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public static class Location {
        @SerializedName("address")
        private String address;

        @SerializedName("city")
        private String city;

        @SerializedName("latitude")
        private double latitude;

        @SerializedName("longitude")
        private double longitude;

        public Location() {}

        public Location(String address, String city, double latitude, double longitude) {
            this.address = address;
            this.city = city;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getFullAddress() {
            return address + ", " + city;
        }
    }
}