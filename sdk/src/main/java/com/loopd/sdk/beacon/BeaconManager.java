package com.loopd.sdk.beacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanRecord;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.loopd.sdk.beacon.feature.BeaconConnector;
import com.loopd.sdk.beacon.feature.BeaconScanner;
import com.loopd.sdk.beacon.feature.BluetoothVerifier;
import com.loopd.sdk.beacon.general.util.GeneralUtil;
import com.loopd.sdk.beacon.listener.ConnectListener;
import com.loopd.sdk.beacon.listener.RangingListener;
import com.loopd.sdk.beacon.model.Beacon;
import com.loopd.sdk.beacon.service.BluetoothLeService;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.loopd.sdk.beacon.ScanningConfigs.SCAN_MODE_WITH_DATA;

public class BeaconManager implements BluetoothVerifier, BeaconScanner, BeaconConnector {
    private static final String TAG = "BeaconManager";
    public static final byte[] COMMAND_TURN_OFF_BOTH_LEDS = new byte[]{0x00};
//    public static final byte[] COMMAND_TURN_ON_RED_LED = new byte[]{0x0F};
//    public static final byte[] COMMAND_TURN_ON_YELLOW_LED = new byte[]{(byte) 0xF0};
    public static final byte[] COMMAND_TURN_ON_BOTH_LEDS = new byte[]{(byte) 0xFF};
//    public static final byte[] COMMAND_BLINK_LEDS_PWM = new byte[]{(byte) 0xF5};
//    public static final byte[] COMMAND_STOP_BLINK_LEDS_PWM = new byte[]{(byte) 0xF6};
//    public static final byte[] COMMAND_BLINK_LEDS_ON_OFF = new byte[]{(byte) 0xF7};
//    public static final byte[] COMMAND_CHANGE_TRANSMISSION_POWER = new byte[]{0x10};
    public static final byte[] COMMAND_FORCE_DISCONNECT = new byte[]{0x11};
//    public static final byte[] COMMAND_GET_MAC_ADDRESS = new byte[]{0x12};
//    public static final byte[] COMMAND_GET_AMOUNT_OF_FREE_SPACE = new byte[]{0x14};
//    public static final byte[] COMMAND_GET_DEVICE_ID = new byte[]{0x02};
//    public static final byte[] COMMAND_SET_DEVICE_ID = new byte[]{0x20};
//    public static final byte[] COMMAND_IBEACON_ADVERTISEMENT = new byte[]{(byte) 0x80};
//    public static final byte[] COMMAND_EDDYSTONE_ADVERTISEMENT = new byte[]{(byte) 0x90};
//    public static final byte[] COMMAND_ADVERTISE_EDDYSTONE_AND_IBEACON = new byte[]{(byte) 0x89};
    public static final byte[] COMMAND_CHANGE_ADVERTISEMENT_FREQUENCY = new byte[]{(byte) 0xA0};
//    public static final byte[] COMMAND_SOFT_RESET = new byte[]{(byte) 0xEF};
    public static final byte[] COMMAND_READ_DATA = new byte[]{0x07};
//    public static final byte[] COMMAND_READ_DATA_WITHOUT_CLEAR = new byte[]{0x08};
//    public static final byte[] COMMAND_ERASE_STORAGE_DATA = new byte[]{0x20};
//    public static final byte[] COMMAND_CHANGE_STATE_INACTIVE = new byte[]{(byte) 0xD1};
//    public static final byte[] COMMAND_CHANGE_STATE_IN_TEST = new byte[]{(byte) 0xD2};
//    public static final byte[] COMMAND_CHANGE_STATE_UNREGISTERED = new byte[]{(byte) 0xD3};
//    public static final byte[] COMMAND_CHANGE_STATE_REGISTERED = new byte[]{(byte) 0xD4};
//    public static final byte[] COMMAND_CHANGE_STATE_IN_EVENT = new byte[]{(byte) 0xD5};
//    public static final byte[] COMMAND_CHANGE_STATE_CONTACT_EXCHANGE = new byte[]{(byte) 0xD6};
//    public static final byte[] COMMAND_CHANGE_STATE_AWAY_FROM_EVENT = new byte[]{(byte) 0xD7};
//    public static final byte[] COMMAND_CHANGE_STATE_RETURN = new byte[]{(byte) 0xD8};
//    public static final byte[] COMMAND_CHANGE_STATE_SHIPPING = new byte[]{(byte) 0xD9};
//    public static final byte[] COMMAND_CHANGE_STATE_SYS_FAILURE = new byte[]{(byte) 0xDA};
//    public static final byte[] COMMAND_GET_PREVIOUS_STATE = new byte[]{(byte) 0xDF};
//    public static final byte[] COMMAND_GET_FIRMWARE_VERSION = new byte[]{(byte) 0xE0};

    private static final int PREPARING_RETRY_TIME = 800;
    private static final int CONNECT_TIMEOUT = 12000;
    private static final int AUTO_RECONNECT = 1000;

    private Context mAppContext;

    private RangingListener mRangingListener;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private boolean mIsRanging;
    private ScanningConfigs mScanningConfigs;

    private boolean mIsBleServiceBound = false;
    private boolean mIsPreparing = false;
    private boolean mIsBleServiceConnected = false;
    private boolean mIsBeaconConnected = false;
    private ServiceConnection mServiceConnection;
    private BluetoothLeService mBluetoothLeService;
    private ConnectListener mConnectListener;
    private BroadcastReceiver mGattUpdateReceiver;
    private Handler mTimeoutHandler;
    private final Runnable mTimeoutTask = new Runnable() {
        @Override
        public void run() {
            if (!mIsBeaconConnected) {
                Log.i(TAG, "onConnectTimout");
                if (mConnectListener != null) {
                    mConnectListener.onConnectTimout();
                }
            }
        }
    };
    private Timer mBleRescanTimer = new Timer();

    public BeaconManager(@NonNull Context appContext) {
        mAppContext = appContext;
        initBluetoothManager();
        bindBluetoothLeService();
        registerGattUpdateReceiver();
    }

    private void initBluetoothManager() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void bindBluetoothLeService() {
        Intent gattServiceIntent = new Intent(mAppContext, BluetoothLeService.class);
        mIsBleServiceBound = mAppContext.bindService(gattServiceIntent, getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    private void registerGattUpdateReceiver() {
        mAppContext.registerReceiver(getGattUpdateReceiver(), makeGattUpdateIntentFilter());
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void release() {
        Log.i(TAG, "release");
        if (isRanging()) {
            stopRanging();
        }
        if (mIsBleServiceBound) {
            mAppContext.unbindService(getServiceConnection());
            mIsBleServiceBound = false;
        }
        mAppContext.unregisterReceiver(getGattUpdateReceiver());
        disconnect();
        mBluetoothLeService.close();
    }

    @Override
    public boolean hasBluetooth() {
        return mBluetoothAdapter != null;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter != null ? mBluetoothAdapter.isEnabled() : false;
    }

    @Override
    public void setRangingListener(RangingListener rangingListener) {
        mRangingListener = rangingListener;
    }

    @Override
    public void startRanging(@NonNull ScanningConfigs configs) {
        Log.i(TAG, "startRanging");
        if (!mIsRanging) {
            mIsRanging = true;
            mScanningConfigs = configs;
            if (configs.isEnableAutoRescan()) {
                mBleRescanTimer.cancel();
                mBleRescanTimer = new Timer();
                mBleRescanTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                            getBluetoothAdapterSafely().stopLeScan(mLeScanCallback);
                            getBluetoothAdapterSafely().startLeScan(getLeScanCallback());
                        }
                    }
                }, 0, AUTO_RECONNECT);
            } else {
                if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    getBluetoothAdapterSafely().stopLeScan(mLeScanCallback);
                    getBluetoothAdapterSafely().startLeScan(getLeScanCallback());
                }
            }
        } else {
            Log.e(TAG, "Need to call stopRanging before startRanging");
        }
    }

    private BluetoothAdapter getBluetoothAdapterSafely() {
        if (mBluetoothAdapter == null) {
            throw new RuntimeException("Bluetooth LE not supported by this device");
        }

        return mBluetoothAdapter;
    }

    private BluetoothAdapter.LeScanCallback getLeScanCallback() {
        if (mLeScanCallback == null) {
            mLeScanCallback =
                    new BluetoothAdapter.LeScanCallback() {
                        int nonStopCount = 0;

                        @Override
                        public void onLeScan(final BluetoothDevice device, final int rssi,
                                             final byte[] scanRecord) {
                            Log.v(TAG, device.getAddress() + ", rssi: " + rssi);
                            if (!mIsRanging) {
                                Log.v(TAG, "ignore scaned device because mIsRanging = false");
                                nonStopCount++;
                                if (nonStopCount % 10 == 0) {
                                    Log.v(TAG, "try to stop scanning");
                                    getBluetoothAdapterSafely().stopLeScan(mLeScanCallback);
                                }
                                return;
                            }
                            if (mRangingListener != null && checkUUID(scanRecord)) {
                                final Beacon beacon = new Beacon(device, rssi, scanRecord);
                                if (checkMatchScanningConfigs(beacon)) {
                                    new Handler(mAppContext.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mRangingListener.onBeaconDiscoverd(beacon);
                                        }
                                    });
                                }
                            }
                        }
                    };
        }
        return mLeScanCallback;
    }

    private boolean checkMatchScanningConfigs(Beacon beacon) {
        int rangingMode = mScanningConfigs.getMode();
        Integer rangingRssi = mScanningConfigs.getRssi();
        String beaconId = mScanningConfigs.getBeaconId();
        if (rangingMode == SCAN_MODE_WITH_DATA && !beacon.isWithData()) {
            return false;
        } else if (rangingRssi != null && beacon.getRssi() < rangingRssi) {
            return false;
        } else if (beaconId != null && !beaconId.equals(beacon.getId())) {
            return false;
        }

        return true;
    }

    private boolean checkUUID(byte[] scanRecord) {
        return ((scanRecord[2] & 0xff) == 0x46
                && (scanRecord[3] & 0xff) == 0xF8
                && (scanRecord[4] & 0xff) == 0x78
                && (scanRecord[5] & 0xff) == 0xBA
                && (scanRecord[6] & 0xff) == 0x1B
                && (scanRecord[7] & 0xff) == 0x17
                && (scanRecord[8] & 0xff) == 0x06
                && (scanRecord[9] & 0xff) == 0x83
                && (scanRecord[10] & 0xff) == 0x97
                && (scanRecord[11] & 0xff) == 0x45
                && (scanRecord[12] & 0xff) == 0x9E
                && (scanRecord[13] & 0xff) == 0xF4
                && (scanRecord[14] & 0xff) == 0x90
                && (scanRecord[15] & 0xff) == 0x4B
                && (scanRecord[16] & 0xff) == 0x69
                && (scanRecord[17] & 0xff) == 0xFB) ||
                ((scanRecord[5] & 0xff) == 0x46
                        && (scanRecord[6] & 0xff) == 0xF8
                        && (scanRecord[7] & 0xff) == 0x78
                        && (scanRecord[8] & 0xff) == 0xBA
                        && (scanRecord[9] & 0xff) == 0x1B
                        && (scanRecord[10] & 0xff) == 0x17
                        && (scanRecord[11] & 0xff) == 0x06
                        && (scanRecord[12] & 0xff) == 0x83
                        && (scanRecord[13] & 0xff) == 0x97
                        && (scanRecord[14] & 0xff) == 0x45
                        && (scanRecord[15] & 0xff) == 0x9E
                        && (scanRecord[16] & 0xff) == 0xF4
                        && (scanRecord[17] & 0xff) == 0x90
                        && (scanRecord[18] & 0xff) == 0x4B
                        && (scanRecord[19] & 0xff) == 0x69
                        && (scanRecord[20] & 0xff) == 0xFB);
    }

    @Override
    synchronized public void stopRanging() {
        Log.i(TAG, "stopRanging");
        mIsRanging = false;
        mBleRescanTimer.cancel();
        getBluetoothAdapterSafely().stopLeScan(mLeScanCallback);
    }

    @Override
    public boolean isRanging() {
        return mIsRanging;
    }

    @Override
    public void connect(final Beacon beacon, final ConnectListener connectListener) {
        if (mIsBeaconConnected) {
            disconnect();
        }
        mConnectListener = connectListener;
        if (!mIsBleServiceConnected) {
            mIsPreparing = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mIsPreparing) {
                        connect(beacon, connectListener);
                    }
                }
            }, PREPARING_RETRY_TIME);
            return;
        }
        mIsPreparing = false;
        if (beacon == null || beacon.getAddress() == null) {
            Log.e(TAG, "make sure beacon and address are not null");
            return;
        }
        Log.i(TAG, "connect");
        startConnectTimoutHandler();
        mBluetoothLeService.connect(beacon.getAddress());
    }

    private void startConnectTimoutHandler() {
        removeTimeoutCallback();
        mTimeoutHandler = new Handler(mAppContext.getMainLooper());
        mTimeoutHandler.postDelayed(mTimeoutTask, CONNECT_TIMEOUT);
    }

    private void removeTimeoutCallback() {
        if (mTimeoutHandler != null) {
            mTimeoutHandler.removeCallbacks(mTimeoutTask);
        }
    }

    @Override
    public void disconnect() {
        mIsPreparing = false;
        if (mIsBleServiceConnected) {
            Log.i(TAG, "disconnect");
            mBluetoothLeService.disconnect();
        }
    }

    private ServiceConnection getServiceConnection() {
        if (mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder service) {
                    mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                    if (!mBluetoothLeService.initialize()) {
                        Log.e(TAG, "Unable to initialize Bluetooth");
                    }
                    Log.i(TAG, "onServiceConnected");
                    mIsBleServiceConnected = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    Log.i(TAG, "onServiceConnected");
                    mIsBleServiceConnected = false;
                }
            };
        }

        return mServiceConnection;
    }

    private BroadcastReceiver getGattUpdateReceiver() {
        if (mGattUpdateReceiver == null) {
            mGattUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                        Log.i(TAG, "onBeaconConnected");
                        mIsBeaconConnected = true;
                        removeTimeoutCallback();
                        if (mConnectListener != null) {
                            mConnectListener.onBeaconConnected();
                        }
                    } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                        Log.i(TAG, "onBeaconDisconnected");
                        mIsBeaconConnected = false;
                        if (mConnectListener != null) {
                            mConnectListener.onBeaconDisconnected();
                        }
                    } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                        if (mBluetoothLeService.getSupportedGattServices() != null) {
                            boolean isFound = false;
                            for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
                                if (gattService.getCharacteristics() != null) {
                                    for (final BluetoothGattCharacteristic bluetoothGattCharacteristic : gattService.getCharacteristics()) {
                                        if (BluetoothLeService.UUID_LOOPD_COMMAND_CHAR.equals(bluetoothGattCharacteristic.getUuid())) {
                                            Log.i(TAG, "onDataServiceAvailable");
                                            if (mConnectListener != null) {
                                                mConnectListener.onDataServiceAvailable(bluetoothGattCharacteristic);
                                            }
                                            isFound = true;
                                            break;
                                        }
                                    }
                                    if (isFound) break;
                                } else {
                                    Log.w(TAG, "ACTION_GATT_SERVICES_DISCOVERED gattService.getCharacteristics() null");
                                }
                            }
                        } else {
                            Log.w(TAG, "ACTION_GATT_SERVICES_DISCOVERED mBluetoothLeService.getSupportedGattServices() null");
                        }
                    } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                        byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                        Log.d(TAG, "onContactExchangeDataReceived: " + GeneralUtil.bytesToHex(data));
                        if (mConnectListener != null) {
                            try {
                                String utf8String = new String(data, "UTF-8");
                                utf8String = utf8String.replaceAll("\\u0007", "");
                                mConnectListener.onDataReceived(utf8String.getBytes());
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                mConnectListener.onDataReceived(data);
                            }
                        }
                    }
                }
            };
        }

        return mGattUpdateReceiver;
    }

    public void writeCommand(BluetoothGattCharacteristic characteristic, byte[] values, byte[] param) {
        byte[] completeCommand = ByteBuffer.allocate(values.length + param.length).put(values).put(param).array();
        writeCommand(characteristic, completeCommand);
    }

    @Override
    public void writeCommand(BluetoothGattCharacteristic characteristic, byte[] values) {
        Log.d(TAG, "writeCommand: " + values.toString());
        if (mIsBleServiceConnected) {
            mBluetoothLeService.writeValue(characteristic, values);
        }
    }

    @Override
    public void writeCommand(String address, byte[] values) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothLeService.UUID_LOOPD_COMMAND_CHAR, 0x3E, 0);
        Class myClass;
        BluetoothDevice device = null;
        try {
            myClass = Class.forName("android.bluetooth.BluetoothDevice");
            Class[] argTypes = {String.class};
            Constructor constructor = myClass.getDeclaredConstructor(argTypes);
            Object[] arguments = {address};
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(arguments);
            device = (BluetoothDevice) instance;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

        BluetoothGattService gattService = null;
        try {
            myClass = Class.forName("android.bluetooth.BluetoothGattService");
            Class[] argTypes = {BluetoothDevice.class, UUID.class, int.class, int.class};
            Constructor constructor = myClass.getDeclaredConstructor(argTypes);
            Object[] arguments = {device, BluetoothLeService.UUID_LOOPD_SERVICE, 0, 0};
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(arguments);
            gattService = (BluetoothGattService) instance;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        gattService.addCharacteristic(characteristic);
        writeCommand(characteristic, values);
    }
}
