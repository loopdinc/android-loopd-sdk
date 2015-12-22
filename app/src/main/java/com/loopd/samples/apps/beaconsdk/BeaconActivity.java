package com.loopd.samples.apps.beaconsdk;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
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
        Log.d(TAG, "onDataReceived: " + data);
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
                showCommandDialog();
            }
        });
    }

    private void showCommandDialog() {
        new AlertDialog.Builder(this)
                .setItems(getResources().getStringArray(R.array.command_array), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onCommandDialogItemSelected(which);
                    }
                })
                .show();
    }

    public void onCommandDialogItemSelected(int position) {
        byte commandByte = 0;
        switch (position) {
            case 0:
                commandByte = (byte) 0x00;
                break;
            case 1:
                commandByte = (byte) 0x0F;
                break;
            case 2:
                commandByte = (byte) 0xF0;
                break;
            case 3:
                commandByte = (byte) 0xFF;
                break;
        }
        byte[] bytes = new byte[1];
        Arrays.fill(bytes, commandByte);
        mBeaconManager.writeCommand(mLoopdCharacteristic, bytes);
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
