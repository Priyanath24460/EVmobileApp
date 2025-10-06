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
}
