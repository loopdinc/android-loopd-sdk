package com.loopd.sdk.beacon.feature;

import com.loopd.sdk.beacon.listener.RangingListener;
import com.loopd.sdk.beacon.ScanningConfigs;

public interface BeaconScanner {

    void setRangingListener(RangingListener rangingListener);

    void startRanging(ScanningConfigs configs);

    void stopRanging();

    boolean isRanging();
}
