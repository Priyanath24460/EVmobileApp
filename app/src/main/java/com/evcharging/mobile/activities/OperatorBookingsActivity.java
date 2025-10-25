package com.evcharging.mobile.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.evcharging.mobile.R;
import com.evcharging.mobile.adapters.OperatorBookingAdapter;
import com.evcharging.mobile.api.ApiClient;
import com.evcharging.mobile.api.ApiService;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorBookingsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvTotalCount, tvActiveCount, tvCompletedCount;
    private MaterialButtonToggleGroup toggleFilter;
    private MaterialButton btnFilterAll, btnFilterActive, btnFilterCompleted;
    private RecyclerView rvBookings;
    private LinearLayout layoutEmptyState;
    private FloatingActionButton fabRefresh;

    private OperatorBookingAdapter bookingAdapter;
    private List<Booking> allBookings = new ArrayList<>();
    private List<Booking> filteredBookings = new ArrayList<>();
    
    private SharedPreferencesHelper prefs;
    private ApiService apiService;
    private String operatorUsername;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_bookings);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        loadBookings();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        toggleFilter = findViewById(R.id.toggleFilter);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterActive = findViewById(R.id.btnFilterActive);
        btnFilterCompleted = findViewById(R.id.btnFilterCompleted);
        rvBookings = findViewById(R.id.rvBookings);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabRefresh = findViewById(R.id.fabRefresh);

        prefs = new SharedPreferencesHelper(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);
        operatorUsername = prefs.getLoggedInUserNIC();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        bookingAdapter = new OperatorBookingAdapter(filteredBookings, this);
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        rvBookings.setAdapter(bookingAdapter);
    }

    private void setupClickListeners() {
        fabRefresh.setOnClickListener(v -> loadBookings());

        toggleFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFilterAll) {
                    currentFilter = "ALL";
                } else if (checkedId == R.id.btnFilterActive) {
                    currentFilter = "ACTIVE";
                } else if (checkedId == R.id.btnFilterCompleted) {
                    currentFilter = "COMPLETED";
                }
                applyFilter();
            }
        });
    }

    private void loadBookings() {
        if (operatorUsername == null) {
            Toast.makeText(this, "Error: Operator username not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        fabRefresh.setEnabled(false);

        Call<List<Booking>> call = apiService.getOperatorBookings(operatorUsername);
        call.enqueue(new Callback<List<Booking>>() {
            @Override
            public void onResponse(Call<List<Booking>> call, Response<List<Booking>> response) {
                fabRefresh.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    allBookings.clear();
                    allBookings.addAll(response.body());
                    updateStatistics();
                    applyFilter();
                    Toast.makeText(OperatorBookingsActivity.this, 
                        "Loaded " + allBookings.size() + " bookings", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OperatorBookingsActivity.this, 
                        "Failed to load bookings", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Booking>> call, Throwable t) {
                fabRefresh.setEnabled(true);
                Toast.makeText(OperatorBookingsActivity.this, 
                    "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateStatistics() {
        int totalCount = allBookings.size();
        int activeCount = 0;
        int completedCount = 0;

        for (Booking booking : allBookings) {
            String status = booking.getStatus();
            // Active count now shows only "Started" bookings (charging sessions in progress)
            if ("Started".equalsIgnoreCase(status)) {
                activeCount++;
            } else if ("Completed".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                completedCount++;
            }
        }

        tvTotalCount.setText(String.valueOf(totalCount));
        tvActiveCount.setText(String.valueOf(activeCount));
        tvCompletedCount.setText(String.valueOf(completedCount));
    }

    private void applyFilter() {
        filteredBookings.clear();

        switch (currentFilter) {
            case "ALL":
                filteredBookings.addAll(allBookings);
                break;
            case "ACTIVE":
                for (Booking booking : allBookings) {
                    String status = booking.getStatus();
                    // Active filter now shows only "Started" bookings (charging sessions in progress)
                    if ("Started".equalsIgnoreCase(status)) {
                        filteredBookings.add(booking);
                    }
                }
                break;
            case "COMPLETED":
                for (Booking booking : allBookings) {
                    String status = booking.getStatus();
                    if ("Completed".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                        filteredBookings.add(booking);
                    }
                }
                break;
        }

        bookingAdapter.notifyDataSetChanged();
        
        // Show/hide empty state
        if (filteredBookings.isEmpty()) {
            rvBookings.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvBookings.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}