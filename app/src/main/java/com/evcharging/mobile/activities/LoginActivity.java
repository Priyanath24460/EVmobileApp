package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
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
    private Button btnLogin, btnRegister;
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

        prefs = new SharedPreferencesHelper(this);
    }

    private void setupDatabase() {
        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> navigateToRegistration());
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
        new Thread(() -> {
            try {
                // Fetch by NIC first (more robust) then compare password in Java
                User user = userDao.getUserByNIC(nic);

                if (user == null) {
                    // Try to fetch from server and import
                    runOnUiThread(() -> Toast.makeText(this, "User not found locally. Trying server...", Toast.LENGTH_SHORT).show());

                    com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
                    retrofit2.Call<User> call = api.getEVOwner(nic);
                    call.enqueue(new retrofit2.Callback<User>() {
                        @Override
                        public void onResponse(retrofit2.Call<User> call, retrofit2.Response<User> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                User serverUser = response.body();
                                // Save local copy with entered password so local login works
                                serverUser.setPassword(password);
                                new Thread(() -> userDao.insert(serverUser)).start();

                                // Auto-login
                                prefs.setLoggedIn(true);
                                prefs.setLoggedInUserNIC(serverUser.getNic());
                                prefs.setUserType("EVOwner");

                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, "Imported account from server. Logged in.", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, EVOwnerDashboardActivity.class));
                                    finish();
                                });
                            } else {
                                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "User not found on server (" + response.code() + ")", Toast.LENGTH_LONG).show());
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<User> call, Throwable t) {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Server fetch failed: " + t.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });

                    return;
                }

                runOnUiThread(() -> {
                    if (!user.isActive()) {
                        Toast.makeText(this, "Account is inactive. Contact support.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String stored = user.getPassword();
                    if (stored == null) stored = "";

                    if (stored.trim().equals(password.trim())) {
                        // Login successful
                        prefs.setLoggedIn(true);
                        prefs.setLoggedInUserNIC(nic);
                        prefs.setUserType("EVOwner");

                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, EVOwnerDashboardActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Invalid credentials. If you registered via the server, try registering again or use 'Forgot password' if available.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void loginStationOperator(String username, String password) {
        // Use API authentication for station operators
        com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
        com.evcharging.mobile.api.AuthRequest request = new com.evcharging.mobile.api.AuthRequest(username, password);
        
        retrofit2.Call<com.evcharging.mobile.api.AuthResponse> call = api.authenticateUser(request);
        call.enqueue(new retrofit2.Callback<com.evcharging.mobile.api.AuthResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.evcharging.mobile.api.AuthResponse> call, retrofit2.Response<com.evcharging.mobile.api.AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    prefs.setLoggedIn(true);
                    prefs.setLoggedInUserNIC(username);
                    prefs.setUserType("StationOperator");

                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, OperatorDashboardActivity.class));
                    finish();
                } else {
                    String message = response.body() != null ? response.body().getMessage() : "Invalid credentials";
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.evcharging.mobile.api.AuthResponse> call, Throwable t) {
                // Fallback to demo credentials if API fails
                if ("operator".equals(username) && "operator123".equals(password)) {
                    prefs.setLoggedIn(true);
                    prefs.setLoggedInUserNIC(username);
                    prefs.setUserType("StationOperator");

                    Toast.makeText(LoginActivity.this, "Login successful! (Demo Mode)", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, OperatorDashboardActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
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