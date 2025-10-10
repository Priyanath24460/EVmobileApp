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

    @Query("SELECT * FROM bookings WHERE evOwnerNIC = :nic AND status IN ('Pending', 'Approved') AND reservationDateTime >= :currentTime ORDER BY reservationDateTime ASC")
    List<Booking> getUpcomingBookings(String nic, long currentTime);

    @Query("SELECT * FROM bookings WHERE evOwnerNIC = :nic AND status IN ('Completed', 'Cancelled') ORDER BY reservationDateTime DESC")
    List<Booking> getPastBookings(String nic);

    @Query("SELECT COUNT(*) FROM bookings WHERE evOwnerNIC = :nic AND status = 'Pending' AND reservationDateTime >= :currentTime")
    int getPendingBookingCount(String nic, long currentTime);

    @Query("SELECT COUNT(*) FROM bookings WHERE evOwnerNIC = :nic AND status = 'Approved' AND reservationDateTime >= :currentTime")
    int getApprovedBookingCount(String nic, long currentTime);

    // Convenience methods with current time
    default List<Booking> getUpcomingBookings(String nic) {
        return getUpcomingBookings(nic, System.currentTimeMillis());
    }

    default int getPendingBookingCount(String nic) {
        return getPendingBookingCount(nic, System.currentTimeMillis());
    }

    default int getApprovedBookingCount(String nic) {
        return getApprovedBookingCount(nic, System.currentTimeMillis());
    }

    @Query("DELETE FROM bookings WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    void updateStatus(String id, String status);

    @Query("DELETE FROM bookings WHERE evOwnerNIC = :nic")
    void deleteAllBookingsForUser(String nic);

    // Fix upsert to handle station name updates and prevent duplicates
    default void upsert(com.evcharging.mobile.models.Booking booking) {
        Booking existing = getBookingById(booking.getId());
        if (existing == null) {
            insert(booking);
        } else {
            // Preserve local station name if server doesn't provide it
            if (booking.getStationName() == null && existing.getStationName() != null) {
                booking.setStationName(existing.getStationName());
            }
            update(booking);
        }
    }

    @Query("DELETE FROM bookings WHERE evOwnerNIC = :nic AND id != :excludeId AND chargingStationId = :stationId AND reservationDateTime = :reservationTime")
    void deleteDuplicateBookings(String nic, String excludeId, String stationId, long reservationTime);

    @Query("DELETE FROM bookings WHERE id IN (SELECT id FROM bookings WHERE evOwnerNIC = :nic GROUP BY chargingStationId, reservationDateTime, status HAVING COUNT(*) > 1 AND id != MIN(id))")
    void cleanupAllDuplicates(String nic);
}
