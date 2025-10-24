package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.TextView;
import android.widget.LinearLayout;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.utils.SharedPreferencesHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.MediaType;

public class OperatorProfileActivity extends AppCompatActivity {

    private TextView tvAvatarLetter, tvOperatorName;
    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnEdit, btnSave, btnCancel, btnLogout;
    private LinearLayout layoutEditButtons;
    private Toolbar toolbar;
    
    private SharedPreferencesHelper prefs;
    private ApiService apiService;
    private String currentUsername;
    private String originalUsername, originalPassword;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_profile);

        initializeViews();
        setupToolbar();
        loadOperatorData();
        setupClickListeners();
    }

    private void initializeViews() {
        tvAvatarLetter = findViewById(R.id.tvAvatarLetter);
        tvOperatorName = findViewById(R.id.tvOperatorName);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnLogout = findViewById(R.id.btnLogout);
        layoutEditButtons = findViewById(R.id.layoutEditButtons);
        toolbar = findViewById(R.id.toolbar);

        prefs = new SharedPreferencesHelper(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            if (isEditMode) {
                cancelEdit();
            } else {
                finish();
            }
        });
    }

    private void loadOperatorData() {
        currentUsername = prefs.getLoggedInUserNIC();
        
        if (currentUsername != null) {
            // Set username
            etUsername.setText(currentUsername);
            originalUsername = currentUsername;
            
            // Set placeholder password (we don't store actual passwords)
            etPassword.setText("••••••••");
            originalPassword = "";
            
            // Set avatar letter
            if (!currentUsername.isEmpty()) {
                tvAvatarLetter.setText(String.valueOf(currentUsername.charAt(0)).toUpperCase());
            }
            
            // Set operator name
            tvOperatorName.setText("Station Operator");
        }
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> enterEditMode());
        btnSave.setOnClickListener(v -> saveCredentials());
        btnCancel.setOnClickListener(v -> cancelEdit());
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void enterEditMode() {
        isEditMode = true;
        
        // Enable text fields
        etUsername.setEnabled(true);
        etPassword.setEnabled(true);
        etPassword.setText(""); // Clear password field for new input
        
        // Show/hide buttons
        btnEdit.setVisibility(View.GONE);
        layoutEditButtons.setVisibility(View.VISIBLE);
        
        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Credentials");
        }
        
        Toast.makeText(this, "Enter your new credentials", Toast.LENGTH_SHORT).show();
    }

    private void cancelEdit() {
        isEditMode = false;
        
        // Restore original values
        etUsername.setText(originalUsername);
        etPassword.setText("••••••••");
        
        // Disable text fields
        etUsername.setEnabled(false);
        etPassword.setEnabled(false);
        
        // Show/hide buttons
        btnEdit.setVisibility(View.VISIBLE);
        layoutEditButtons.setVisibility(View.GONE);
        
        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Station Operator Profile");
        }
    }

    private void saveCredentials() {
        String newUsername = etUsername.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();
        
        // Validation
        if (newUsername.isEmpty()) {
            tilUsername.setError("Username cannot be empty");
            return;
        }
        
        if (newPassword.isEmpty()) {
            tilPassword.setError("Password cannot be empty");
            return;
        }
        
        if (newPassword.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }
        
        // Clear errors
        tilUsername.setError(null);
        tilPassword.setError(null);
        
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Credentials")
                .setMessage("Are you sure you want to update your login credentials?\n\nNote: You will need to use the new credentials for future logins.")
                .setPositiveButton("Update", (dialog, which) -> updateCredentialsOnServer(newUsername, newPassword))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCredentialsOnServer(String newUsername, String newPassword) {
        // Disable buttons during update
        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);
        
        // Create JSON request body
        String jsonBody = String.format(
            "{\"username\":\"%s\",\"password\":\"%s\"}", 
            newUsername, newPassword
        );
        
        RequestBody requestBody = RequestBody.create(
            MediaType.parse("application/json"), 
            jsonBody
        );
        
        // Call API to update credentials
        Call<ResponseBody> call = apiService.updateOperatorCredentials(currentUsername, requestBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        // Update successful
                        originalUsername = newUsername;
                        currentUsername = newUsername;
                        
                        // Update shared preferences
                        prefs.setLoggedInUserNIC(newUsername);
                        
                        // Update avatar
                        if (!newUsername.isEmpty()) {
                            tvAvatarLetter.setText(String.valueOf(newUsername.charAt(0)).toUpperCase());
                        }
                        
                        // Exit edit mode
                        cancelEdit();
                        
                        Toast.makeText(OperatorProfileActivity.this, 
                            "Credentials updated successfully!", Toast.LENGTH_LONG).show();
                        
                    } else {
                        // Handle error
                        String errorMessage = "Failed to update credentials";
                        try {
                            if (response.errorBody() != null) {
                                errorMessage = response.errorBody().string();
                            }
                        } catch (Exception e) {
                            // Use default message
                        }
                        
                        Toast.makeText(OperatorProfileActivity.this, 
                            errorMessage, Toast.LENGTH_LONG).show();
                    }
                    
                    // Re-enable buttons
                    btnSave.setEnabled(true);
                    btnCancel.setEnabled(true);
                });
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(OperatorProfileActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    
                    // Re-enable buttons
                    btnSave.setEnabled(true);
                    btnCancel.setEnabled(true);
                });
            }
        });
    }

    private void performLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    prefs.clearUserData();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            cancelEdit();
        } else {
            super.onBackPressed();
        }
    }
}