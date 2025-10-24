package com.evcharging.mobile.utils;

import android.content.Context;
import android.util.Log;

import com.evcharging.mobile.database.AppDatabase;
import com.evcharging.mobile.database.UserDao;
import com.evcharging.mobile.models.User;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.api.AuthRequest;
import com.evcharging.mobile.api.AuthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class for managing station operator credentials and offline authentication
 */
public class StationOperatorManager {
    private static final String TAG = "StationOperatorManager";
    private UserDao userDao;
    private ApiService apiService;
    private Context context;

    public StationOperatorManager(Context context) {
        this.context = context;
        this.userDao = AppDatabase.getInstance(context).userDao();
        this.apiService = ApiClient.getClient(context).create(ApiService.class);
    }

    /**
     * Interface for login callbacks
     */
    public interface LoginCallback {
        void onSuccess(User operator, boolean isOfflineMode);
        void onError(String message);
    }

    /**
     * Authenticate station operator with server-first approach and local fallback
     */
    public void authenticateOperator(String username, String password, LoginCallback callback) {
        // Try server authentication first
        AuthRequest request = new AuthRequest(username, password);
        Call<AuthResponse> call = apiService.authenticateUser(request);
        
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Server authentication successful
                    User operator = createOperatorUser(username, password);
                    
                    // Save to local cache
                    new Thread(() -> {
                        try {
                            userDao.upsert(operator);
                            Log.d(TAG, "Operator credentials cached locally: " + username);
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching operator credentials: " + e.getMessage());
                        }
                    }).start();
                    
                    callback.onSuccess(operator, false); // Not offline mode
                } else {
                    String message = response.body() != null ? response.body().getMessage() : "Invalid credentials";
                    callback.onError(message);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.d(TAG, "Server unavailable, checking local credentials for: " + username);
                
                // Server unavailable - check local cache
                new Thread(() -> {
                    try {
                        User localOperator = userDao.loginStationOperator(username, password);
                        
                        if (localOperator != null && localOperator.isActive()) {
                            callback.onSuccess(localOperator, true); // Offline mode
                        } else {
                            // Check demo credentials as last resort
                            if (isDemoCredentials(username, password)) {
                                User demoOperator = createOperatorUser(username, password);
                                userDao.upsert(demoOperator);
                                callback.onSuccess(demoOperator, true); // Offline mode
                            } else {
                                callback.onError("No cached credentials found. Please connect to internet for first-time login.");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Database error during offline login: " + e.getMessage());
                        callback.onError("Database error: " + e.getMessage());
                    }
                }).start();
            }
        });
    }

    /**
     * Create a User object for station operator
     */
    private User createOperatorUser(String username, String password) {
        User operator = new User();
        operator.setNic(username);
        operator.setFirstName(isDemoCredentials(username, password) ? "Demo" : "Station");
        operator.setLastName("Operator");
        operator.setEmail(username + "@station.local");
        operator.setUserType("StationOperator");
        operator.setPassword(password);
        operator.setActive(true);
        return operator;
    }

    /**
     * Check if credentials are demo credentials
     */
    private boolean isDemoCredentials(String username, String password) {
        return "operator".equals(username) && "operator123".equals(password);
    }

    /**
     * Get cached station operator by username
     */
    public void getCachedOperator(String username, OperatorCallback callback) {
        new Thread(() -> {
            try {
                User operator = userDao.getStationOperatorByUsername(username);
                callback.onResult(operator);
            } catch (Exception e) {
                Log.e(TAG, "Error getting cached operator: " + e.getMessage());
                callback.onResult(null);
            }
        }).start();
    }

    /**
     * Update station operator password
     */
    public void updateOperatorPassword(String username, String newPassword, UpdateCallback callback) {
        new Thread(() -> {
            try {
                userDao.updateStationOperatorPassword(username, newPassword);
                Log.d(TAG, "Password updated for operator: " + username);
                callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Error updating password: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Clear cached credentials for a specific operator
     */
    public void clearOperatorCredentials(String username) {
        new Thread(() -> {
            try {
                userDao.deleteStationOperator(username);
                Log.d(TAG, "Cleared credentials for operator: " + username);
            } catch (Exception e) {
                Log.e(TAG, "Error clearing credentials: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Clear all cached station operator credentials
     */
    public void clearAllOperatorCredentials() {
        new Thread(() -> {
            try {
                userDao.clearAllStationOperators();
                Log.d(TAG, "Cleared all station operator credentials");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing all credentials: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Callback interfaces
     */
    public interface OperatorCallback {
        void onResult(User operator);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(String message);
    }
}