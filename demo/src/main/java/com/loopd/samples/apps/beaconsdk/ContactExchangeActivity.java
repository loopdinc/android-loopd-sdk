package com.loopd.samples.apps.beaconsdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.loopd.sdk.beacon.ContactExchangeManager;
import com.loopd.sdk.beacon.ScanningConfigs;
import com.loopd.sdk.beacon.listener.ContactExchangeListener;
import com.loopd.sdk.beacon.listener.DetectListener;
import com.loopd.sdk.beacon.model.Beacon;

import java.util.List;

public class ContactExchangeActivity extends AppCompatActivity implements View.OnClickListener, DetectListener, ContactExchangeListener {

    private TextView mHintTextView;
    private TextView mBeaconInfoTextView;
    private TextView mOutputTextView;
    private LinearLayout mBeaconInfoLayout;
    private Button mResetButton;
    private static final int SCAN_RSSI = -40;

    private ContactExchangeManager mContactExchangeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_exchange);

        mHintTextView = (TextView) findViewById(R.id.hint);
        mBeaconInfoTextView = (TextView) findViewById(R.id.beacon_info);
        mOutputTextView = (TextView) findViewById(R.id.output);
        mBeaconInfoLayout = (LinearLayout) findViewById(R.id.beacon_info_layout);
        mResetButton = (Button) findViewById(R.id.reset_button);
        mResetButton.setOnClickListener(this);

        mContactExchangeManager = new ContactExchangeManager(getApplicationContext());
        mContactExchangeManager.setDetectingListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScanningConfigs configs = new ScanningConfigs(ScanningConfigs.SCAN_MODE_ALL, SCAN_RSSI, null);
        mContactExchangeManager.startDetecting(configs);
    }

    @Override
    public void onBeaconDetected(final Beacon beacon) {
        Log.e("TEST", "onBeaconDetected: " + beacon.getAddress());
        updateBeaconInfo(beacon);
        mContactExchangeManager.startListenContactExchange(new ScanningConfigs(ScanningConfigs.SCAN_MODE_WITH_DATA, null, beacon.getId()), this);
    }

    @Override
    public void onContactExchangeDataReceived(Beacon targetBeacon, List<String> data) {
        updateOutputDataText(data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mContactExchangeManager.stopDetecting();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mContactExchangeManager.release();
    }

    private void updateBeaconInfo(Beacon beacon) {
        mHintTextView.setVisibility(View.GONE);
        mBeaconInfoLayout.setVisibility(View.VISIBLE);
        mBeaconInfoTextView.setText(String.format("id: %s , mac: %s", beacon.getId(), beacon.getAddress()));
    }

    private void updateOutputDataText(List<String> data) {
        String outputString = mOutputTextView.getText().toString();
        for (String s : data) {
            outputString += s + "\n";
        }
        mOutputTextView.setText(outputString);
    }

    @Override
    public void onClick(View view) {
        resetLayout();
        mContactExchangeManager.stopListenContactExchange();
        ScanningConfigs configs = new ScanningConfigs(ScanningConfigs.SCAN_MODE_ALL, SCAN_RSSI, null);
        mContactExchangeManager.startDetecting(configs);
    }

    private void resetLayout() {
        mOutputTextView.setText("");
        mHintTextView.setVisibility(View.VISIBLE);
        mBeaconInfoLayout.setVisibility(View.GONE);
    }
}
