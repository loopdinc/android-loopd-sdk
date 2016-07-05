package com.loopd.samples.apps.beaconsdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.loopd.sdk.beacon.ContactExchangeManager;
import com.loopd.sdk.beacon.ScanningConfigs;
import com.loopd.sdk.beacon.listener.ContactExchangeListener;
import com.loopd.sdk.beacon.model.Beacon;

import java.util.List;

public class ContactExchangeActivity extends AppCompatActivity implements ContactExchangeListener {

    private TextView mOutputTextView;
    private ContactExchangeManager mContactExchangeManager;
    private ScanningConfigs mScanningConfigs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_exchange);

        mOutputTextView = (TextView) findViewById(R.id.output);

        mContactExchangeManager = new ContactExchangeManager(getApplicationContext());
        mScanningConfigs = new ScanningConfigs(ScanningConfigs.SCAN_MODE_WITH_DATA, null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mContactExchangeManager.startListenContactExchange(mScanningConfigs, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mContactExchangeManager.stopListenContactExchange();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mContactExchangeManager.release();
    }

    @Override
    public void onContactExchangeDataReceived(Beacon targetBeacon, List<String> data) {
        updateOutputDataText(targetBeacon, data);
    }

    private void updateOutputDataText(Beacon targetBeacon, List<String> data) {
        String outputString = mOutputTextView.getText().toString();
        for (String s : data) {
            outputString += targetBeacon.getId() + " -> " + s + "\n";
        }
        mOutputTextView.setText(outputString);
    }
}
