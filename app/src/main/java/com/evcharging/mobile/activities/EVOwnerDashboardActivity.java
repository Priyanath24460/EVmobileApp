package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.evcharging.mobile.R;
import com.evcharging.mobile.adapters.BookingAdapter;
import com.evcharging.mobile.database.AppDatabase;
import com.evcharging.mobile.database.BookingDao;
import com.evcharging.mobile.database.UserDao;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.models.User;
import com.evcharging.mobile.utils.SharedPreferencesHelper;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class EVOwnerDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvPendingCount, tvApprovedCount;
    private Button btnNewBooking, btnViewHistory, btnViewMap, btnProfile;
    private RecyclerView rvUpcomingBookings;
    private BookingAdapter bookingAdapter;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabNewBooking;
    private androidx.appcompat.widget.Toolbar toolbar;
    private androidx.cardview.widget.CardView cvProfileAvatar;
    private TextView tvToolbarAvatar;

    private UserDao userDao;
    private BookingDao bookingDao;
    private SharedPreferencesHelper prefs;
    private String currentUserNIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evowner_dashboard);

        initializeViews();
        setupDatabase();
        loadUserData();
        setupClickListeners();
        // Clean up any duplicate bookings first
        new Thread(() -> {
            bookingDao.cleanupAllDuplicates(currentUserNIC);
            runOnUiThread(() -> {
                loadUpcomingBookings();
                updateBookingCounts();
            });
        }).start();
        // Attempt to sync bookings from server on first open
        syncBookingsFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When returning to the dashboard, refresh local DB from server then reload UI
        syncBookingsFromServer();
    }

    private void syncBookingsFromServer() {
        if (currentUserNIC == null || currentUserNIC.isEmpty()) {
            android.util.Log.w("Dashboard", "Cannot sync bookings: currentUserNIC is null or empty");
            return;
        }

        android.util.Log.d("Dashboard", "Starting server sync for user: " + currentUserNIC);

        com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);

        // Sync upcoming bookings
        retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> upcomingCall = api.getUpcomingBookings(currentUserNIC);
        upcomingCall.enqueue(new retrofit2.Callback<java.util.List<com.evcharging.mobile.models.Booking>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, retrofit2.Response<java.util.List<com.evcharging.mobile.models.Booking>> response) {
                android.util.Log.d("Dashboard", "Server response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.evcharging.mobile.models.Booking> serverBookings = response.body();
                    android.util.Log.d("Dashboard", "Server returned " + serverBookings.size() + " upcoming bookings");

                    // Log each booking details
                    for (com.evcharging.mobile.models.Booking b : serverBookings) {
                        android.util.Log.d("Dashboard", "Server booking: ID=" + b.getId() +
                            ", Status=" + b.getStatus() +
                            ", DateTime=" + b.getReservationDateTime() +
                            ", Station=" + b.getStationName());
                    }

                    new Thread(() -> {
                        for (com.evcharging.mobile.models.Booking b : serverBookings) {
                            try {
                                android.util.Log.d("Dashboard", "Saving booking: " + b.getId() + " Status: " + b.getStatus());
                                bookingDao.upsert(b);
                                // Clean up any duplicates
                                if (b.getReservationDateTime() != null) {
                                    bookingDao.deleteDuplicateBookings(currentUserNIC, b.getId(),
                                        b.getChargingStationId(), b.getReservationDateTime().getTime());
                                }
                            } catch (Exception e) {
                                android.util.Log.e("Dashboard", "Error saving booking: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        runOnUiThread(() -> {
                            loadUpcomingBookings();
                            updateBookingCounts();
                        });
                    }).start();
                } else {
                    android.util.Log.w("Dashboard", "Server response failed or empty. Code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            android.util.Log.w("Dashboard", "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            android.util.Log.w("Dashboard", "Could not read error body");
                        }
                    }
                    // Still load local bookings even if server fails
                    runOnUiThread(() -> {
                        loadUpcomingBookings();
                        updateBookingCounts();
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, Throwable t) {
                android.util.Log.e("Dashboard", "Failed to sync upcoming bookings: " + t.getMessage());
                // Still load local bookings even if network fails
                runOnUiThread(() -> {
                    loadUpcomingBookings();
                    updateBookingCounts();
                });
            }
        });

        // Also sync booking history for complete data
        retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> historyCall = api.getBookingHistory(currentUserNIC);
        historyCall.enqueue(new retrofit2.Callback<java.util.List<com.evcharging.mobile.models.Booking>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, retrofit2.Response<java.util.List<com.evcharging.mobile.models.Booking>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.evcharging.mobile.models.Booking> historyBookings = response.body();
                    android.util.Log.d("Dashboard", "Server returned " + historyBookings.size() + " history bookings");
                    new Thread(() -> {
                        for (com.evcharging.mobile.models.Booking b : historyBookings) {
                            try {
                                bookingDao.upsert(b);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, Throwable t) {
                android.util.Log.w("Dashboard", "Failed to sync booking history: " + t.getMessage());
                // Silently fail for history sync
            }
        });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cvProfileAvatar = findViewById(R.id.cvProfileAvatar);
        tvToolbarAvatar = findViewById(R.id.tvToolbarAvatar);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        btnNewBooking = findViewById(R.id.btnNewBooking);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnProfile = findViewById(R.id.btnProfile);
        fabNewBooking = findViewById(R.id.fabNewBooking);
        rvUpcomingBookings = findViewById(R.id.rvUpcomingBookings);

        prefs = new SharedPreferencesHelper(this);
        currentUserNIC = prefs.getLoggedInUserNIC();

        // Setup RecyclerView
        rvUpcomingBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(this, true); // true for showing actions
        rvUpcomingBookings.setAdapter(bookingAdapter);
    }

    private void setupDatabase() {
        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
        bookingDao = database.bookingDao();
    }

    private void loadUserData() {
        new Thread(() -> {
            User user = userDao.getUserByNIC(currentUserNIC);
            runOnUiThread(() -> {
                if (user != null) {
                    String greeting = getTimeBasedGreeting() + ", " + user.getFirstName() + "!";
                    tvWelcome.setText(greeting);
                    
                    // Set avatar letter
                    if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                        String firstLetter = user.getFirstName().substring(0, 1).toUpperCase();
                        tvToolbarAvatar.setText(firstLetter);
                    }
                }
            });
        }).start();
    }

    private String getTimeBasedGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return "Good morning";
        } else if (hour >= 12 && hour < 17) {
            return "Good afternoon";
        } else if (hour >= 17 && hour < 22) {
            return "Good evening";
        } else {
            return "Good night";
        }
    }

    private void loadUpcomingBookings() {
        new Thread(() -> {
            // Debug: Check total bookings first
            List<Booking> allBookings = bookingDao.getBookingsByNIC(currentUserNIC);
            android.util.Log.d("Dashboard", "Total bookings for user: " + allBookings.size());

            // Debug: Log all bookings with details
            for (Booking booking : allBookings) {
                android.util.Log.d("Dashboard", "Booking: ID=" + booking.getId() +
                    ", Status=" + booking.getStatus() +
                    ", DateTime=" + booking.getReservationDateTime() +
                    ", Timestamp=" + (booking.getReservationDateTime() != null ? booking.getReservationDateTime().getTime() : "null") +
                    ", CurrentTime=" + System.currentTimeMillis() +
                    ", IsUpcoming=" + booking.isUpcoming());
            }

            List<Booking> upcomingBookings = bookingDao.getUpcomingBookings(currentUserNIC);
            android.util.Log.d("Dashboard", "Upcoming bookings found: " + upcomingBookings.size());

            // Debug: Log upcoming bookings
            for (Booking booking : upcomingBookings) {
                android.util.Log.d("Dashboard", "Upcoming: " + booking.getId() + " - " + booking.getStatus() + " - " + booking.getReservationDateTime());
            }

            runOnUiThread(() -> {
                bookingAdapter.setBookings(upcomingBookings);
                android.util.Log.d("Dashboard", "Adapter updated with " + upcomingBookings.size() + " bookings");
            });
        }).start();
    }

    private void updateBookingCounts() {
        // First update from local database
        new Thread(() -> {
            int pendingCount = bookingDao.getPendingBookingCount(currentUserNIC);
            int approvedCount = bookingDao.getApprovedBookingCount(currentUserNIC);

            runOnUiThread(() -> {
                tvPendingCount.setText(String.valueOf(pendingCount));
                tvApprovedCount.setText(String.valueOf(approvedCount));
            });
        }).start();

        // Then try to get updated counts from server
        updateBookingCountsFromServer();
    }

    private void updateBookingCountsFromServer() {
        if (currentUserNIC == null || currentUserNIC.isEmpty()) return;

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        
        // Get pending count
        Call<Integer> pendingCall = api.getPendingBookingsCount(currentUserNIC);
        pendingCall.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvPendingCount.setText(String.valueOf(response.body()));
                }
                // If server fails, keep local count (already displayed)
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                // Keep local count (already displayed)
            }
        });

        // Get approved count
        Call<Integer> approvedCall = api.getApprovedBookingsCount(currentUserNIC);
        approvedCall.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvApprovedCount.setText(String.valueOf(response.body()));
                }
                // If server fails, keep local count (already displayed)
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                // Keep local count (already displayed)
            }
        });
    }

    private void setupClickListeners() {
        btnNewBooking.setOnClickListener(v -> {
            startActivity(new Intent(this, BookingActivity.class));
        });

        btnViewHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, BookingHistoryActivity.class));
        });

        btnViewMap.setOnClickListener(v -> {
            startActivity(new Intent(this, MapActivity.class));
        });

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        fabNewBooking.setOnClickListener(v -> {
            startActivity(new Intent(this, BookingActivity.class));
        });

        // Profile avatar click listener
        cvProfileAvatar.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        // Booking adapter click listeners
        bookingAdapter.setOnBookingClickListener(booking -> {
            // Show booking details
            Intent intent = new Intent(this, BookingActivity.class);
            intent.putExtra("booking_id", booking.getId());
            startActivity(intent);
        });

        bookingAdapter.setOnCancelClickListener(booking -> {
            if (booking.canBeCancelled()) {
                cancelBooking(booking);
            } else {
                Toast.makeText(this, "Cannot cancel booking. Must be at least 12 hours before reservation.", Toast.LENGTH_LONG).show();
            }
        });

        bookingAdapter.setOnQRClickListener(booking -> {
            if ("Approved".equals(booking.getStatus())) {
                showQRCode(booking);
            } else {
                Toast.makeText(this, "QR Code available only for approved bookings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            prefs.clearUserData();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_settings) {
            // TODO: Open settings activity
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cancelBooking(Booking booking) {
        // Delete from local database first
        new Thread(() -> {
            bookingDao.deleteById(booking.getId());
            
            runOnUiThread(() -> {
                // Delete from server
                ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
                retrofit2.Call<Void> call = apiService.cancelBooking(booking.getId());
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(EVOwnerDashboardActivity.this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EVOwnerDashboardActivity.this, "Booking cancelled locally", Toast.LENGTH_SHORT).show();
                        }
                        loadUpcomingBookings();
                        updateBookingCounts();
                    }
                    
                    @Override
                    public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                        Toast.makeText(EVOwnerDashboardActivity.this, "Booking cancelled locally. Will sync when online.", Toast.LENGTH_SHORT).show();
                        loadUpcomingBookings();
                        updateBookingCounts();
                    }
                });
            });
        }).start();
    }

    private void showQRCode(Booking booking) {
        Intent intent = new Intent(this, QRDisplayActivity.class);
        intent.putExtra("booking", booking);
        startActivity(intent);
    }
}