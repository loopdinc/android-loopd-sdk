package com.loopd.sdk.beacon;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.loopd.sdk.beacon.feature.BluetoothVerifier;
import com.loopd.sdk.beacon.feature.ContactExchangeMonitor;
import com.loopd.sdk.beacon.feature.BeaconDetector;
import com.loopd.sdk.beacon.listener.ConnectListener;
import com.loopd.sdk.beacon.listener.ContactExchangeListener;
import com.loopd.sdk.beacon.listener.DetectListener;
import com.loopd.sdk.beacon.listener.RangingListener;
import com.loopd.sdk.beacon.model.Beacon;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.loopd.sdk.beacon.BeaconManager.COMMAND_FORCE_DISCONNECT;
import static com.loopd.sdk.beacon.BeaconManager.COMMAND_READ_DATA;

public class ContactExchangeManager implements BluetoothVerifier, BeaconDetector, ContactExchangeMonitor {
    private static final String TAG = "ContactExchangeManager";
    private static final long CONTACT_EXCHANGE_CONNECT_LIMITED_PERIOD = 5000;
    private static final long CONTACT_EXCHANGE_DATA_RECEIVER_WAIT_PERIOD = 1000;
    private static final ScanningConfigs SCANNING_CONFIGS_ALL = new ScanningConfigs(ScanningConfigs.SCAN_MODE_WITH_DATA, null, null);
    private Context mAppContext;
    private BeaconManager mMonitorBeaconManager;
    private BeaconManager mDetectingBeaconManager;
    private DetectListener mDetectListener;

    public ContactExchangeManager(@NonNull Context appContext) {
        mAppContext = appContext;
        mMonitorBeaconManager = new BeaconManager(appContext);
        mDetectingBeaconManager = new BeaconManager(appContext);
    }

    public void release() {
        mDetectingBeaconManager.release();
        mMonitorBeaconManager.release();
    }

    @Override
    public boolean hasBluetooth() {
        return mMonitorBeaconManager.hasBluetooth();
    }

    @Override
    public boolean isBluetoothEnabled() {
        return mMonitorBeaconManager.isBluetoothEnabled();
    }

    @Override
    public void startListenContactExchange(@NonNull final ScanningConfigs configs, final ContactExchangeListener contactExchangeListener) {
        if (mMonitorBeaconManager.isRanging()) {
            mMonitorBeaconManager.stopRanging();
        }
        mMonitorBeaconManager.setRangingListener(new RangingListener() {
            @Override
            public void onBeaconDiscovered(final Beacon beacon) {
                if (configs.getBeaconId() == null || configs.getBeaconId().equals(beacon.getId())) {
                    if (mMonitorBeaconManager.isRanging()) {
                        mMonitorBeaconManager.stopRanging();
                    }
                    mMonitorBeaconManager.connect(beacon, new ConnectListener() {
                        boolean hasGotFirstData = false;
                        List<String> receivedDataList = new ArrayList<>();
                        BluetoothGattCharacteristic lastCharacteristic;

                        @Override
                        public void onBeaconConnected() {
                            Log.i(TAG, "onBeaconConnected");
                            if (!hasGotFirstData) {
                                mMonitorBeaconManager.writeCommand(beacon.getAddress(), COMMAND_READ_DATA);
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!hasGotFirstData) {
                                        Log.i(TAG, "timeout after beacon connected");
                                        collectDataAndBackToDetecting();
                                    }
                                }
                            }, CONTACT_EXCHANGE_CONNECT_LIMITED_PERIOD);
                        }

                        @Override
                        public void onBeaconDisconnected() {
                            Log.i(TAG, "onBeaconDisconnected");
                        }

                        @Override
                        public void onDataServiceAvailable(BluetoothGattCharacteristic characteristic) {
                            Log.i(TAG, "onDataServiceAvailable");
                            lastCharacteristic = characteristic;
                        }

                        @Override
                        public void onDataReceived(byte[] data) {
                            String utf8String = null;
                            try {
                                utf8String = new String(data, "UTF-8");
                                utf8String = utf8String.replaceAll("\\u0007", "");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            Log.i(TAG, "onDataReceived: " + utf8String);
                            receivedDataList.add(utf8String);
                            if (!hasGotFirstData) {
                                hasGotFirstData = true;
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        collectDataAndBackToDetecting();
                                    }
                                }, CONTACT_EXCHANGE_DATA_RECEIVER_WAIT_PERIOD);
                            }
                        }

                        @Override
                        public void onConnectTimeout() {
                            Log.i(TAG, "onConnectTimeout");
                            collectDataAndBackToDetecting();
                        }

                        private void collectDataAndBackToDetecting() {
                            if (hasGotFirstData && receivedDataList.size() > 0) {
                                if (contactExchangeListener != null) {
                                    // must use other list to return, because this list is defined only in this scope
                                    List<String> resultData = new ArrayList<>();
                                    resultData.addAll(receivedDataList);
                                    contactExchangeListener.onContactExchangeDataReceived(beacon, resultData);
                                    receivedDataList.clear();
                                }
                            }
                            if (lastCharacteristic != null) {
                                mMonitorBeaconManager.writeCommand(lastCharacteristic, COMMAND_FORCE_DISCONNECT);
                            }
                            mMonitorBeaconManager.disconnect();
                            mMonitorBeaconManager.startRanging(configs);
                        }
                    });
                }
            }
        });
        mMonitorBeaconManager.startRanging(configs);
    }

    @Override
    public void stopListenContactExchange() {
        mMonitorBeaconManager.stopRanging();
    }

    @Override
    public void setDetectingListener(DetectListener detectListener) {
        mDetectListener = detectListener;
    }

    @Override
    public void startDetecting(ScanningConfigs configs) {
        if (mDetectingBeaconManager.isRanging()) {
            mDetectingBeaconManager.stopRanging();
        }
        mDetectingBeaconManager.setRangingListener(new RangingListener() {
            @Override
            public void onBeaconDiscovered(final Beacon beacon) {
                mDetectingBeaconManager.stopRanging();
                if (mDetectListener != null) {
                    mDetectListener.onBeaconDetected(beacon);
                }
            }
        });
        mDetectingBeaconManager.startRanging(configs);
    }

    @Override
    public void stopDetecting() {
        mDetectingBeaconManager.stopRanging();
    }

    @Override
    public boolean isDetecting() {
        return mDetectingBeaconManager.isRanging();
    }
}
