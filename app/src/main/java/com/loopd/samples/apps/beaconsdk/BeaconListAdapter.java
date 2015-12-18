package com.loopd.samples.apps.beaconsdk;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.loopd.sdk.beacon.model.Beacon;

import java.util.List;

public class BeaconListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<BeaconListItem> mItems;

    public BeaconListAdapter(List<BeaconListItem> items) {
        mItems = items;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.beacon_item, parent, false);

        return new ViewHolderItem(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolderItem holderItem = (ViewHolderItem) holder;
        BeaconListItem beaconListItem = mItems.get(position);
        holderItem.beaconIdTextView.setText(beaconListItem.getBeacon().getId());
        holderItem.beaconAddressTextView.setText(beaconListItem.getBeacon().getAddress());
        holderItem.advertisementCountTextView.setText(String.valueOf(beaconListItem.getAdvertisementCount()));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ViewHolderItem extends RecyclerView.ViewHolder {
        View view;
        TextView beaconIdTextView;
        TextView beaconAddressTextView;
        TextView advertisementCountTextView;

        public ViewHolderItem(View view) {
            super(view);
            beaconIdTextView = (TextView) view.findViewById(R.id.beacon_id);
            beaconAddressTextView = (TextView) view.findViewById(R.id.beacon_address);
            advertisementCountTextView = (TextView) view.findViewById(R.id.advertisement_count);
            this.view = view;
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
}
