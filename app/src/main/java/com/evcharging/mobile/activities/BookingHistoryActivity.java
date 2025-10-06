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
        if (currentUserNIC == null || currentUserNIC.isEmpty()) return;

        com.evcharging.mobile.api.ApiService api = com.evcharging.mobile.api.ApiClient.getClient(this).create(com.evcharging.mobile.api.ApiService.class);
        
        // Get upcoming bookings
        retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> upcomingCall = api.getUpcomingBookings(currentUserNIC);
        upcomingCall.enqueue(new retrofit2.Callback<java.util.List<com.evcharging.mobile.models.Booking>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, retrofit2.Response<java.util.List<com.evcharging.mobile.models.Booking>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.evcharging.mobile.models.Booking> serverBookings = response.body();
                    new Thread(() -> {
                        for (com.evcharging.mobile.models.Booking b : serverBookings) {
                            try {
                                bookingDao.upsert(b);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                        runOnUiThread(() -> loadAllBookings());
                    }).start();
                } else {
                    runOnUiThread(() -> loadAllBookings());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, Throwable t) {
                runOnUiThread(() -> loadAllBookings());
            }
        });

        // Also get booking history
        retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> historyCall = api.getBookingHistory(currentUserNIC);
        historyCall.enqueue(new retrofit2.Callback<java.util.List<com.evcharging.mobile.models.Booking>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.evcharging.mobile.models.Booking>> call, retrofit2.Response<java.util.List<com.evcharging.mobile.models.Booking>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<com.evcharging.mobile.models.Booking> historyBookings = response.body();
                    new Thread(() -> {
                        for (com.evcharging.mobile.models.Booking b : historyBookings) {
                            try {
                                bookingDao.upsert(b);
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