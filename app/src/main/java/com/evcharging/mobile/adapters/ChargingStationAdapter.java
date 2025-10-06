package com.evcharging.mobile.adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.evcharging.mobile.R;
import com.evcharging.mobile.models.ChargingStation;
import java.util.List;

public class ChargingStationAdapter extends RecyclerView.Adapter<ChargingStationAdapter.StationViewHolder> {

    private Context context;
    private List<ChargingStation> stations;
    private OnStationClickListener onStationClickListener;

    public interface OnStationClickListener {
        void onStationClick(ChargingStation station);
    }

    public ChargingStationAdapter(Context context) {
        this.context = context;
    }

    public void setStations(List<ChargingStation> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    public void setOnStationClickListener(OnStationClickListener listener) {
        this.onStationClickListener = listener;
    }

    @Override
    public StationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_charging_station, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StationViewHolder holder, int position) {
        ChargingStation station = stations.get(position);
        holder.bind(station);
    }

    @Override
    public int getItemCount() {
        return stations != null ? stations.size() : 0;
    }

    class StationViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName, tvType, tvSlots, tvAddress, tvStatus;

        public StationViewHolder(View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvName);
            tvType = itemView.findViewById(R.id.tvType);
            tvSlots = itemView.findViewById(R.id.tvSlots);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvStatus = itemView.findViewById(R.id.tvStatus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onStationClickListener != null) {
                    onStationClickListener.onStationClick(stations.get(position));
                }
            });
        }

        public void bind(ChargingStation station) {
            tvName.setText(station.getName());
            tvType.setText(station.getStationType());
            tvSlots.setText(station.getTotalSlots() + " slots");

            if (station.getLocation() != null) {
                tvAddress.setText(station.getLocation().getFullAddress());
            }

            if (station.isActive()) {
                tvStatus.setText("Active");
                tvStatus.setTextColor(context.getResources().getColor(R.color.colorSuccess));
            } else {
                tvStatus.setText("Inactive");
                tvStatus.setTextColor(context.getResources().getColor(R.color.colorDanger));
            }
        }
    }
}
