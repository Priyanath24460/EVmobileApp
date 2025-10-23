package com.evcharging.mobile.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.models.ChargingStation;
import com.evcharging.mobile.models.TimeSlotAvailability;
import com.evcharging.mobile.models.BookingRequest;
import com.evcharging.mobile.models.BookingResponse;
import com.evcharging.mobile.models.ChargingPointSlots;
import com.evcharging.mobile.models.TimeSlot;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.SharedPreferencesHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private static final String TAG = "BookingActivity";
    
    // UI Components
    private TextInputEditText etStation, etDate;
    private Button btnSelectStation, btnSelectDate, btnCancel, btnBookNow;
    private Button btnViewAll, btnNearby;
    private ProgressBar progressBar;
    private LinearLayout layoutTimeSlots, containerChargingPoints;
    private WebView mapWebView;

    // Map Components
    private FusedLocationProviderClient fusedLocationClient;
    private boolean mapReady = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // Data
    private SharedPreferencesHelper prefs;
    private ApiService apiService;
    private ChargingStation selectedStation;
    private String selectedDate;
    private TimeSlotAvailability availability;
    private int selectedChargingPoint = 0;
    private TimeSlot selectedTimeSlot = null;
    private Calendar calendar;
    private List<ChargingStation> allStations = new ArrayList<>();
    private boolean showingNearby = false;
    private android.location.Location userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        initializeViews();
        setupClickListeners();
        setupDefaultDate();
        setupDefaultDate();
    }

    private void initializeViews() {
        etStation = findViewById(R.id.etStation);
        etDate = findViewById(R.id.etDate);
        
        btnSelectStation = findViewById(R.id.btnSelectStation);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnCancel = findViewById(R.id.btnCancel);
        btnBookNow = findViewById(R.id.btnBookNow);
        btnViewAll = findViewById(R.id.btnViewAll);
        btnNearby = findViewById(R.id.btnNearby);
        
        progressBar = findViewById(R.id.progressBar);
        layoutTimeSlots = findViewById(R.id.layoutTimeSlots);
        containerChargingPoints = findViewById(R.id.containerChargingPoints);

        prefs = new SharedPreferencesHelper(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);
        calendar = Calendar.getInstance();
        
        // Initialize map WebView
        initializeMapView();
        
        // Initialize button state
        btnBookNow.setEnabled(false);
        btnBookNow.setAlpha(0.5f);
        
        // Set initial button states
        updateMapFilterButtons();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeMapView() {
        Log.d(TAG, "Initializing map WebView");

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
            }

            // Initialize location service
            try {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                Log.d(TAG, "Location services initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing location services: " + e.getMessage(), e);
            }

            Log.d(TAG, "MapActivity initialization completed");
        } catch (Exception e) {
            Log.e(TAG, "Critical error in initializeMapView: " + e.getMessage(), e);
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
        public void onMarkerClick(String stationId) {
            Log.d(TAG, "Marker clicked for station: " + stationId);
            runOnUiThread(() -> {
                selectStationFromMap(stationId);
            });
        }
    }

    private void checkLocationPermission() {
        Log.d(TAG, "=== CHECKING LOCATION PERMISSION ===");

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "=== PERMISSION RESULT ===");

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

        // Store user location for nearby filtering
        if (userLocation == null) {
            userLocation = new android.location.Location("");
        }
        userLocation.setLatitude(latitude);
        userLocation.setLongitude(longitude);

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
                mapWebView.evaluateJavascript(jsCode, result -> {
                    Log.d(TAG, "JavaScript execution result: " + result);
                });

                Log.d(TAG, "=== LOCATION SENT TO MAP ===");

                // If user was waiting for location to filter nearby stations, do it now
                if (showingNearby && userLocation != null) {
                    Log.d(TAG, "User location received, filtering nearby stations");
                    showNearbyStations();
                }
            });
        } else {
            Log.w(TAG, "=== CANNOT SEND LOCATION - MAP NOT READY ===");

            // If location is available but map not ready, store it for later
            if (mapWebView != null) {
                Log.d(TAG, "Retrying in 2 seconds...");
                new android.os.Handler().postDelayed(() -> {
                    Log.d(TAG, "=== RETRY SENDING LOCATION ===");
                    sendLocationToMap(latitude, longitude);
                }, 2000);
            }
        }
    }    private void loadAllStations() {
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
                    allStations = response.body();
                    Log.d(TAG, "Stations received: " + (allStations != null ? allStations.size() : 0));

                    if (allStations != null && !allStations.isEmpty()) {
                        Log.d(TAG, "Loading " + allStations.size() + " real stations to map");
                        // Show all stations initially
                        loadStationsToMap(allStations);
                    } else {
                        Log.w(TAG, "No active stations found, map will show demo data");
                    }
                } else {
                    Log.e(TAG, "API call failed with code: " + response.code() + ", message: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<ChargingStation>> call, Throwable t) {
                Log.e(TAG, "Network error loading stations: " + t.getMessage());
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

    private void showAllStations() {
        Log.d(TAG, "Showing all stations");
        showingNearby = false;
        updateMapFilterButtons();

        if (allStations != null && !allStations.isEmpty()) {
            loadStationsToMap(allStations);
            Toast.makeText(this, "Showing all " + allStations.size() + " stations", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No stations available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNearbyStations() {
        Log.d(TAG, "Showing nearby stations");

        if (userLocation == null) {
            // Try to get current location first
            if (hasLocationPermission()) {
                getCurrentLocation();
                Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();
                // The location callback will trigger filtering once location is available
                return;
            } else {
                Toast.makeText(this, "Location permission required for nearby stations", Toast.LENGTH_LONG).show();
                checkLocationPermission();
                return;
            }
        }

        // Filter stations within 5km radius
        List<ChargingStation> nearbyStations = filterStationsByDistance(allStations, userLocation, 5.0);

        showingNearby = true;
        updateMapFilterButtons();

        if (!nearbyStations.isEmpty()) {
            loadStationsToMap(nearbyStations);
            Toast.makeText(this, "Found " + nearbyStations.size() + " stations within 5km", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No stations found within 5km", Toast.LENGTH_SHORT).show();
            // Show all stations as fallback
            loadStationsToMap(allStations);
        }
    }

    private List<ChargingStation> filterStationsByDistance(List<ChargingStation> stations, android.location.Location userLocation, double maxDistanceKm) {
        List<ChargingStation> nearbyStations = new ArrayList<>();

        for (ChargingStation station : stations) {
            if (station.getLocation() != null && station.getLocation().getLatitude() != 0 && station.getLocation().getLongitude() != 0) {
                try {
                    android.location.Location stationLocation = new android.location.Location("");
                    stationLocation.setLatitude(station.getLocation().getLatitude());
                    stationLocation.setLongitude(station.getLocation().getLongitude());

                    float distanceInMeters = userLocation.distanceTo(stationLocation);
                    double distanceInKm = distanceInMeters / 1000.0;

                    if (distanceInKm <= maxDistanceKm) {
                        nearbyStations.add(station);
                        Log.d(TAG, "Station " + station.getName() + " is " + String.format("%.2f", distanceInKm) + "km away");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating distance for station " + station.getName(), e);
                }
            }
        }

        return nearbyStations;
    }

    private void updateMapFilterButtons() {
        if (showingNearby) {
            btnViewAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
            btnNearby.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorSecondary)));
        } else {
            btnViewAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
            btnNearby.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
        }
    }

    private void selectStationFromMap(String stationId) {
        Log.d(TAG, "Selecting station from map: " + stationId);

        if (allStations == null || allStations.isEmpty()) {
            Toast.makeText(this, "Stations not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the station by ID
        ChargingStation selectedStationFromMap = null;
        for (ChargingStation station : allStations) {
            if (station.getId().equals(stationId)) {
                selectedStationFromMap = station;
                break;
            }
        }

        if (selectedStationFromMap == null) {
            Toast.makeText(this, "Station not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the selected station
        selectedStation = selectedStationFromMap;

        // Update the station input field
        etStation.setText(selectedStation.getName());

        // Clear existing time slots since station changed
        clearTimeSlots();

        // Load time slots if date is already selected
        if (selectedDate != null && !selectedDate.isEmpty()) {
            loadTimeSlots();
        }

        // Show confirmation
        Toast.makeText(this, "Selected: " + selectedStation.getName(), Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Station selected from map: " + selectedStation.getName() + " (ID: " + stationId + ")");
    }

    private void setupClickListeners() {
        btnSelectStation.setOnClickListener(v -> showStationSelection());
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnCancel.setOnClickListener(v -> finish());
        btnBookNow.setOnClickListener(v -> bookTimeSlot());
        btnViewAll.setOnClickListener(v -> showAllStations());
        btnNearby.setOnClickListener(v -> showNearbyStations());
    }

    private void setupDefaultDate() {
        // Set today as default date
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(formatDateForDisplay(selectedDate));
    }

    private void showStationSelection() {
        // Show loading indicator
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Loading charging stations...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Fetch active charging stations from API
        Call<List<ChargingStation>> call = apiService.getActiveStations();
        call.enqueue(new Callback<List<ChargingStation>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChargingStation>> call, @NonNull Response<List<ChargingStation>> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    List<ChargingStation> stations = response.body();
                    if (!stations.isEmpty()) {
                        showStationSelectionDialog(stations);
                    } else {
                        Toast.makeText(BookingActivity.this, "No active charging stations available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(BookingActivity.this, "Failed to load charging stations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChargingStation>> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to load stations", t);
                Toast.makeText(BookingActivity.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showStationSelectionDialog(List<ChargingStation> stations) {
        String[] stationNames = new String[stations.size()];
        for (int i = 0; i < stations.size(); i++) {
            ChargingStation station = stations.get(i);
            stationNames[i] = station.getName() + " (" + station.getStationType() + ")";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Charging Station")
                .setItems(stationNames, (dialog, which) -> {
                    selectedStation = stations.get(which);
                    etStation.setText(selectedStation.getName());
                    clearTimeSlots();
                    loadTimeSlots();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePicker() {
        Calendar minDate = Calendar.getInstance();
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DAY_OF_MONTH, 7); // Max 7 days ahead

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
                    etDate.setText(formatDateForDisplay(selectedDate));
                    clearTimeSlots();
                    if (selectedStation != null) {
                        loadTimeSlots();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePicker.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        datePicker.show();
    }

    private void loadTimeSlots() {
        if (selectedStation == null || selectedDate == null) {
            Toast.makeText(this, "Please select a station and date first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutTimeSlots.setVisibility(View.GONE);

        Call<TimeSlotAvailability> call = apiService.getStationAvailability(selectedStation.getId(), selectedDate);
        call.enqueue(new Callback<TimeSlotAvailability>() {
            @Override
            public void onResponse(@NonNull Call<TimeSlotAvailability> call, @NonNull Response<TimeSlotAvailability> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    availability = response.body();
                    displayTimeSlots();
                } else {
                    // Log detailed failure info to help debugging
                    String errMsg = "Failed to load time slots: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errMsg += " - " + errorBody;
                            Log.e(TAG, "TimeSlots error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading errorBody", e);
                    }
                    Log.e(TAG, errMsg);
                    Toast.makeText(BookingActivity.this, "Failed to load time slots (see logs)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TimeSlotAvailability> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                // Log detailed throwable for network failures (timeouts, DNS, TLS, etc.)
                Log.e(TAG, "Network error loading time slots", t);
                String userMessage = "Network error. Please check your connection.";
                if (t.getMessage() != null) {
                    Log.e(TAG, "Network exception message: " + t.getMessage());
                }
                Toast.makeText(BookingActivity.this, userMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTimeSlots() {
        if (availability == null) {
            Log.e(TAG, "displayTimeSlots: availability is null");
            return;
        }

        Log.d(TAG, "displayTimeSlots: availability has " + availability.getChargingPoints().size() + " charging points");
        
        layoutTimeSlots.setVisibility(View.VISIBLE);
        containerChargingPoints.removeAllViews();

        // Display charging points (1, 2, 3)
        for (ChargingPointSlots chargingPoint : availability.getChargingPoints()) {
            Log.d(TAG, "Processing charging point " + chargingPoint.getChargingPointNumber() 
                + " with " + chargingPoint.getTimeSlots().size() + " time slots");
                
            View chargingPointView = getLayoutInflater().inflate(R.layout.item_charging_point, containerChargingPoints, false);
            
            TextView tvChargingPointTitle = chargingPointView.findViewById(R.id.tvChargingPointTitle);
            RecyclerView rvTimeSlots = chargingPointView.findViewById(R.id.rvTimeSlots);
            
            if (tvChargingPointTitle == null) {
                Log.e(TAG, "tvChargingPointTitle is null - check item_charging_point.xml");
            }
            if (rvTimeSlots == null) {
                Log.e(TAG, "rvTimeSlots is null - check item_charging_point.xml");
            }
            
            tvChargingPointTitle.setText("Charging Point " + chargingPoint.getChargingPointNumber());
            
            // Set up time slots grid
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4); // 4 columns
            rvTimeSlots.setLayoutManager(gridLayoutManager);
            
            TimeSlotAdapter adapter = new TimeSlotAdapter(chargingPoint.getTimeSlots(), chargingPoint.getChargingPointNumber());
            rvTimeSlots.setAdapter(adapter);
            
            Log.d(TAG, "Added charging point view to container");
            containerChargingPoints.addView(chargingPointView);
        }
    }

    private void clearTimeSlots() {
        selectedChargingPoint = 0;
        selectedTimeSlot = null;
        containerChargingPoints.removeAllViews();
        layoutTimeSlots.setVisibility(View.GONE);
        updateBookButton();
    }

    private void updateBookButton() {
        boolean canBook = selectedStation != null && 
                         selectedDate != null && 
                         selectedChargingPoint > 0 && 
                         selectedTimeSlot != null;
        
        btnBookNow.setEnabled(canBook);
        btnBookNow.setAlpha(canBook ? 1.0f : 0.5f);
        
        if (canBook) {
            String bookingText = String.format("Book Point %d at %s", 
                selectedChargingPoint, selectedTimeSlot.getDisplayTime());
            btnBookNow.setText(bookingText);
        } else {
            btnBookNow.setText("Select Time Slot");
        }
    }

    private void bookTimeSlot() {
        if (selectedStation == null || selectedDate == null || selectedChargingPoint == 0 || selectedTimeSlot == null) {
            Toast.makeText(this, "Please select all booking details", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the logged in user's NIC
        String userNIC = prefs.getLoggedInUserNIC();
        if (userNIC == null || userNIC.trim().isEmpty()) {
            Toast.makeText(this, "User not logged in. Please login first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create booking request with all required fields
        BookingRequest request = new BookingRequest();
        request.setChargingStationId(selectedStation.getId()); // Changed to match server expectation
        request.setEvOwnerNIC(userNIC); // Added missing field
        request.setBookingDate(selectedDate);
        request.setTimeSlot(selectedTimeSlot.getHour());
        request.setChargingPointNumber(selectedChargingPoint);
        request.setDurationMinutes(60); // Fixed 1 hour duration

        Log.d(TAG, "Creating booking request: StationId=" + selectedStation.getId() 
            + ", UserNIC=" + userNIC + ", Date=" + selectedDate 
            + ", TimeSlot=" + selectedTimeSlot.getHour() + ", ChargingPoint=" + selectedChargingPoint);

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating booking...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Call<BookingResponse> call = apiService.createBooking(request);
        call.enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(@NonNull Call<BookingResponse> call, @NonNull Response<BookingResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    BookingResponse bookingResponse = response.body();
                    showBookingSuccess(bookingResponse);
                } else {
                    String errorMessage = "Failed to create booking";
                    if (response.code() == 409) {
                        errorMessage = "This time slot is no longer available";
                    }
                    Toast.makeText(BookingActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    // Refresh time slots to get updated availability
                    loadTimeSlots();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BookingResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Booking failed", t);
                Toast.makeText(BookingActivity.this, "Network error. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showBookingSuccess(BookingResponse booking) {
        // Launch BookingSuccessActivity with all the booking details
        Intent intent = new Intent(this, BookingSuccessActivity.class);
        intent.putExtra("bookingId", booking.getBookingId());
        intent.putExtra("stationName", selectedStation.getName());
        intent.putExtra("bookingDate", formatDateForDisplay(selectedDate));
        intent.putExtra("bookingTime", selectedTimeSlot.getDisplayTime());
        intent.putExtra("chargingPoint", selectedChargingPoint);
        intent.putExtra("qrCodeData", booking.getQrCodeData()); // Use proper QR code data field
        
        startActivity(intent);
        finish(); // Close booking activity
    }

    private String formatDateForDisplay(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    // TimeSlotAdapter inner class
    private class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {
        private List<TimeSlot> timeSlots;
        private int chargingPointNumber;

        public TimeSlotAdapter(List<TimeSlot> timeSlots, int chargingPointNumber) {
            this.timeSlots = timeSlots;
            this.chargingPointNumber = chargingPointNumber;
            Log.d(TAG, "TimeSlotAdapter created for charging point " + chargingPointNumber 
                + " with " + (timeSlots != null ? timeSlots.size() : "null") + " time slots");
        }

        @NonNull
        @Override
        public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder called");
            View view = getLayoutInflater().inflate(R.layout.item_time_slot, parent, false);
            return new TimeSlotViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
            Log.d(TAG, "onBindViewHolder called for position " + position);
            TimeSlot timeSlot = timeSlots.get(position);
            holder.bind(timeSlot, chargingPointNumber);
        }

        @Override
        public int getItemCount() {
            int count = timeSlots != null ? timeSlots.size() : 0;
            Log.d(TAG, "getItemCount returning " + count);
            return count;
        }

        class TimeSlotViewHolder extends RecyclerView.ViewHolder {
            private MaterialCardView cardTimeSlot;
            private TextView tvTimeSlot;

            public TimeSlotViewHolder(@NonNull View itemView) {
                super(itemView);
                cardTimeSlot = itemView.findViewById(R.id.cardTimeSlot);
                tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
            }

            public void bind(TimeSlot timeSlot, int chargingPointNumber) {
                Log.d(TAG, "Binding time slot: " + timeSlot.getDisplayTime() + " for charging point " + chargingPointNumber);
                
                tvTimeSlot.setText(timeSlot.getDisplayTime());

                // Set card appearance based on availability
                if (timeSlot.isAvailable()) {
                    cardTimeSlot.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                    cardTimeSlot.setClickable(true);
                    cardTimeSlot.setOnClickListener(v -> {
                        selectedChargingPoint = chargingPointNumber;
                        selectedTimeSlot = timeSlot;
                        updateBookButton();
                        notifyDataSetChanged(); // Refresh all items to update selection
                    });
                } else {
                    cardTimeSlot.setCardBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    cardTimeSlot.setClickable(false);
                    cardTimeSlot.setOnClickListener(null);
                }

                // Highlight selected time slot
                if (selectedChargingPoint == chargingPointNumber && 
                    selectedTimeSlot != null && 
                    selectedTimeSlot.getHour() == timeSlot.getHour()) {
                    cardTimeSlot.setCardBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapWebView != null) {
            mapWebView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapWebView != null) {
            mapWebView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // WebView cleanup handled automatically
    }
}