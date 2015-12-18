package com.loopd.samples.apps.beaconsdk;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.loopd.sdk.beacon.model.Beacon;

import java.util.List;

public class BeaconListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Beacon> mItems;

    public BeaconListAdapter(List<Beacon> items) {
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
        Beacon item = mItems.get(position);
        holderItem.beaconIdTextView.setText(item.getId());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ViewHolderItem extends RecyclerView.ViewHolder {
        View view;
        TextView beaconIdTextView;

        public ViewHolderItem(View view) {
            super(view);
            beaconIdTextView = (TextView) view.findViewById(R.id.beacon_id);
            this.view = view;
        }
    }
}
