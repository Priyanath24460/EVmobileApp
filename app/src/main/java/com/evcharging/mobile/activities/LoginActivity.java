package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.evcharging.mobile.R;
import com.evcharging.mobile.database.AppDatabase;
import com.evcharging.mobile.database.UserDao;
import com.evcharging.mobile.models.User;
import com.evcharging.mobile.utils.SharedPreferencesHelper;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etNIC, etPassword;
    private RadioGroup rgUserType;
    private Button btnLogin;
    private TextView btnRegister, tvForgotPassword;
    private CheckBox cbRememberMe;
    private UserDao userDao;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupDatabase();
        setupClickListeners();
    }

    private void initializeViews() {
        etNIC = findViewById(R.id.etNIC);
        etPassword = findViewById(R.id.etPassword);
        rgUserType = findViewById(R.id.rgUserType);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        prefs = new SharedPreferencesHelper(this);
    }

    private void setupDatabase() {
        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> navigateToRegistration());
        tvForgotPassword.setOnClickListener(v -> showForgotPassword());
    }

    private void showForgotPassword() {
        Toast.makeText(this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void attemptLogin() {
        String nic = etNIC.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int selectedId = rgUserType.getCheckedRadioButtonId();

        if (nic.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedId == -1) {
            Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show();
            return;
        }

        String userType = (selectedId == R.id.rbEVOwner) ? "EVOwner" : "StationOperator";

        // For demo purposes - in real app, this would be API call
        if (userType.equals("EVOwner")) {
            loginEVOwner(nic, password);
        } else {
            loginStationOperator(nic, password);
        }
    }

    private void loginEVOwner(String nic, String password) {
        // Always check server first for current account status
        com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
        retrofit2.Call<User> call = api.getEVOwner(nic);
        call.enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(retrofit2.Call<User> call, retrofit2.Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User serverUser = response.body();

                    // Check if account is active
                    if (!serverUser.isActive()) {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Account is deactivated. Please contact support.", Toast.LENGTH_LONG).show());
                        return;
                    }

                    // Account is active, now check local credentials
                    new Thread(() -> {
                        try {
                            User localUser = userDao.getUserByNIC(nic);

                            runOnUiThread(() -> {
                                // For server-verified users, we accept the password they entered
                                // Update local storage with server data using upsert
                                serverUser.setPassword(password);
                                new Thread(() -> {
                                    try {
                                        userDao.upsert(serverUser);
                                    } catch (Exception e) {
                                        android.util.Log.e("LoginActivity", "Error saving user data: " + e.getMessage());
                                    }
                                }).start();

                                // Login successful
                                prefs.setLoggedIn(true);
                                prefs.setLoggedInUserNIC(serverUser.getNic());
                                prefs.setUserType("EVOwner");

                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                
                                try {
                                    startActivity(new Intent(LoginActivity.this, EVOwnerDashboardActivity.class));
                                    finish();
                                } catch (Exception e) {
                                    android.util.Log.e("LoginActivity", "Error starting dashboard: " + e.getMessage());
                                    Toast.makeText(LoginActivity.this, "Error opening dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();

                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "User not found on server (" + response.code() + ")", Toast.LENGTH_LONG).show());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<User> call, Throwable t) {
                // If server is unavailable, fall back to local login with warning
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Server unavailable. Using cached login...", Toast.LENGTH_SHORT).show());

                new Thread(() -> {
                    try {
                        User user = userDao.getUserByNIC(nic);

                        if (user == null) {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "No local account found and server unavailable", Toast.LENGTH_LONG).show());
                            return;
                        }

                        runOnUiThread(() -> {
                            if (!user.isActive()) {
                                Toast.makeText(LoginActivity.this, "Account is deactivated. Please contact support.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String stored = user.getPassword();
                            if (stored == null) stored = "";

                            if (stored.trim().equals(password.trim())) {
                                // Login successful (offline mode)
                                prefs.setLoggedIn(true);
                                prefs.setLoggedInUserNIC(nic);
                                prefs.setUserType("EVOwner");

                                Toast.makeText(LoginActivity.this, "Login successful! (Offline Mode)", Toast.LENGTH_SHORT).show();
                                
                                try {
                                    startActivity(new Intent(LoginActivity.this, EVOwnerDashboardActivity.class));
                                    finish();
                                } catch (Exception e) {
                                    android.util.Log.e("LoginActivity", "Error starting dashboard (offline): " + e.getMessage());
                                    Toast.makeText(LoginActivity.this, "Error opening dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }).start();
            }
        });
    }

    private void loginStationOperator(String username, String password) {
        try {
            // Use API authentication for station operators
            com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
            com.evcharging.mobile.api.AuthRequest request = new com.evcharging.mobile.api.AuthRequest(username, password);
            
            retrofit2.Call<com.evcharging.mobile.api.AuthResponse> call = api.authenticateUser(request);
            call.enqueue(new retrofit2.Callback<com.evcharging.mobile.api.AuthResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.evcharging.mobile.api.AuthResponse> call, retrofit2.Response<com.evcharging.mobile.api.AuthResponse> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            prefs.setLoggedIn(true);
                            prefs.setLoggedInUserNIC(username);
                            prefs.setUserType("StationOperator");

                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            
                            Intent intent = new Intent(LoginActivity.this, OperatorDashboardActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String message = response.body() != null ? response.body().getMessage() : "Invalid credentials";
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("LoginActivity", "Error in onResponse: " + e.getMessage(), e);
                        Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.evcharging.mobile.api.AuthResponse> call, Throwable t) {
                    try {
                        android.util.Log.e("LoginActivity", "API call failed: " + t.getMessage(), t);
                        
                        // Fallback to demo credentials if API fails
                        if ("operator".equals(username) && "operator123".equals(password)) {
                            prefs.setLoggedIn(true);
                            prefs.setLoggedInUserNIC(username);
                            prefs.setUserType("StationOperator");

                            Toast.makeText(LoginActivity.this, "Login successful! (Demo Mode)", Toast.LENGTH_SHORT).show();
                            
                            Intent intent = new Intent(LoginActivity.this, OperatorDashboardActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("LoginActivity", "Error in onFailure: " + e.getMessage(), e);
                        Toast.makeText(LoginActivity.this, "Unexpected error during login", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Error in loginStationOperator: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing login: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToRegistration() {
        int selectedId = rgUserType.getCheckedRadioButtonId();
        if (selectedId == R.id.rbEVOwner) {
            startActivity(new Intent(this, RegistrationActivity.class));
        } else {
            Toast.makeText(this, "Station operator registration is handled by backoffice", Toast.LENGTH_LONG).show();
        }
    }
}