package com.evcharging.mobile.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.evcharging.mobile.R;
import com.evcharging.mobile.models.Booking;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OperatorBookingAdapter extends RecyclerView.Adapter<OperatorBookingAdapter.BookingViewHolder> {

    private List<Booking> bookings;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    public OperatorBookingAdapter(List<Booking> bookings, Context context) {
        this.bookings = bookings;
        this.context = context;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_operator_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        
        // Set booking ID (use a substring if too long)
        String bookingId = booking.getId();
        if (bookingId.length() > 8) {
            bookingId = "..." + bookingId.substring(bookingId.length() - 8);
        }
        holder.tvBookingId.setText("Booking #" + bookingId);
        
        // Set customer NIC
        holder.tvCustomerNIC.setText("NIC: " + booking.getEvOwnerNIC());
        
        // Set date and time
        if (booking.getReservationDateTime() != null) {
            holder.tvDateTime.setText(dateFormat.format(booking.getReservationDateTime()));
        }
        
        // Set duration
        int durationMinutes = booking.getDurationMinutes();
        String duration;
        if (durationMinutes >= 60) {
            int hours = durationMinutes / 60;
            int minutes = durationMinutes % 60;
            if (minutes > 0) {
                duration = hours + "h " + minutes + "m";
            } else {
                duration = hours + " hour" + (hours > 1 ? "s" : "");
            }
        } else {
            duration = durationMinutes + " minutes";
        }
        holder.tvDuration.setText("Duration: " + duration);
        
        // Set charging point (using slotId)
        holder.tvChargingPoint.setText("Charging Point: " + booking.getSlotId());
        
        // Set status with appropriate styling
        String status = booking.getStatus();
        holder.tvStatus.setText(status);
        
        // Update status background based on status
        if ("Approved".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_badge_active);
        } else if ("Completed".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_badge_completed);
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_badge_cancelled);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_badge_pending);
        }

        // Show action buttons only for active bookings
        if ("Approved".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status)) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            
            holder.btnViewDetails.setOnClickListener(v -> {
                // TODO: Implement view details functionality
                android.widget.Toast.makeText(context, "View details: " + booking.getId(), android.widget.Toast.LENGTH_SHORT).show();
            });
            
            holder.btnVerify.setOnClickListener(v -> {
                // TODO: Implement verification functionality
                android.widget.Toast.makeText(context, "Verify booking: " + booking.getId(), android.widget.Toast.LENGTH_SHORT).show();
            });
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvStatus, tvCustomerNIC, tvDateTime, tvDuration, tvChargingPoint;
        LinearLayout layoutActions;
        MaterialButton btnViewDetails, btnVerify;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCustomerNIC = itemView.findViewById(R.id.tvCustomerNIC);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvChargingPoint = itemView.findViewById(R.id.tvChargingPoint);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnVerify = itemView.findViewById(R.id.btnVerify);
        }
    }
}