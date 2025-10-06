package com.evcharging.mobile.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    private String nic;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String vehicleModel;
    private String vehiclePlateNumber;
    private boolean isActive;
    private String userType; // "EVOwner" or "StationOperator"
    private String password; // For local storage only

    // Constructors
    public User() {
        this.nic = ""; // Initialize with empty string to avoid null
    }

    public User(@NonNull String nic, String firstName, String lastName, String email,
                String phoneNumber, String vehicleModel, String vehiclePlateNumber,
                boolean isActive, String userType) {
        this.nic = nic;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.vehicleModel = vehicleModel;
        this.vehiclePlateNumber = vehiclePlateNumber;
        this.isActive = isActive;
        this.userType = userType;
    }

    // Getters and Setters
    @NonNull
    public String getNic() { return nic; }
    public void setNic(@NonNull String nic) { this.nic = nic; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getVehiclePlateNumber() { return vehiclePlateNumber; }
    public void setVehiclePlateNumber(String vehiclePlateNumber) { this.vehiclePlateNumber = vehiclePlateNumber; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}