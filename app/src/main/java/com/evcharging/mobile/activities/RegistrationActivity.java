package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.database.AppDatabase;
import com.evcharging.mobile.database.UserDao;
import com.evcharging.mobile.models.User;
import com.evcharging.mobile.utils.NetworkUtils;
import com.evcharging.mobile.utils.SharedPreferencesHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationActivity extends AppCompatActivity {

    private TextInputEditText etNIC, etFirstName, etLastName, etEmail, etPhone, etVehicleModel, etVehiclePlate, etPassword, etConfirmPassword;
    private Button btnRegister;
    private UserDao userDao;
    private ApiService apiService;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        initializeViews();
        setupDatabase();
        setupClickListeners();
    }

    private void initializeViews() {
        etNIC = findViewById(R.id.etNIC);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etVehicleModel = findViewById(R.id.etVehicleModel);
        etVehiclePlate = findViewById(R.id.etVehiclePlate);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

    prefs = new SharedPreferencesHelper(this);
    apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void setupDatabase() {
        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());
    }

    private void attemptRegistration() {
        if (!validateForm()) {
            return;
        }

        User user = new User(
                etNIC.getText().toString().trim(),
                etFirstName.getText().toString().trim(),
                etLastName.getText().toString().trim(),
                etEmail.getText().toString().trim(),
                etPhone.getText().toString().trim(),
                etVehicleModel.getText().toString().trim(),
                etVehiclePlate.getText().toString().trim(),
                true, // isActive
                "EVOwner"
        );

        String password = etPassword.getText().toString().trim();

        if (NetworkUtils.isNetworkAvailable(this)) {
            registerWithApi(user);
        } else {
            registerOffline(user, password);
        }
    }

    private boolean validateForm() {
        String nic = etNIC.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (nic.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
                email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerWithApi(User user) {
        // Capture the plain password so we can store it locally after a successful API response
        final String plainPassword = etPassword.getText().toString().trim();

        // Don't send the plain password to the server (server may not expect it). Clear it from payload.
        user.setPassword(null);

        // Debug: log JSON payload being sent
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String payload = gson.toJson(user);
            Log.d("REGISTRATION", "Outgoing payload: " + payload);
        } catch (Exception ignored) {}

        Call<User> call = apiService.createEVOwner(user);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                Log.d("REGISTRATION", "Response Code: " + response.code());
                Log.d("REGISTRATION", "Response Message: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    User registeredUser = response.body();
                    // Ensure local copy includes the plain password so local login works
                    registeredUser.setPassword(plainPassword);
                    Log.d("REGISTRATION", "✅ Registration successful via API");

                    // Save user locally
                    saveUserLocally(registeredUser);

                    Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Auto-login after registration
                    prefs.setLoggedIn(true);
                    prefs.setLoggedInUserNIC(registeredUser.getNic());
                    prefs.setUserType("EVOwner");

                    startActivity(new Intent(RegistrationActivity.this, EVOwnerDashboardActivity.class));
                    finish();
                } else {
                    // Get detailed error message
                    String errorMessage = "Registration failed: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMessage += " - " + errorBody;
                            Log.e("REGISTRATION", "Error body: " + errorBody);
                        }
                    } catch (IOException e) {
                        Log.e("REGISTRATION", "Error reading error body: " + e.getMessage());
                    }

                    Toast.makeText(RegistrationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("REGISTRATION", "❌ Registration failed: " + errorMessage);

                    // Fallback to offline registration
                    registerOffline(user, etPassword.getText().toString().trim());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e("REGISTRATION", "❌ Network error: " + t.getMessage());
                t.printStackTrace();
                Toast.makeText(RegistrationActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                // Fallback to offline registration
                registerOffline(user, etPassword.getText().toString().trim());
            }
        });
    }

    private void registerOffline(User user, String password) {
        new Thread(() -> {
            // Check if user already exists
            if (userDao.userExists(user.getNic()) > 0) {
                runOnUiThread(() ->
                        Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show());
                return;
            }

            // Save user locally
            user.setPassword(password);
            userDao.insert(user);

            runOnUiThread(() -> {
                Toast.makeText(this, "Registration successful (offline)", Toast.LENGTH_SHORT).show();

                // Auto-login after registration
                prefs.setLoggedIn(true);
                prefs.setLoggedInUserNIC(user.getNic());
                prefs.setUserType("EVOwner");

                startActivity(new Intent(this, EVOwnerDashboardActivity.class));
                finish();
            });
        }).start();
    }

    private void saveUserLocally(User user) {
        new Thread(() -> {
            userDao.insert(user);
        }).start();
    }
}