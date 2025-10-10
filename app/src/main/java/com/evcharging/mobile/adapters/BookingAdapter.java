package com.evcharging.mobile.adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.evcharging.mobile.R;
import com.evcharging.mobile.models.Booking;
import com.evcharging.mobile.utils.DateUtils;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private Context context;
    private List<Booking> bookings;
    private boolean showActions;
    private OnBookingClickListener onBookingClickListener;
    private OnCancelClickListener onCancelClickListener;
    private OnQRClickListener onQRClickListener;

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    public interface OnCancelClickListener {
        void onCancelClick(Booking booking);
    }

    public interface OnQRClickListener {
        void onQRClick(Booking booking);
    }

    public BookingAdapter(Context context, boolean showActions) {
        this.context = context;
        this.showActions = showActions;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
        notifyDataSetChanged();
    }

    public void setOnBookingClickListener(OnBookingClickListener listener) {
        this.onBookingClickListener = listener;
    }

    public void setOnCancelClickListener(OnCancelClickListener listener) {
        this.onCancelClickListener = listener;
    }

    public void setOnQRClickListener(OnQRClickListener listener) {
        this.onQRClickListener = listener;
    }

    @Override
    public BookingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {

        private TextView tvReference, tvStation, tvDateTime, tvStatus;
        private Button btnCancel, btnQR, btnDetails;
        private View layoutActions;

        public BookingViewHolder(View itemView) {
            super(itemView);

            tvReference = itemView.findViewById(R.id.tvReference);
            tvStation = itemView.findViewById(R.id.tvStation);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnQR = itemView.findViewById(R.id.btnQR);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            layoutActions = itemView.findViewById(R.id.layoutActions);

            // Show/hide actions based on adapter configuration
            layoutActions.setVisibility(showActions ? View.VISIBLE : View.GONE);
        }

        public void bind(Booking booking) {
            tvReference.setText(booking.getBookingReference());
            tvDateTime.setText(DateUtils.formatDateTime(booking.getReservationDateTime()));
            tvStatus.setText(booking.getStatus());

            // Set station name consistently
            if (booking.getStationName() != null && !booking.getStationName().trim().isEmpty()) {
                tvStation.setText(booking.getStationName());
            } else {
                tvStation.setText("Station: " + booking.getChargingStationId());
            }

            // Set status color
            int statusColor = getStatusColor(booking.getStatus());
            tvStatus.setTextColor(statusColor);

            // Setup button states
            btnCancel.setEnabled(booking.canBeCancelled());
            btnQR.setEnabled("Approved".equals(booking.getStatus()));

            // Click listeners
            btnDetails.setOnClickListener(v -> {
                if (onBookingClickListener != null) {
                    onBookingClickListener.onBookingClick(booking);
                }
            });

            btnCancel.setOnClickListener(v -> {
                if (onCancelClickListener != null) {
                    onCancelClickListener.onCancelClick(booking);
                }
            });

            btnQR.setOnClickListener(v -> {
                if (onQRClickListener != null) {
                    onQRClickListener.onQRClick(booking);
                }
            });

            // Item click listener
            itemView.setOnClickListener(v -> {
                if (onBookingClickListener != null) {
                    onBookingClickListener.onBookingClick(booking);
                }
            });
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
