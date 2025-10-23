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

    private TextView tvName, tvNIC, tvUserType, tvEmail, tvPhone, tvVehicle, tvPlate;
    private TextView tvAvatarLetter, tvUserFullName, tvUserRole;
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
        tvUserType = findViewById(R.id.tvUserType);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvVehicle = findViewById(R.id.tvVehicle);
        tvPlate = findViewById(R.id.tvPlate);

        // New avatar views
        tvAvatarLetter = findViewById(R.id.tvAvatarLetter);
        tvUserFullName = findViewById(R.id.tvUserFullName);
        tvUserRole = findViewById(R.id.tvUserRole);

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
        tvUserType.setText(user.getUserType() != null ? formatUserType(user.getUserType()) : "Unknown");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Not provided");
        tvPhone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "Not provided");
        tvVehicle.setText(user.getVehicleModel() != null ? user.getVehicleModel() : "Not provided");
        tvPlate.setText(user.getVehiclePlateNumber() != null ? user.getVehiclePlateNumber() : "Not provided");

        // Update avatar and header information
        String firstName = user.getFirstName();
        if (firstName != null && !firstName.isEmpty()) {
            tvAvatarLetter.setText(String.valueOf(firstName.charAt(0)).toUpperCase());
            tvUserFullName.setText(user.getFullName());
            tvUserRole.setText(formatUserType(user.getUserType()));
        }

        // Show/hide deactivate button based on current status
        btnDeactivate.setText(user.isActive() ? "Deactivate Account" : "Activate Account");
    }

    private String formatUserType(String userType) {
        if ("EVOwner".equals(userType)) {
            return "EV Owner";
        } else if ("StationOperator".equals(userType)) {
            return "Station Operator";
        }
        return userType;
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
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Confirm Account Action");
        builder.setMessage("Are you sure you want to " + (btnDeactivate.getText().toString().contains("Deactivate") ? "deactivate" : "activate") + " your account?");
        builder.setPositiveButton("Yes", (dialog, which) -> performAccountStatusChange());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performAccountStatusChange() {
        boolean newStatus = !btnDeactivate.getText().toString().contains("Deactivate");

        // Debug: Log what we're trying to do
        android.util.Log.d("ProfileActivity", "Attempting to change status for NIC: " + currentUserNIC + " to: " + newStatus);

        // Call server API to update status
        com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
        String statusString = newStatus ? "true" : "false";
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), statusString);
        retrofit2.Call<okhttp3.ResponseBody> call = api.updateEVOwnerStatus(currentUserNIC, requestBody);

        // Debug: Log the API call details
        android.util.Log.d("ProfileActivity", "Making API call to update status with body: " + statusString);

        call.enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Get the response body as string
                    String responseMessage = "Account status updated successfully";
                    try {
                        if (response.body() != null) {
                            responseMessage = response.body().string();
                        }
                    } catch (Exception e) {
                        android.util.Log.w("ProfileActivity", "Could not read response body: " + e.getMessage());
                    }
                    // Update local database
                    new Thread(() -> {
                        User user = userDao.getUserByNIC(currentUserNIC);
                        if (user != null) {
                            user.setActive(newStatus);
                            userDao.update(user);
                        }

                        runOnUiThread(() -> {
                            String message = newStatus ? "Account activated successfully" : "Account deactivated successfully";
                            Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();

                            if (!newStatus) {
                                // Logout if account is deactivated
                                prefs.clearUserData();
                                Toast.makeText(ProfileActivity.this, "You have been logged out", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                                finish();
                            } else {
                                loadUserProfile();
                            }
                        });
                    }).start();
                } else {
                    // Debug: Log the error response
                    String errorMsg = "Failed to update account status";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ": " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg += " (could not read error body)";
                        }
                    }
                    errorMsg += " (Code: " + response.code() + ")";

                    String finalErrorMsg = errorMsg;
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, finalErrorMsg, Toast.LENGTH_LONG).show());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                // Debug: Log the actual network error
                String errorMsg = "Network error: " + t.getClass().getSimpleName() + " - " + t.getMessage();
                if (t.getCause() != null) {
                    errorMsg += " (Cause: " + t.getCause().getMessage() + ")";
                }
                
                String finalErrorMsg = errorMsg;
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, finalErrorMsg, Toast.LENGTH_LONG).show());
            }
        });
    }
}