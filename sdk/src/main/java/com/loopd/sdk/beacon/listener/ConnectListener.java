package com.loopd.sdk.beacon.listener;

import android.bluetooth.BluetoothGattCharacteristic;

public interface ConnectListener {

    void onBeaconConnected();

    void onBeaconDisconnected();

    void onDataServiceAvailable(BluetoothGattCharacteristic characteristic);

    void onDataReceived(byte[] data);

    void onConnectTimout();
}
