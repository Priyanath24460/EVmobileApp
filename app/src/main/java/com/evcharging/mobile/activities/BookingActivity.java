package com.evcharging.mobile.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.evcharging.mobile.R;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.database.AppDatabase;
import com.evcharging.mobile.database.BookingDao;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.models.ChargingStation;
import com.evcharging.mobile.utils.DateUtils;
import com.evcharging.mobile.utils.SharedPreferencesHelper;
import com.google.android.material.textfield.TextInputEditText;
import android.view.View;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BookingActivity extends AppCompatActivity {

    private TextInputEditText etStation, etDateTime, etDuration;
    private TextView tvSummary;
    private Button btnSelectStation, btnSelectDateTime, btnBook, btnCancel;

    private SharedPreferencesHelper prefs;
    private ApiService apiService;
    private BookingDao bookingDao;

    private ChargingStation selectedStation;
    private Date selectedDateTime;
    private int selectedDuration = 60; // Default 60 minutes

    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        initializeViews();
        setupDatabase();
        setupClickListeners();

        // Check if editing existing booking
        String bookingId = getIntent().getStringExtra("booking_id");
        if (bookingId != null) {
            loadBooking(bookingId);
        }
    }

    private void initializeViews() {
        etStation = findViewById(R.id.etStation);
        etDateTime = findViewById(R.id.etDateTime);
        etDuration = findViewById(R.id.etDuration);
        tvSummary = findViewById(R.id.tvSummary);
        btnSelectStation = findViewById(R.id.btnSelectStation);
        btnSelectDateTime = findViewById(R.id.btnSelectDateTime);
        btnBook = findViewById(R.id.btnBook);
        btnCancel = findViewById(R.id.btnCancel);

    prefs = new SharedPreferencesHelper(this);
    apiService = ApiClient.getClient(this).create(ApiService.class);
        calendar = Calendar.getInstance();
    }

    private void setupDatabase() {
        AppDatabase database = AppDatabase.getInstance(this);
        bookingDao = database.bookingDao();
    }

    private void setupClickListeners() {
        btnSelectStation.setOnClickListener(v -> showStationSelection());
        btnSelectDateTime.setOnClickListener(v -> showDateTimePicker());
        btnBook.setOnClickListener(v -> createBooking());
        btnCancel.setOnClickListener(v -> finish());

        etDuration.setOnClickListener(v -> showDurationDialog());
    }

    private void showStationSelection() {
        // Show loading indicator
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Loading charging stations...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Fetch active charging stations from API
        Call<java.util.List<ChargingStation>> call = apiService.getActiveStations();
        call.enqueue(new Callback<java.util.List<ChargingStation>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.List<ChargingStation>> call, @NonNull Response<java.util.List<ChargingStation>> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<ChargingStation> stations = response.body();
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
            public void onFailure(@NonNull Call<java.util.List<ChargingStation>> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                // Show error and offer to try again
                new androidx.appcompat.app.AlertDialog.Builder(BookingActivity.this)
                        .setTitle("Network Error")
                        .setMessage("Failed to load charging stations. Please check your internet connection and try again.")
                        .setPositiveButton("Retry", (dialog, which) -> showStationSelection())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void showStationSelectionDialog(java.util.List<ChargingStation> stations) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_station_selection, null);
        
        androidx.recyclerview.widget.RecyclerView rvStations = dialogView.findViewById(R.id.rvStations);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        rvStations.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        com.evcharging.mobile.adapters.ChargingStationAdapter adapter = new com.evcharging.mobile.adapters.ChargingStationAdapter(this);
        adapter.setStations(stations);
        rvStations.setAdapter(adapter);
        
        android.app.AlertDialog dialog = builder.setView(dialogView).create();
        
        adapter.setOnStationClickListener(station -> {
            selectedStation = station;
            etStation.setText(selectedStation.getName());
            updateSummary();
            dialog.dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showDateTimePicker() {
        // Date Picker
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Time Picker
                    TimePickerDialog timePicker = new TimePickerDialog(
                            this,
                            (view1, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);

                                selectedDateTime = calendar.getTime();
                                etDateTime.setText(DateUtils.formatDateTime(selectedDateTime));
                                updateSummary();
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                    );
                    timePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        // Set maximum date to 7 days from now
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        datePicker.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_MONTH, -7); // Reset calendar

        datePicker.show();
    }

    private void showDurationDialog() {
        // Simple duration selection - in real app, use a proper dialog
        String[] durations = {"30 minutes", "60 minutes", "90 minutes", "120 minutes"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Duration")
                .setItems(durations, (dialog, which) -> {
                    selectedDuration = (which + 1) * 30; // 30, 60, 90, 120
                    etDuration.setText(durations[which]);
                    updateSummary();
                })
                .show();
    }

    private void updateSummary() {
        if (selectedStation != null && selectedDateTime != null) {
            String locationText = "N/A";
            if (selectedStation.getLocation() != null && selectedStation.getLocation().getFullAddress() != null) {
                locationText = selectedStation.getLocation().getFullAddress();
            }
            
            String summary = "Booking Summary:\n\n" +
                    "Station: " + selectedStation.getName() + "\n" +
                    "Type: " + selectedStation.getStationType() + "\n" +
                    "Date & Time: " + DateUtils.formatDateTime(selectedDateTime) + "\n" +
                    "Duration: " + selectedDuration + " minutes\n" +
                    "Location: " + locationText;

            tvSummary.setText(summary);
        }
    }

    private void createBooking() {
        if (selectedStation == null || selectedDateTime == null) {
            Toast.makeText(this, "Please select station and date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!DateUtils.isWithinNext7Days(selectedDateTime)) {
            Toast.makeText(this, "Reservation must be within 7 days", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        showBookingConfirmation();
    }

    private void showBookingConfirmation() {
        String confirmationMessage = buildConfirmationMessage();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage(confirmationMessage)
                .setPositiveButton("Confirm", (dialog, which) -> processBooking())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String buildConfirmationMessage() {
        StringBuilder message = new StringBuilder();
        message.append("Station: ").append(selectedStation.getName()).append("\n\n");
        
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
        message.append("Date & Time: ").append(dateFormat.format(selectedDateTime)).append("\n\n");
        
        message.append("Duration: ").append(selectedDuration).append(" minutes\n\n");
        message.append("Status: Pending (awaiting approval)\n\n");
        message.append("Note: You will receive a QR code once your booking is approved.");
        
        return message.toString();
    }

    private void processBooking() {
        Booking booking = new Booking();
        // Generate proper ID first
        booking.setId("LOCAL_" + System.currentTimeMillis());
        booking.setEvOwnerNIC(prefs.getLoggedInUserNIC());
        booking.setChargingStationId(selectedStation.getId());
        booking.setSlotId("slot-" + selectedStation.getId() + "-" + selectedDateTime.getTime());
        booking.setReservationDateTime(selectedDateTime);
        booking.setDurationMinutes(selectedDuration);
        booking.setStatus("Pending");
        booking.setBookingDate(new Date());

        // Store station details locally with null checks
        booking.setStationName(selectedStation.getName());
        String stationAddress = "N/A";
        if (selectedStation.getLocation() != null && selectedStation.getLocation().getAddress() != null) {
            stationAddress = selectedStation.getLocation().getAddress();
        }
        booking.setStationAddress(stationAddress);

        booking.setQrCodeData("");
        saveBooking(booking);
    }

    private void saveBooking(Booking booking) {
        new Thread(() -> {
            try {
                booking.setBookingReference("EVB" + System.currentTimeMillis());
                bookingDao.upsert(booking);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Booking created successfully!", Toast.LENGTH_SHORT).show();
                    syncBookingWithApi(booking);

                    Intent intent = new Intent(BookingActivity.this, BookingConfirmationActivity.class);
                    intent.putExtra("booking_id", booking.getId());
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to save booking: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void syncBookingWithApi(Booking booking) {
        // Debug: log outgoing booking JSON
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String json = gson.toJson(booking);
            android.util.Log.d("BOOKING", "Outgoing payload: " + json);
        } catch (Exception ignored) {}

    // Create a server payload: exclude local-only fields (id, bookingReference)
    Booking payload = new Booking();
    payload.setEvOwnerNIC(booking.getEvOwnerNIC());
    payload.setChargingStationId(booking.getChargingStationId());
    payload.setSlotId(booking.getSlotId());
    payload.setReservationDateTime(booking.getReservationDateTime());
    payload.setDurationMinutes(booking.getDurationMinutes());
    payload.setStatus(booking.getStatus());
    payload.setQrCodeData(booking.getQrCodeData());

        // Debug: log full endpoint URL
        try {
            String fullUrl = com.evcharging.mobile.BuildConfig.API_BASE_URL + "api/Bookings";
            android.util.Log.d("BOOKING", "POST to: " + fullUrl);
        } catch (Exception ignored) {}

        Call<Booking> call = apiService.createBooking(payload);
        call.enqueue(new Callback<Booking>() {
            @Override
            public void onResponse(@NonNull Call<Booking> call, @NonNull Response<Booking> response) {
                if (response.isSuccessful()) {
                    Booking serverBooking = response.body();
                    if (serverBooking != null) {
                        // Update local booking with server data
                        new Thread(() -> {
                            try {
                                booking.setId(serverBooking.getId());
                                if (serverBooking.getBookingReference() != null) {
                                    booking.setBookingReference(serverBooking.getBookingReference());
                                }
                                if (serverBooking.getStatus() != null) {
                                    booking.setStatus(serverBooking.getStatus());
                                }
                                if (serverBooking.getQrCodeData() != null) {
                                    booking.setQrCodeData(serverBooking.getQrCodeData());
                                }
                                bookingDao.update(booking);
                            } catch (Exception e) {
                                android.util.Log.e("BOOKING", "Error updating booking: " + e.getMessage());
                            }
                        }).start();
                    }
                } else {
                    // Log server error and show a message
                    String err = "Booking sync failed: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            err += " - " + errorBody;
                            android.util.Log.e("BOOKING", "Server error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("BOOKING", "Error reading error body: " + e.getMessage());
                    }
                    android.util.Log.e("BOOKING", err);
                    runOnUiThread(() -> Toast.makeText(BookingActivity.this, "Booking saved locally but failed to sync", Toast.LENGTH_LONG).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Booking> call, @NonNull Throwable t) {
                // Booking remains in local storage for offline use
                android.util.Log.e("BOOKING", "Sync failure: " + t.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(BookingActivity.this, "Booking saved offline", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void loadBooking(String bookingId) {
        new Thread(() -> {
            Booking booking = bookingDao.getBookingById(bookingId);
            if (booking != null) {
                runOnUiThread(() -> {
                    // Populate fields with booking data
                    // This would require fetching station details, etc.
                    etDateTime.setText(DateUtils.formatDateTime(booking.getReservationDateTime()));
                    etDuration.setText(booking.getDurationMinutes() + " minutes");
                    selectedDuration = booking.getDurationMinutes();
                    selectedDateTime = booking.getReservationDateTime();

                    btnBook.setText("Update Booking");
                    // Update other logic for editing
                });
            }
        }).start();
    }
}