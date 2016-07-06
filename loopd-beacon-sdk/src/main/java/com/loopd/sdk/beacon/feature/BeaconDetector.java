package com.loopd.sdk.beacon.feature;

import com.loopd.sdk.beacon.ScanningConfigs;
import com.loopd.sdk.beacon.listener.DetectListener;

public interface BeaconDetector {

    void setDetectingListener(DetectListener detectListener);

    void startDetecting(ScanningConfigs configs);

    void stopDetecting();

    boolean isDetecting();
}
