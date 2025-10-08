package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.widget.RadioGroup;
import com.evcharging.mobile.R;
import com.evcharging.mobile.adapters.BookingAdapter;
import com.evcharging.mobile.database.AppDatabase;
import com.evcharging.mobile.database.BookingDao;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.SharedPreferencesHelper;
import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private RadioGroup rgFilter;
    private BookingAdapter bookingAdapter;
    private BookingDao bookingDao;
    private SharedPreferencesHelper prefs;
    private String currentUserNIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        initializeViews();
        setupDatabase();
        setupClickListeners();
        // Sync with server first, then load local DB
        syncBookingsFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure we refresh after returning
        syncBookingsFromServer();
    }

    private void syncBookingsFromServer() {
        if (currentUserNIC == null || currentUserNIC.isEmpty()) {
            loadAllBookings(); // Fallback to local if no user
            return;
        }

        com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
        
        // Clear local database first to ensure fresh data
        new Thread(() -> {
            bookingDao.deleteAllBookingsForUser(currentUserNIC);
        }).start();
        
        // Get upcoming bookings - SERVER FIRST
        retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> upcomingCall = api.getUpcomingBookings(currentUserNIC);
        upcomingCall.enqueue(new retrofit2.Callback<java.util.List<com.evcharging.mobile.models.Booking>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, retrofit2.Response<java.util.List<com.evcharging.mobile.models.Booking>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.evcharging.mobile.models.Booking> serverBookings = response.body();
                    new Thread(() -> {
                        // Insert fresh server data
                        for (com.evcharging.mobile.models.Booking b : serverBookings) {
                            try {
                                bookingDao.insert(b);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                        runOnUiThread(() -> loadAllBookings());
                    }).start();
                } else {
                    // Server error - show empty list (no stale data)
                    runOnUiThread(() -> bookingAdapter.setBookings(new java.util.ArrayList<>()));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, Throwable t) {
                // Network error - fallback to local data only as last resort
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(BookingHistoryActivity.this, "No internet connection. Showing offline data.", android.widget.Toast.LENGTH_SHORT).show();
                    loadAllBookings();
                });
            }
        });

        // Get booking history - SERVER FIRST
        retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> historyCall = api.getBookingHistory(currentUserNIC);
        historyCall.enqueue(new retrofit2.Callback<java.util.List<com.evcharging.mobile.models.Booking>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, retrofit2.Response<java.util.List<com.evcharging.mobile.models.Booking>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.evcharging.mobile.models.Booking> historyBookings = response.body();
                    new Thread(() -> {
                        // Insert fresh server data
                        for (com.evcharging.mobile.models.Booking b : historyBookings) {
                            try {
                                bookingDao.insert(b);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, Throwable t) {
                // Silently fail for history
            }
        });
    }

    private void initializeViews() {
        rvBookings = findViewById(R.id.rvBookings);
        rgFilter = findViewById(R.id.rgFilter);

        prefs = new SharedPreferencesHelper(this);
        currentUserNIC = prefs.getLoggedInUserNIC();

        // Setup RecyclerView
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(this, false); // false for hiding actions in history
        rvBookings.setAdapter(bookingAdapter);
    }

    private void setupDatabase() {
        AppDatabase database = AppDatabase.getInstance(this);
        bookingDao = database.bookingDao();
    }

    private void setupClickListeners() {
        rgFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAll) {
                loadAllBookings();
            } else if (checkedId == R.id.rbUpcoming) {
                loadUpcomingBookings();
            } else if (checkedId == R.id.rbPast) {
                loadPastBookings();
            }
        });
        
        // Set up booking adapter listeners
        bookingAdapter.setOnCancelClickListener(this::cancelBooking);
        bookingAdapter.setOnBookingClickListener(this::openBookingDetails);
        bookingAdapter.setOnQRClickListener(this::showQRCode);
    }
    
    private void cancelBooking(com.evcharging.mobile.models.Booking booking) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this booking?\n\nStation: " + booking.getStationName() + "\nDate: " + com.evcharging.mobile.utils.DateUtils.formatDateTime(booking.getReservationDateTime()))
                .setPositiveButton("Yes, Cancel", (dialog, which) -> performBookingCancellation(booking))
                .setNegativeButton("No", null)
                .show();
    }
    
    private void performBookingCancellation(com.evcharging.mobile.models.Booking booking) {
        // Show progress
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Cancelling booking...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Update local database first
        new Thread(() -> {
            booking.setStatus("Cancelled");
            bookingDao.update(booking);
            
            runOnUiThread(() -> {
                // Sync with server
                com.evcharging.mobile.api.ApiService apiService = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
                retrofit2.Call<Void> call = apiService.cancelBooking(booking.getId());
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            android.widget.Toast.makeText(BookingHistoryActivity.this, "Booking cancelled successfully", android.widget.Toast.LENGTH_SHORT).show();
                            // Refresh the list
                            syncBookingsFromServer();
                        } else {
                            android.widget.Toast.makeText(BookingHistoryActivity.this, "Booking cancelled locally but failed to sync with server", android.widget.Toast.LENGTH_LONG).show();
                        }
                    }
                    
                    @Override
                    public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                        progressDialog.dismiss();
                        android.widget.Toast.makeText(BookingHistoryActivity.this, "Booking cancelled locally. Will sync when network is available.", android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            });
        }).start();
    }
    
    private void openBookingDetails(com.evcharging.mobile.models.Booking booking) {
        android.content.Intent intent = new android.content.Intent(this, com.evcharging.mobile.activities.BookingConfirmationActivity.class);
        intent.putExtra("booking_id", booking.getId());
        startActivity(intent);
    }
    
    private void showQRCode(com.evcharging.mobile.models.Booking booking) {
        if ("Approved".equals(booking.getStatus())) {
            android.content.Intent intent = new android.content.Intent(this, com.evcharging.mobile.activities.QRDisplayActivity.class);
            intent.putExtra("booking", booking);
            startActivity(intent);
        } else {
            android.widget.Toast.makeText(this, "QR Code available only for approved bookings", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAllBookings() {
        new Thread(() -> {
            List<Booking> allBookings = bookingDao.getBookingsByNIC(currentUserNIC);
            runOnUiThread(() -> bookingAdapter.setBookings(allBookings));
        }).start();
    }

    private void loadUpcomingBookings() {
        new Thread(() -> {
            List<Booking> upcomingBookings = bookingDao.getUpcomingBookings(currentUserNIC);
            runOnUiThread(() -> bookingAdapter.setBookings(upcomingBookings));
        }).start();
    }

    private void loadPastBookings() {
        new Thread(() -> {
            List<Booking> pastBookings = bookingDao.getPastBookings(currentUserNIC);
            runOnUiThread(() -> bookingAdapter.setBookings(pastBookings));
        }).start();
    }
}