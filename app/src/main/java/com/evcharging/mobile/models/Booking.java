package com.evcharging.mobile.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.evcharging.mobile.utils.DateConverter;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "bookings")
public class Booking implements Serializable {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    private String id;

    @SerializedName("bookingReference")
    private String bookingReference;

    @SerializedName("evOwnerNIC")
    private String evOwnerNIC;

    @SerializedName("chargingStationId")
    private String chargingStationId;

    @SerializedName("chargingPointNumber")
    private String slotId;

    @TypeConverters(DateConverter.class)
    @SerializedName("startTime")
    private Date reservationDateTime;

    @TypeConverters(DateConverter.class)
    @SerializedName("bookingDate")
    private Date bookingDate;

    @SerializedName("durationMinutes")
    private int durationMinutes;

    @SerializedName("status")
    private String status; // Pending, Approved, Started, Completed, Cancelled

    @SerializedName("qrCodeData")
    private String qrCodeData;

    // Local fields (not from API)
    private String stationName;
    private String stationAddress;

    // Constructors
    public Booking() {
        // ID will be set when saving to database
    }

    public Booking(@NonNull String id, String bookingReference, String evOwnerNIC, String chargingStationId,
                   String slotId, Date reservationDateTime, Date bookingDate, int durationMinutes,
                   String status, String qrCodeData) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.evOwnerNIC = evOwnerNIC;
        this.chargingStationId = chargingStationId;
        this.slotId = slotId;
        this.reservationDateTime = reservationDateTime;
        this.bookingDate = bookingDate;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.qrCodeData = qrCodeData;
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public String getEvOwnerNIC() { return evOwnerNIC; }
    public void setEvOwnerNIC(String evOwnerNIC) { this.evOwnerNIC = evOwnerNIC; }

    public String getChargingStationId() { return chargingStationId; }
    public void setChargingStationId(String chargingStationId) { this.chargingStationId = chargingStationId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public Date getReservationDateTime() { return reservationDateTime; }
    public void setReservationDateTime(Date reservationDateTime) { this.reservationDateTime = reservationDateTime; }

    public Date getBookingDate() { return bookingDate; }
    public void setBookingDate(Date bookingDate) { this.bookingDate = bookingDate; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getStationAddress() { return stationAddress; }
    public void setStationAddress(String stationAddress) { this.stationAddress = stationAddress; }

    public boolean isUpcoming() {
        return reservationDateTime != null &&
                reservationDateTime.after(new Date()) &&
                !status.equals("Cancelled") &&
                !status.equals("Completed");
    }

    public boolean canBeCancelled() {
        if (reservationDateTime == null) return false;

        long timeDifference = reservationDateTime.getTime() - new Date().getTime();
        long hoursDifference = timeDifference / (1000 * 60 * 60);

        return hoursDifference >= 12 && (status.equals("Pending") || status.equals("Approved"));
    }
}