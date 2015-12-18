package com.loopd.samples.apps.beaconsdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

import com.loopd.sdk.beacon.model.Beacon;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private BeaconListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        List<Beacon> beaconList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Beacon beacon = new Beacon(null, 0, new byte[50]);
            beacon.setId(String.valueOf(i));
            beaconList.add(beacon);
        }
        mAdapter = new BeaconListAdapter(beaconList);
        mRecyclerView.setAdapter(mAdapter);
    }
}
