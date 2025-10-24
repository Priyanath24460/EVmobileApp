package com.evcharging.mobile.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.evcharging.mobile.models.User;

@Dao
public interface UserDao {

    @Insert
    void insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users WHERE nic = :nic")
    User getUserByNIC(String nic);

    @Query("SELECT * FROM users WHERE nic = :nic AND password = :password")
    User login(String nic, String password);

    @Query("DELETE FROM users WHERE nic = :nic")
    void deleteByNIC(String nic);

    @Query("UPDATE users SET isActive = :isActive WHERE nic = :nic")
    void updateStatus(String nic, boolean isActive);

    @Query("SELECT COUNT(*) FROM users WHERE nic = :nic")
    int userExists(String nic);

    // Station Operator specific queries
    @Query("SELECT * FROM users WHERE userType = 'StationOperator'")
    java.util.List<User> getAllStationOperators();

    @Query("SELECT * FROM users WHERE nic = :username AND userType = 'StationOperator'")
    User getStationOperatorByUsername(String username);

    @Query("SELECT * FROM users WHERE nic = :username AND password = :password AND userType = 'StationOperator'")
    User loginStationOperator(String username, String password);

    @Query("UPDATE users SET password = :newPassword WHERE nic = :username AND userType = 'StationOperator'")
    void updateStationOperatorPassword(String username, String newPassword);

    @Query("DELETE FROM users WHERE userType = 'StationOperator'")
    void clearAllStationOperators();

    @Query("DELETE FROM users WHERE nic = :username AND userType = 'StationOperator'")
    void deleteStationOperator(String username);

    // EVOwner specific queries
    @Query("SELECT * FROM users WHERE userType = 'EVOwner' OR userType IS NULL")
    java.util.List<User> getAllEVOwners();

    @Query("SELECT * FROM users WHERE nic = :nic AND (userType = 'EVOwner' OR userType IS NULL)")
    User getEVOwnerByNIC(String nic);

    @Query("SELECT * FROM users WHERE nic = :nic AND password = :password AND (userType = 'EVOwner' OR userType IS NULL)")
    User loginEVOwner(String nic, String password);

    // Upsert operation - insert if new, update if exists
    default void upsert(User user) {
        User existing = getUserByNIC(user.getNic());
        if (existing == null) {
            insert(user);
        } else {
            update(user);
        }
    }
}
