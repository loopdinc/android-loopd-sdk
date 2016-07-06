package com.loopd.sdk.beacon.feature;

import android.bluetooth.BluetoothGattCharacteristic;

import com.loopd.sdk.beacon.listener.ConnectListener;
import com.loopd.sdk.beacon.model.Beacon;

public interface BeaconConnector {

    void connect(Beacon beacon, ConnectListener connectListener);

    void disconnect();

    void writeCommand(BluetoothGattCharacteristic characteristic, byte[] values);

    void writeCommand(String address, byte[] values);
}
