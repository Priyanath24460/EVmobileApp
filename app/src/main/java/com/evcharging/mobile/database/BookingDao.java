package com.evcharging.mobile.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.evcharging.mobile.models.Booking;

import java.util.List;

@Dao
public interface BookingDao {

    @Insert
    void insert(Booking booking);

    @Update
    void update(Booking booking);

    @Delete
    void delete(Booking booking);

    @Query("SELECT * FROM bookings WHERE evOwnerNIC = :nic ORDER BY reservationDateTime DESC")
    List<Booking> getBookingsByNIC(String nic);

    @Query("SELECT * FROM bookings WHERE id = :id")
    Booking getBookingById(String id);

    @Query("SELECT * FROM bookings WHERE evOwnerNIC = :nic AND status IN ('Pending', 'Approved') ORDER BY reservationDateTime ASC")
    List<Booking> getUpcomingBookings(String nic);

    @Query("SELECT * FROM bookings WHERE evOwnerNIC = :nic AND status IN ('Completed', 'Cancelled') ORDER BY reservationDateTime DESC")
    List<Booking> getPastBookings(String nic);

    @Query("SELECT COUNT(*) FROM bookings WHERE evOwnerNIC = :nic AND status = 'Pending'")
    int getPendingBookingCount(String nic);

    @Query("SELECT COUNT(*) FROM bookings WHERE evOwnerNIC = :nic AND status = 'Approved'")
    int getApprovedBookingCount(String nic);

    @Query("DELETE FROM bookings WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    void updateStatus(String id, String status);

    @Query("DELETE FROM bookings WHERE evOwnerNIC = :nic")
    void deleteAllBookingsForUser(String nic);

    // Room doesn't have a native upsert prior to newer versions; implement simple upsert
    default void upsert(com.evcharging.mobile.models.Booking booking) {
        Booking existing = getBookingById(booking.getId());
        if (existing == null) {
            insert(booking);
        } else {
            update(booking);
        }
    }
}
