package com.loopd.samples.apps.beaconsdk;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.loopd.sdk.beacon.BeaconManager;
import com.loopd.sdk.beacon.listener.ConnectListener;
import com.loopd.sdk.beacon.model.Beacon;

import java.util.Arrays;

public class BeaconActivity extends AppCompatActivity implements ConnectListener {

    public static final String TAG = "BeaconActivity";
    private static final String INTENT_PARAMS_BEACON = "INTENT_PARAMS_BEACON";

    private Beacon mBeacon;
    private BeaconManager mBeaconManager;
    private BluetoothGattCharacteristic mLoopdCharacteristic;

    private TextView mStatusTextView;
    private TextView mBeaconIdTextView;
    private TextView mBeaconAddressTextView;
    private Button mWriteCommandButton;

    public static Intent getCallingIntent(Context context, Beacon beacon) {
        Intent intent = new Intent(context, BeaconActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(INTENT_PARAMS_BEACON, beacon);
        intent.putExtras(bundle);

        return intent;
    }

    @Override
    public void onBeaconConnected() {
        Log.d(TAG, "onBeaconConnected");
        mStatusTextView.setText(R.string.wait_for_service);
    }

    @Override
    public void onBeaconDisconnected() {
        Log.d(TAG, "onBeaconDisconnected");
        mStatusTextView.setText(R.string.disconnected);
        mWriteCommandButton.setVisibility(View.GONE);
    }

    @Override
    public void onDataServiceAvailable(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onDataServiceAvailable: " + characteristic);
        mLoopdCharacteristic = characteristic;
        mStatusTextView.setText(R.string.connected);
        mWriteCommandButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDataReceived(byte[] data) {
        Log.d(TAG, "onDataReceived: " + byteArrayToHex(data));
        if (Arrays.equals(data, new byte[]{0x00})) {
            mBeaconManager.writeCommand(mLoopdCharacteristic, BeaconManager.COMMAND_CHANGE_STATE_IN_EVENT);
        }
    }

    public static String byteArrayToHex(byte[] byteArray) {
        StringBuilder sb = new StringBuilder(byteArray.length * 2);
        for (byte b: byteArray) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Override
    public void onConnectTimout() {
        Log.d(TAG, "onConnectTimout");
        mStatusTextView.setText(R.string.disconnected);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mBeacon = extras.getParcelable(INTENT_PARAMS_BEACON);
        }
        mBeaconManager = new BeaconManager(getApplicationContext());
        setContentView(R.layout.activity_beacon);
        initViews();
    }

    private void initViews() {
        mStatusTextView = (TextView) findViewById(R.id.status);
        mBeaconIdTextView = (TextView) findViewById(R.id.beacon_id);
        mBeaconAddressTextView = (TextView) findViewById(R.id.beacon_address);
        mWriteCommandButton = (Button) findViewById(R.id.write_command_btn);

        mBeaconIdTextView.setText(mBeacon.getId());
        mBeaconAddressTextView.setText(mBeacon.getAddress());
        mWriteCommandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBadgeTestingCommand();
            }
        });
    }

    private void sendBadgeTestingCommand() {
        mBeaconManager.writeCommand(mLoopdCharacteristic, new byte[]{0x40});
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStatusTextView.setText(R.string.connecting);
        mWriteCommandButton.setVisibility(View.GONE);
        mBeaconManager.connect(mBeacon, BeaconActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBeaconManager.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBeaconManager.release();
    }
}
