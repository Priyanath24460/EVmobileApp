package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.evcharging.mobile.R;
import com.evcharging.mobile.database.AppDatabase;
import com.evcharging.mobile.database.UserDao;
import com.evcharging.mobile.models.User;
import com.evcharging.mobile.utils.SharedPreferencesHelper;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvNIC, tvEmail, tvPhone, tvVehicle, tvPlate;
    private Button btnUpdate, btnDeactivate, btnBack;
    private UserDao userDao;
    private SharedPreferencesHelper prefs;
    private String currentUserNIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        setupDatabase();
        loadUserProfile();
        setupClickListeners();
    }

    private void initializeViews() {
        tvName = findViewById(R.id.tvName);
        tvNIC = findViewById(R.id.tvNIC);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvVehicle = findViewById(R.id.tvVehicle);
        tvPlate = findViewById(R.id.tvPlate);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDeactivate = findViewById(R.id.btnDeactivate);
        btnBack = findViewById(R.id.btnBack);

        prefs = new SharedPreferencesHelper(this);
        currentUserNIC = prefs.getLoggedInUserNIC();
    }

    private void setupDatabase() {
        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
    }

    private void loadUserProfile() {
        new Thread(() -> {
            User user = userDao.getUserByNIC(currentUserNIC);
            runOnUiThread(() -> {
                if (user != null) {
                    displayUserProfile(user);
                }
            });
        }).start();
    }

    private void displayUserProfile(User user) {
        tvName.setText(user.getFullName());
        tvNIC.setText(user.getNic());
        tvEmail.setText(user.getEmail());
        tvPhone.setText(user.getPhoneNumber());
        tvVehicle.setText(user.getVehicleModel());
        tvPlate.setText(user.getVehiclePlateNumber());

        // Show/hide deactivate button based on current status
        btnDeactivate.setText(user.isActive() ? "Deactivate Account" : "Activate Account");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v ->
                // In real app, this would open an edit profile activity
                Toast.makeText(this, "Update profile feature coming soon", Toast.LENGTH_SHORT).show()
        );

        btnDeactivate.setOnClickListener(v -> toggleAccountStatus());
    }

    private void toggleAccountStatus() {
        new Thread(() -> {
            User user = userDao.getUserByNIC(currentUserNIC);
            if (user != null) {
                user.setActive(!user.isActive());
                userDao.update(user);

                runOnUiThread(() -> {
                    String message = user.isActive() ? "Account activated" : "Account deactivated";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    if (!user.isActive()) {
                        // Logout if account is deactivated
                        prefs.clearUserData();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    } else {
                        loadUserProfile();
                    }
                });
            }
        }).start();
    }
}