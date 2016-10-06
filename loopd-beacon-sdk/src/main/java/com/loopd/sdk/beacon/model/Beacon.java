package com.loopd.sdk.beacon.model;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Beacon implements Parcelable {

    private static final int DATA_TYPE_SERVICE_DATA = 0x16;

    @IntDef({STATE_INACTIVE, STATE_IN_TEST, STATE_UNREGISTERED, STATE_REGISTERED, STATE_IN_EVENT,
            STATE_CONTACT_EXCHANGE, STATE_AWAY_FROM_EVENT, STATE_RETURN, STATE_SHIPPING, STATE_SYS_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    @interface State {}

    public static final int STATE_INACTIVE = 1;
    public static final int STATE_IN_TEST = 2;
    public static final int STATE_UNREGISTERED = 3;
    public static final int STATE_REGISTERED = 4;
    public static final int STATE_IN_EVENT = 5;
    public static final int STATE_CONTACT_EXCHANGE = 6;
    public static final int STATE_AWAY_FROM_EVENT = 7;
    public static final int STATE_RETURN = 8;
    public static final int STATE_SHIPPING = 9;
    public static final int STATE_SYS_FAILURE = 10;

    @IntDef({BATTERY_LESS_THAN_15_PERCENT, BATTERY_ABOVE_15_PERCENT, BATTERY_ABOVE_30_PERCENT,
            BATTERY_ABOVE_45_PERCENT, BATTERY_ABOVE_60_PERCENT, BATTERY_ABOVE_75_PERCENT, BATTERY_ABOVE_90_PERCENT})
    @Retention(RetentionPolicy.SOURCE)
    @interface BatteryInfo {}

    public static final int BATTERY_LESS_THAN_15_PERCENT = 1;
    public static final int BATTERY_ABOVE_15_PERCENT = 2;
    public static final int BATTERY_ABOVE_30_PERCENT = 3;
    public static final int BATTERY_ABOVE_45_PERCENT = 4;
    public static final int BATTERY_ABOVE_60_PERCENT = 5;
    public static final int BATTERY_ABOVE_75_PERCENT = 6;
    public static final int BATTERY_ABOVE_90_PERCENT = 7;

    private String mId;
    private String mAddress;
    private int mRssi;
    private boolean mWithData;
//    private int mState;
    private int mBatteryInfo;
    private String mFirmwareVersion;

    public Beacon(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device != null) {
            mId = device.getName();
            mAddress = device.getAddress();
        }
        mRssi = rssi;
        byte manufactureId1 = scanRecord[28];
        byte manufactureId2 = scanRecord[29];
        byte manufactureId3 = scanRecord[30];
        mWithData = (manufactureId1 & 0xff) == 0x02 && (manufactureId2 & 0xff) == 0xFF && (manufactureId3 & 0xff) == 0x01;
//        mState = (scanRecord[30] & 0xf0) >>> 4;
        mBatteryInfo = scanRecord[30] & 0x07;
        if ((scanRecord[32] & 0xff) == DATA_TYPE_SERVICE_DATA) {
            mFirmwareVersion = (scanRecord[33] & 0xff) + "." + (scanRecord[34] & 0xff);
        }
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Beacon) && mAddress != null && mAddress.equals(((Beacon) o).getAddress());
    }

    @Override
    public int hashCode() {
        return mAddress.hashCode();
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public int getRssi() {
        return mRssi;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public boolean isWithData() {
        return mWithData;
    }

//    public int getState() {
//        return mState;
//    }
//
//    public void setState(@State int state) {
//        mState = state;
//    }

    public int getBatteryInfo() {
        return mBatteryInfo;
    }

    public void setBatteryInfo(@BatteryInfo int batteryInfo) {
        mBatteryInfo = batteryInfo;
    }

    public String getFirmwareVersion() {
        return mFirmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        mFirmwareVersion = firmwareVersion;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mId);
        dest.writeString(this.mAddress);
        dest.writeInt(this.mRssi);
        dest.writeByte(mWithData ? (byte) 1 : (byte) 0);
//        dest.writeInt(this.mState);
        dest.writeInt(this.mBatteryInfo);
        dest.writeString(this.mFirmwareVersion);
    }

    protected Beacon(Parcel in) {
        this.mId = in.readString();
        this.mAddress = in.readString();
        this.mRssi = in.readInt();
        this.mWithData = in.readByte() != 0;
//        this.mState = in.readInt();
        this.mBatteryInfo = in.readInt();
        this.mFirmwareVersion = in.readString();
    }

    public static final Parcelable.Creator<Beacon> CREATOR = new Parcelable.Creator<Beacon>() {
        public Beacon createFromParcel(Parcel source) {
            return new Beacon(source);
        }

        public Beacon[] newArray(int size) {
            return new Beacon[size];
        }
    };
}
