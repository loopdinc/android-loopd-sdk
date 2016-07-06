package com.loopd.sdk.beacon.listener;

import com.loopd.sdk.beacon.model.Beacon;

public interface DetectListener {

    void onBeaconDetected(Beacon beacon);
}
