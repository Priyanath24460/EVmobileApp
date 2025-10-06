package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.models.ChargingStation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initializeViews();
        checkLocationPermission();
    }

    private void initializeViews() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    apiService = ApiClient.getClient(this).create(ApiService.class);
    }

    private void checkLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
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
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
                if (googleMap != null) {
                    enableMyLocation();
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                showDefaultLocation();
            }
        }
    }

    private void getCurrentLocation() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted");
            return;
        }

        try {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (googleMap != null) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12));
                        loadNearbyStations(location.getLatitude(), location.getLongitude());
                    }
                } else {
                    Log.w(TAG, "Last location is null");
                    showDefaultLocation();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get location: " + e.getMessage());
                showDefaultLocation();
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
            showDefaultLocation();
        }
    }

    private void enableMyLocation() {
        if (googleMap == null) return;

        try {
            if (hasFineLocationPermission()) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else if (hasLocationPermission()) {
                // We have coarse location only, don't enable precise my location
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException enabling my location: " + e.getMessage());
        }
    }

    private void showDefaultLocation() {
        // Default to Colombo, Sri Lanka
        LatLng defaultLatLng = new LatLng(6.9271, 79.8612);
        if (googleMap != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10));
            loadNearbyStations(defaultLatLng.latitude, defaultLatLng.longitude);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Enable basic map controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        // Enable my location if we have permission
        enableMyLocation();

        // If we don't have location permission yet, show default location
        if (!hasLocationPermission()) {
            showDefaultLocation();
        } else {
            getCurrentLocation();
        }
    }

    private void loadNearbyStations(double latitude, double longitude) {
        // For demo purposes, we'll use mock data
        // In real app, you would call your API
        displayMockStations();

        // Uncomment this when your API is ready:
        /*
        Call<List<ChargingStation>> call = apiService.getNearbyStations(latitude, longitude, 10);
        call.enqueue(new Callback<List<ChargingStation>>() {
            @Override
            public void onResponse(Call<List<ChargingStation>> call, Response<List<ChargingStation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayStationsOnMap(response.body());
                } else {
                    // Fallback to mock data
                    displayMockStations();
                }
            }

            @Override
            public void onFailure(Call<List<ChargingStation>> call, Throwable t) {
                Toast.makeText(MapActivity.this, "Failed to load stations", Toast.LENGTH_SHORT).show();
                // Fallback to mock data
                displayMockStations();
            }
        });
        */
    }

    private void displayMockStations() {
        if (googleMap == null) return;

        googleMap.clear();

        // Mock charging stations in Colombo area
        List<ChargingStation> mockStations = List.of(
                new ChargingStation("1", "Colombo City Center Station", "DC", 4, true,
                        new ChargingStation.Location("123 Galle Road", "Colombo", 6.9271, 79.8612)),
                new ChargingStation("2", "One Galle Face Station", "AC", 6, true,
                        new ChargingStation.Location("1 Galle Face", "Colombo", 6.9285, 79.8417)),
                new ChargingStation("3", "Bambalapitiya Station", "DC", 2, true,
                        new ChargingStation.Location("Bambalapitiya", "Colombo", 6.8958, 79.8554))
        );

        displayStationsOnMap(mockStations);
    }

    private void displayStationsOnMap(List<ChargingStation> stations) {
        if (googleMap == null) return;

        for (ChargingStation station : stations) {
            if (station.getLocation() != null && station.isActive()) {
                LatLng stationLatLng = new LatLng(
                        station.getLocation().getLatitude(),
                        station.getLocation().getLongitude()
                );

                // Use different colors for DC vs AC stations
                float markerColor = station.getStationType().equals("DC") ?
                        BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_GREEN;

                googleMap.addMarker(new MarkerOptions()
                        .position(stationLatLng)
                        .title(station.getName())
                        .snippet(station.getStationType() + " - " + station.getTotalSlots() + " slots")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (googleMap != null && hasLocationPermission()) {
            try {
                googleMap.setMyLocationEnabled(false);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException disabling my location: " + e.getMessage());
            }
        }
    }
}