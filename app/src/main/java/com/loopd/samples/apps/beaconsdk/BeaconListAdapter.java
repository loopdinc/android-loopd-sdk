package com.loopd.samples.apps.beaconsdk;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.loopd.sdk.beacon.model.Beacon;

import java.util.ArrayList;
import java.util.List;

public class BeaconListAdapter extends RecyclerView.Adapter<BeaconListAdapter.ViewItemHolder> {

    private List<BeaconListItem> mItems;
    private OnItemClickListener mOnItemClickListener;

    public BeaconListAdapter() {
        mItems = new ArrayList<>();
    }

    @Override
    public BeaconListAdapter.ViewItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beacon_item, parent, false);

        return new ViewItemHolder(view, this);
    }

    @Override
    public void onBindViewHolder(final BeaconListAdapter.ViewItemHolder itemHolder, int position) {
        BeaconListItem beaconListItem = mItems.get(position);
        itemHolder.setBeaconId(beaconListItem.getBeacon().getId());
        itemHolder.setBeaconAddress(beaconListItem.getBeacon().getAddress());
        itemHolder.setAdvertisementCount(beaconListItem.getAdvertisementCount());
    }

    public void addOrIncreaseAdvertisementCount(Beacon beacon) {
        BeaconListAdapter.BeaconListItem aBeaconListItem = new BeaconListAdapter.BeaconListItem(beacon);
        if (mItems.contains(aBeaconListItem)) {
            increaseAdvertisementCount(aBeaconListItem);
        } else {
            addItem(aBeaconListItem);
        }
    }

    public void addItem(BeaconListAdapter.BeaconListItem aBeaconListItem) {
        mItems.add(aBeaconListItem);
        notifyItemInserted(mItems.size() - 1);
    }

    public void increaseAdvertisementCount(BeaconListAdapter.BeaconListItem aBeaconListItem) {
        int existPosition = mItems.indexOf(aBeaconListItem);
        aBeaconListItem.setAdvertisementCount(mItems.get(existPosition).getAdvertisementCount() + 1);
        mItems.set(existPosition, aBeaconListItem);
        notifyItemChanged(existPosition);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void onItemHolderClick(ViewItemHolder itemHolder) {
        if (mOnItemClickListener != null) {
            BeaconListItem item = mItems.get(itemHolder.getAdapterPosition());
            mOnItemClickListener.onItemClick(itemHolder.getAdapterPosition(), item);
        }
    }

    public static class ViewItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mBeaconIdTextView;
        private TextView mBeaconAddressTextView;
        private TextView mAdvertisementCountTextView;

        private BeaconListAdapter mAdapter;

        public ViewItemHolder(View itemView, BeaconListAdapter adapter) {
            super(itemView);
            mAdapter = adapter;
            itemView.setOnClickListener(this);

            mBeaconIdTextView = (TextView) itemView.findViewById(R.id.beacon_id);
            mBeaconAddressTextView = (TextView) itemView.findViewById(R.id.beacon_address);
            mAdvertisementCountTextView = (TextView) itemView.findViewById(R.id.advertisement_count);
        }

        @Override
        public void onClick(View v) {
            mAdapter.onItemHolderClick(this);
        }

        public void setBeaconId(String beaconId) {
            mBeaconIdTextView.setText(beaconId);
        }

        public void setBeaconAddress(String beaconAddress) {
            mBeaconAddressTextView.setText(beaconAddress);
        }

        public void setAdvertisementCount(int count) {
            mAdvertisementCountTextView.setText(String.valueOf(count));
        }
    }

    public static class BeaconListItem {
        private Beacon mBeacon;
        private int mAdvertisementCount = 1;

        @Override
        public boolean equals(Object o) {
            return (o instanceof BeaconListItem) && mBeacon != null && o != null
                    && mBeacon.equals(((BeaconListItem) o).getBeacon());
        }

        public BeaconListItem(Beacon beacon) {
            mBeacon = beacon;
        }

        public Beacon getBeacon() {
            return mBeacon;
        }

        public void setBeacon(Beacon beacon) {
            mBeacon = beacon;
        }

        public int getAdvertisementCount() {
            return mAdvertisementCount;
        }

        public void setAdvertisementCount(int advertisementCount) {
            mAdvertisementCount = advertisementCount;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, BeaconListItem item);
    }
}
