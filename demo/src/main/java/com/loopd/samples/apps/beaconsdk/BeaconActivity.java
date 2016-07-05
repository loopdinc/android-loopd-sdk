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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopd.sdk.beacon.BeaconManager;
import com.loopd.sdk.beacon.listener.ConnectListener;
import com.loopd.sdk.beacon.model.Beacon;

import java.io.UnsupportedEncodingException;
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
        String dataString = null;
        try {
            dataString = new String(data, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onDataReceived: " + Arrays.toString(data) + " encoded: " + dataString);
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
        switch (position) {
            case 0:
                // Switch off Both Leds
                mBeaconManager.writeCommand(mLoopdCharacteristic, BeaconManager.COMMAND_TURN_OFF_BOTH_LEDS);
                break;
            case 1:
                // Switch on Both Leds
                mBeaconManager.writeCommand(mLoopdCharacteristic, BeaconManager.COMMAND_TURN_ON_BOTH_LEDS);
                break;
            case 2:
                // Disconnect Connection
                mBeaconManager.writeCommand(mLoopdCharacteristic, BeaconManager.COMMAND_FORCE_DISCONNECT);
                break;
            case 3:
                // Change Advertisement Frequency
                showModifyAdvertisementFrequencyDialog();
                break;
            case 4:
                // Disconnect Connection
                mBeaconManager.writeCommand(mLoopdCharacteristic, BeaconManager.COMMAND_READ_DATA);
                break;
        }
    }

    private void showModifyAdvertisementFrequencyDialog() {
        new AlertDialog.Builder(this)
                .setItems(getResources().getStringArray(R.array.advertisement_frequency_options), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        byte[] param = null;
                        switch (which) {
                            case 0:
                                // 1 per second
                                param = new byte[]{0x01};
                                break;
                            case 1:
                                // 2 per second
                                param = new byte[]{0x02};
                                break;
                            case 2:
                                // 4 per second
                                param = new byte[]{0x04};
                                break;
                            case 3:
                                // 8 per second
                                param = new byte[]{0x08};
                                break;
                        }
                        mBeaconManager.writeCommand(mLoopdCharacteristic, BeaconManager.COMMAND_CHANGE_ADVERTISEMENT_FREQUENCY, param);
                    }
                })
                .show();
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
