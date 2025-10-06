package com.evcharging.mobile.adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.evcharging.mobile.R;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.DateUtils;
import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.HistoryViewHolder> {

    private Context context;
    private List<Booking> bookings;

    public BookingHistoryAdapter(Context context) {
        this.context = context;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
        notifyDataSetChanged();
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {

        private TextView tvReference, tvStation, tvDateTime, tvStatus, tvDuration;

        public HistoryViewHolder(View itemView) {
            super(itemView);

            tvReference = itemView.findViewById(R.id.tvReference);
            tvStation = itemView.findViewById(R.id.tvStation);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }

        public void bind(Booking booking) {
            tvReference.setText(booking.getBookingReference());
            tvDateTime.setText(DateUtils.formatDateTime(booking.getReservationDateTime()));
            tvDuration.setText(booking.getDurationMinutes() + " minutes");
            tvStatus.setText(booking.getStatus());

            // Set station name if available
            if (booking.getStationName() != null) {
                tvStation.setText(booking.getStationName());
            } else {
                tvStation.setText("Station: " + booking.getChargingStationId());
            }

            // Set status color
            int statusColor = getStatusColor(booking.getStatus());
            tvStatus.setTextColor(statusColor);
        }

        private int getStatusColor(String status) {
            switch (status) {
                case "Pending":
                    return context.getResources().getColor(R.color.colorWarning);
                case "Approved":
                    return context.getResources().getColor(R.color.colorSuccess);
                case "Completed":
                    return context.getResources().getColor(R.color.colorInfo);
                case "Cancelled":
                    return context.getResources().getColor(R.color.colorDanger);
                default:
                    return context.getResources().getColor(R.color.colorSecondary);
            }
        }
    }
}
