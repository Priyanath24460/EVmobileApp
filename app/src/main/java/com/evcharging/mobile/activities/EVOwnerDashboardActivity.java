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
    private Button btnNewBooking, btnViewHistory, btnViewMap, btnLogout;
    private RecyclerView rvUpcomingBookings;
    private BookingAdapter bookingAdapter;

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
        loadUpcomingBookings();
        // Attempt to sync bookings from server on first open
        syncBookingsFromServer();
        updateBookingCounts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When returning to the dashboard, refresh local DB from server then reload UI
        syncBookingsFromServer();
    }

    private void syncBookingsFromServer() {
        if (currentUserNIC == null || currentUserNIC.isEmpty()) return;

        com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
        retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call = api.getUpcomingBookings(currentUserNIC);
        call.enqueue(new retrofit2.Callback<java.util.List<com.evcharging.mobile.models.Booking>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, retrofit2.Response<java.util.List<com.evcharging.mobile.models.Booking>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.evcharging.mobile.models.Booking> serverBookings = response.body();
                    // Upsert into local DB
                    new Thread(() -> {
                        for (com.evcharging.mobile.models.Booking b : serverBookings) {
                            // Ensure we don't accidentally overwrite local-only fields like bookingReference if server doesn't provide them
                            try {
                                bookingDao.upsert(b);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // After DB update, refresh UI counts and list
                        runOnUiThread(() -> {
                            loadUpcomingBookings();
                            updateBookingCounts();
                        });
                    }).start();
                } else {
                    // Non-2xx - ignore gracefully but refresh local UI
                    runOnUiThread(() -> {
                        loadUpcomingBookings();
                        updateBookingCounts();
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, Throwable t) {
                // Network failure - keep local DB and refresh
                runOnUiThread(() -> {
                    loadUpcomingBookings();
                    updateBookingCounts();
                });
            }
        });
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        btnNewBooking = findViewById(R.id.btnNewBooking);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnLogout = findViewById(R.id.btnLogout);
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
                    tvWelcome.setText("Welcome, " + user.getFirstName() + "!");
                }
            });
        }).start();
    }

    private void loadUpcomingBookings() {
        new Thread(() -> {
            List<Booking> upcomingBookings = bookingDao.getUpcomingBookings(currentUserNIC);
            runOnUiThread(() -> {
                bookingAdapter.setBookings(upcomingBookings);
                if (upcomingBookings.isEmpty()) {
                    Toast.makeText(this, "No upcoming bookings", Toast.LENGTH_SHORT).show();
                }
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
        
        // Get pending count - SERVER FIRST
        Call<Integer> pendingCall = api.getPendingBookingsCount(currentUserNIC);
        pendingCall.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvPendingCount.setText(String.valueOf(response.body()));
                } else {
                    // Server error - show 0 (fresh state)
                    tvPendingCount.setText("0");
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                // Network error - fallback to local count
                new Thread(() -> {
                    int localCount = bookingDao.getPendingBookingCount(currentUserNIC);
                    runOnUiThread(() -> tvPendingCount.setText(String.valueOf(localCount)));
                }).start();
            }
        });

        // Get approved count - SERVER FIRST
        Call<Integer> approvedCall = api.getApprovedBookingsCount(currentUserNIC);
        approvedCall.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvApprovedCount.setText(String.valueOf(response.body()));
                } else {
                    // Server error - show 0 (fresh state)
                    tvApprovedCount.setText("0");
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                // Network error - fallback to local count
                new Thread(() -> {
                    int localCount = bookingDao.getApprovedBookingCount(currentUserNIC);
                    runOnUiThread(() -> tvApprovedCount.setText(String.valueOf(localCount)));
                }).start();
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

        btnLogout.setOnClickListener(v -> {
            prefs.clearUserData();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
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

    private void cancelBooking(Booking booking) {
        new Thread(() -> {
            booking.setStatus("Cancelled");
            bookingDao.update(booking);

            runOnUiThread(() -> {
                Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                loadUpcomingBookings();
                updateBookingCounts();
            });
        }).start();
    }

    private void showQRCode(Booking booking) {
        // In a real app, you would generate/show QR code
        Toast.makeText(this, "QR Code for booking: " + booking.getBookingReference(), Toast.LENGTH_LONG).show();

        // For demo, you can start QR display activity
        // Intent intent = new Intent(this, QRDisplayActivity.class);
        // intent.putExtra("booking_data", booking.getQrCodeData());
        // startActivity(intent);
    }
}