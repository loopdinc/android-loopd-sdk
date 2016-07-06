package com.loopd.sdk.beacon;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ScanningConfigs {
    @IntDef({SCAN_MODE_ALL, SCAN_MODE_WITH_DATA})
    @Retention(RetentionPolicy.SOURCE)
    @interface ScanMode {}

    public static final int SCAN_MODE_ALL = 0;
    public static final int SCAN_MODE_WITH_DATA = 1;

    private int mMode;
    private Integer mRssi;
    private String mBeaconId;
    private boolean mIsEnableAutoRescan = true;

    public ScanningConfigs(@ScanMode int mode, Integer rssi, String beaconId) {
        mMode = mode;
        mRssi = rssi;
        mBeaconId = beaconId;
    }

    public int getMode() {
        return mMode;
    }

    public Integer getRssi() {
        return mRssi;
    }

    public String getBeaconId() {
        return mBeaconId;
    }

    public boolean isEnableAutoRescan() {
        return mIsEnableAutoRescan;
    }

    public void setIsEnableAutoRescan(boolean isEnableAutoRescan) {
        mIsEnableAutoRescan = isEnableAutoRescan;
    }
}
