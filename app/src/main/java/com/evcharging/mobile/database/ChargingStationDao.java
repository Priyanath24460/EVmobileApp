package com.evcharging.mobile.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.evcharging.mobile.models.ChargingStation;

import java.util.List;

@Dao
public interface ChargingStationDao {

    @Insert
    void insert(ChargingStation station);

    @Update
    void update(ChargingStation station);

    @Delete
    void delete(ChargingStation station);

    @Query("SELECT * FROM charging_stations")
    List<ChargingStation> getAllStations();

    @Query("SELECT * FROM charging_stations WHERE id = :id")
    ChargingStation getStationById(String id);

    @Query("SELECT * FROM charging_stations WHERE isActive = 1")
    List<ChargingStation> getActiveStations();

    @Query("DELETE FROM charging_stations WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE charging_stations SET isActive = :isActive WHERE id = :id")
    void updateStatus(String id, boolean isActive);

    @Query("SELECT COUNT(*) FROM charging_stations WHERE id = :id")
    int stationExists(String id);
}
