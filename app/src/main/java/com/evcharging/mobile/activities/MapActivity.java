package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.models.ChargingStation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private WebView mapWebView;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;
    private boolean mapReady = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        
        try {
            setContentView(R.layout.activity_map);
            Log.d(TAG, "Layout set successfully");

            initializeViews();
            checkLocationPermission();
            
            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing map: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish(); // Close activity if there's a critical error
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeViews() {
        Log.d(TAG, "Initializing WebView map");
        
        try {
            mapWebView = findViewById(R.id.mapWebView);
            if (mapWebView != null) {
                Log.d(TAG, "WebView found, configuring...");
                
                // Configure WebView settings
                WebSettings webSettings = mapWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setAllowFileAccess(true);
                webSettings.setAllowContentAccess(true);
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                
                // Add JavaScript interface
                mapWebView.addJavascriptInterface(new MapJavaScriptInterface(), "Android");
                
                // Set WebView client
                mapWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Log.d(TAG, "=== MAP PAGE FINISHED LOADING ===");
                        mapReady = true;
                        
                        // Test JavaScript interface immediately
                        view.evaluateJavascript("console.log('JavaScript interface test from Android')", null);
                        
                        // Load stations after map is ready
                        loadAllStations();
                        
                        // Test location detection automatically
                        new android.os.Handler().postDelayed(() -> {
                            Log.d(TAG, "=== AUTO TESTING LOCATION AFTER MAP LOAD ===");
                            checkLocationPermission();
                        }, 2000);
                    }
                    
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Log.e(TAG, "WebView error [" + errorCode + "]: " + description + " URL: " + failingUrl);
                        Toast.makeText(MapActivity.this, "Error loading map: " + description, Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override 
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.d(TAG, "URL loading: " + url);
                        return false;
                    }
                });
                
                // Load the map HTML file
                mapWebView.loadUrl("file:///android_asset/map.html");
                Log.d(TAG, "Map HTML loaded");
                
            } else {
                Log.e(TAG, "WebView is null! Check layout file.");
                Toast.makeText(this, "Error: Map WebView not found", Toast.LENGTH_LONG).show();
            }

            // Initialize API service
            try {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                apiService = ApiClient.getClient(this).create(ApiService.class);
                Log.d(TAG, "API services initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing API services: " + e.getMessage(), e);
            }
            
            Log.d(TAG, "MapActivity initialization completed");
        } catch (Exception e) {
            Log.e(TAG, "Critical error in initializeViews: " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up map view: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // JavaScript interface for WebView communication
    public class MapJavaScriptInterface {
        @JavascriptInterface
        public void onMapReady() {
            Log.d(TAG, "Map is ready from JavaScript");
            mapReady = true;
            
            // Start loading data now that map is ready
            runOnUiThread(() -> {
                Log.d(TAG, "Starting data loading and location detection...");
                loadAllStations();
                
                // Check location permission and get location
                if (hasLocationPermission()) {
                    Log.d(TAG, "Location permission already granted, getting location");
                    getCurrentLocation();
                } else {
                    Log.d(TAG, "Location permission not granted, requesting permission");
                    checkLocationPermission();
                }
            });
        }
        
        @JavascriptInterface
        public void testLocation() {
            // Test method to manually trigger location
            runOnUiThread(() -> {
                Log.d(TAG, "Manual location test triggered");
                
                if (hasLocationPermission()) {
                    getCurrentLocation();
                } else {
                    Log.d(TAG, "No location permission, using test location");
                    // Use Colombo as test location if no permission
                    sendLocationToMap(6.9271, 79.8612);
                    Toast.makeText(MapActivity.this, "Using test location (Colombo)", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @JavascriptInterface 
        public void debugInfo() {
            runOnUiThread(() -> {
                String info = "Map Ready: " + mapReady + 
                            ", Location Permission: " + hasLocationPermission() + 
                            ", WebView: " + (mapWebView != null);
                Log.d(TAG, "Debug Info: " + info);
                Toast.makeText(MapActivity.this, info, Toast.LENGTH_LONG).show();
            });
        }
        
        @JavascriptInterface
        public void onMarkerClick(String stationId) {
            Log.d(TAG, "Marker clicked for station: " + stationId);
            runOnUiThread(() -> {
                Toast.makeText(MapActivity.this, "Selected station: " + stationId, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void checkLocationPermission() {
        Log.d(TAG, "=== CHECKING LOCATION PERMISSION ===");
        Log.d(TAG, "Fine location: " + (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
        Log.d(TAG, "Coarse location: " + (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED));
        Log.d(TAG, "Map ready: " + mapReady);
        
        if (!hasLocationPermission()) {
            Log.d(TAG, "=== REQUESTING LOCATION PERMISSION ===");
            ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "=== LOCATION PERMISSION ALREADY GRANTED ===");
            if (mapReady) {
                getCurrentLocation();
            } else {
                Log.d(TAG, "Map not ready yet, location will be requested when map is ready");
            }
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "=== PERMISSION RESULT ===");
        Log.d(TAG, "Request code: " + requestCode + " (expected: " + LOCATION_PERMISSION_REQUEST_CODE + ")");
        Log.d(TAG, "Results count: " + grantResults.length);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "=== LOCATION PERMISSION GRANTED ===");
                getCurrentLocation();
            } else {
                Log.d(TAG, "=== LOCATION PERMISSION DENIED ===");
                Toast.makeText(this, "Location permission denied - using test location", Toast.LENGTH_LONG).show();
                // Use test location as fallback
                sendLocationToMap(6.9271, 79.8612);
            }
        }
    }

    private void getCurrentLocation() {
        Log.d(TAG, "=== GET CURRENT LOCATION ===");
        
        if (!hasLocationPermission()) {
            Log.w(TAG, "=== NO LOCATION PERMISSION ===");
            Toast.makeText(this, "No location permission - using test location", Toast.LENGTH_SHORT).show();
            sendLocationToMap(6.9271, 79.8612); // Fallback to Colombo
            return;
        }

        Log.d(TAG, "Getting current location with permission...");
        
        try {
            if (fusedLocationClient == null) {
                Log.e(TAG, "=== FUSED LOCATION CLIENT IS NULL ===");
                Toast.makeText(this, "Location service not available", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Try to get last known location first
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    Log.d(TAG, "=== GOT LAST KNOWN LOCATION ===");
                    Log.d(TAG, "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                    Log.d(TAG, "Accuracy: " + location.getAccuracy() + "m, Time: " + new java.util.Date(location.getTime()));
                    sendLocationToMap(location.getLatitude(), location.getLongitude());
                } else {
                    Log.w(TAG, "=== LAST LOCATION IS NULL - REQUESTING FRESH ===");
                    requestFreshLocation();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "=== FAILED TO GET LAST LOCATION ===");
                Log.e(TAG, "Error: " + e.getMessage());
                e.printStackTrace();
                requestFreshLocation();
            });
        } catch (SecurityException e) {
            Log.e(TAG, "=== SECURITY EXCEPTION ===");
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(this, "Location permission error", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "=== UNEXPECTED ERROR IN GET LOCATION ===");
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void requestFreshLocation() {
        if (!hasLocationPermission()) {
            return;
        }

        try {
            Log.d(TAG, "Requesting fresh location update...");
            
            // Import required for LocationRequest
            com.google.android.gms.location.LocationRequest locationRequest = 
                new com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                    .setMinUpdateIntervalMillis(5000L)
                    .setMaxUpdates(1)
                    .build();

            com.google.android.gms.location.LocationCallback locationCallback = 
                new com.google.android.gms.location.LocationCallback() {
                    @Override
                    public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                        if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                            Location location = locationResult.getLastLocation();
                            Log.d(TAG, "Fresh location received: " + location.getLatitude() + ", " + location.getLongitude());
                            sendLocationToMap(location.getLatitude(), location.getLongitude());
                            
                            // Stop location updates
                            fusedLocationClient.removeLocationUpdates(this);
                        }
                    }
                };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
            
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException requesting fresh location: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting fresh location: " + e.getMessage());
        }
    }

    private void sendLocationToMap(double latitude, double longitude) {
        Log.d(TAG, "=== SEND LOCATION TO MAP ===");
        Log.d(TAG, "Location: " + latitude + ", " + longitude);
        Log.d(TAG, "Map ready: " + mapReady + ", WebView null: " + (mapWebView == null));
        
        if (mapWebView != null && mapReady) {
            String jsCode = "if (window.Android && window.Android.setUserLocation) { " +
                           "window.Android.setUserLocation(" + latitude + ", " + longitude + "); " +
                           "console.log('Location received from Android: " + latitude + ", " + longitude + "'); " +
                           "} else if (window.AndroidInterface && window.AndroidInterface.setUserLocation) { " +
                           "window.AndroidInterface.setUserLocation(" + latitude + ", " + longitude + "); " +
                           "console.log('Using AndroidInterface fallback'); " +
                           "} else { console.error('No Android interface available'); }";
            
            runOnUiThread(() -> {
                Log.d(TAG, "=== EXECUTING JAVASCRIPT ===");
                Log.d(TAG, "JS Code: " + jsCode);
                
                mapWebView.evaluateJavascript(jsCode, result -> {
                    Log.d(TAG, "JavaScript execution result: " + result);
                });
                
                Log.d(TAG, "=== LOCATION SENT TO MAP ===");
                Toast.makeText(this, "📍 Location: " + String.format("%.4f, %.4f", latitude, longitude), Toast.LENGTH_LONG).show();
            });
        } else {
            Log.w(TAG, "=== CANNOT SEND LOCATION - MAP NOT READY ===");
            Log.w(TAG, "Map ready: " + mapReady + ", WebView null: " + (mapWebView == null));
            
            // If location is available but map not ready, store it for later
            if (mapWebView != null) {
                Log.d(TAG, "Retrying in 2 seconds...");
                new android.os.Handler().postDelayed(() -> {
                    Log.d(TAG, "=== RETRY SENDING LOCATION ===");
                    sendLocationToMap(latitude, longitude);
                }, 2000);
            }
        }
    }



    private void loadAllStations() {
        Log.d(TAG, "Loading all stations from API...");
        
        if (!mapReady) {
            Log.w(TAG, "Map not ready yet, stations will load when map is ready");
            return;
        }
        
        // Load all available charging stations from the API
        Call<List<ChargingStation>> call = apiService.getActiveStations();
        call.enqueue(new Callback<List<ChargingStation>>() {
            @Override
            public void onResponse(Call<List<ChargingStation>> call, Response<List<ChargingStation>> response) {
                Log.d(TAG, "API Response received. Code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    List<ChargingStation> stations = response.body();
                    Log.d(TAG, "Stations received: " + (stations != null ? stations.size() : 0));
                    
                    if (stations != null && !stations.isEmpty()) {
                        Log.d(TAG, "Loading " + stations.size() + " real stations to map");
                        loadStationsToMap(stations);
                        Toast.makeText(MapActivity.this, "Loaded " + stations.size() + " charging stations", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "No active stations found, map will show demo data");
                        Toast.makeText(MapActivity.this, "No active stations found, showing demo data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "API call failed with code: " + response.code() + ", message: " + response.message());
                    Toast.makeText(MapActivity.this, "Server unavailable, showing demo data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ChargingStation>> call, Throwable t) {
                Log.e(TAG, "Network error loading stations: " + t.getMessage());
                Toast.makeText(MapActivity.this, "Network error, showing demo data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStationsToMap(List<ChargingStation> stations) {
        if (mapWebView != null && mapReady) {
            try {
                // Convert stations to JSON
                Gson gson = new Gson();
                String stationsJson = gson.toJson(stations);
                
                // Call JavaScript function to load stations
                String jsCode = "if (window.Android && window.Android.loadStations) { " +
                               "window.Android.loadStations('" + stationsJson.replace("'", "\\'") + "'); " +
                               "} else if (window.AndroidInterface && window.AndroidInterface.loadStations) { " +
                               "window.AndroidInterface.loadStations('" + stationsJson.replace("'", "\\'") + "'); " +
                               "} else { console.error('No Android interface for loading stations'); }";
                
                runOnUiThread(() -> {
                    mapWebView.evaluateJavascript(jsCode, null);
                    Log.d(TAG, "Stations sent to map via JavaScript");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading stations to map: " + e.getMessage(), e);
            }
        }
    }

    // Mock stations are now loaded via JavaScript on the map

    // Station display is now handled by the Leaflet.js map in WebView

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // WebView cleanup handled automatically
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapWebView != null) {
            mapWebView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapWebView != null) {
            mapWebView.onPause();
        }
    }
}