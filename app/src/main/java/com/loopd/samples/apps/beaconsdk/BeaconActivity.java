package com.loopd.samples.apps.beaconsdk;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.loopd.sdk.beacon.BeaconManager;
import com.loopd.sdk.beacon.listener.ConnectListener;
import com.loopd.sdk.beacon.model.Beacon;

public class BeaconActivity extends AppCompatActivity implements ConnectListener {

    public static final String TAG = "BeaconActivity";
    private static final String INTENT_PARAMS_BEACON = "INTENT_PARAMS_BEACON";

    private Beacon mBeacon;
    private BeaconManager mBeaconManager;

    public static Intent getCallingIntent(Context context, Beacon beacon) {
        Intent intent = new Intent(context, BeaconActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(INTENT_PARAMS_BEACON, beacon);
        intent.putExtras(bundle);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mBeacon = extras.getParcelable(INTENT_PARAMS_BEACON);
        }
        mBeaconManager = new BeaconManager(getApplicationContext());
        setContentView(R.layout.activity_beacon);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBeaconManager.connect(mBeacon, this);
    }

    @Override
    public void onBeaconConnected() {
        Log.d(TAG, "onBeaconConnected");
    }

    @Override
    public void onBeaconDisconnected() {
        Log.d(TAG, "onBeaconDisconnected");
    }

    @Override
    public void onDataServiceAvailable(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onDataServiceAvailable: " + characteristic);
    }

    @Override
    public void onDataReceived(byte[] data) {
        Log.d(TAG, "onDataReceived: " + data);
    }

    @Override
    public void onConnectTimout() {
        Log.d(TAG, "onConnectTimout");
    }
}
