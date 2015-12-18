package com.loopd.samples.apps.beaconsdk;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.loopd.sdk.beacon.BeaconManager;
import com.loopd.sdk.beacon.ScanningConfigs;
import com.loopd.sdk.beacon.listener.RangingListener;
import com.loopd.sdk.beacon.model.Beacon;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RangingListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 2;

    private BeaconManager mBeaconManager;
    private ScanningConfigs mScanningConfigs;

    private RecyclerView mRecyclerView;
    private BeaconListAdapter mAdapter;
    private List<Beacon> mBeacons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mAdapter = new BeaconListAdapter(mBeacons);
        mRecyclerView.setAdapter(mAdapter);

        mBeaconManager = new BeaconManager(getApplicationContext());
        if (!mBeaconManager.hasBluetooth()) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        mBeaconManager.setRangingListener(this);
        mScanningConfigs = new ScanningConfigs(ScanningConfigs.SCAN_MODE_ALL, null, null);
    }

    @Override
    public void onBeaconDiscoverd(Beacon beacon) {
        if (mBeacons.contains(beacon)) {
            int existPosition = mBeacons.indexOf(beacon);
            mBeacons.set(existPosition, beacon);
            mAdapter.notifyItemChanged(existPosition);
        } else {
            mBeacons.add(beacon);
            mAdapter.notifyItemChanged(mBeacons.size() - 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBeaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQUEST_ACCESS_COARSE_LOCATION);
        } else {
            mBeaconManager.startRanging(mScanningConfigs);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBeaconManager.stopRanging();
        invalidateOptionsMenu();
        mBeacons.clear();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBeaconManager.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (mBeaconManager.isRanging()) {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mBeacons.clear();
                mAdapter.notifyDataSetChanged();
                mBeaconManager.startRanging(mScanningConfigs);
                break;
            case R.id.menu_stop:
                mBeaconManager.stopRanging();
                break;
        }
        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    mBeaconManager.startRanging(mScanningConfigs);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
