package com.loopd.sdk.beacon.ota;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

public class OtaUpgrader {
    public static final UUID UUID_UPGRADE_SERVICE          = UUID.fromString("9e5d1e47-5c13-43a0-8635-82ad38a1386f");
    public static final UUID UUID_UPGRADE_CONTROL_POINT    = UUID.fromString("e3dd50bf-f7a7-4e99-838e-570a086c666b");
    public static final UUID UUID_UPGRADE_DATA             = UUID.fromString("92e86c7a-d961-4091-b74f-2409e72efe36");
    public static final UUID UUID_UPGRADE_APP_INFO         = UUID.fromString("347f7608-2e2d-47eb-913b-75d4edc4de3b");
    public static final UUID UUID_CLIENT_CONFIGURATION     = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int STATUS_OK					    = 0;
    public static final int STATUS_UNSUPPORTED_COMMAND	    = 1;
    public static final int STATUS_ILLEGAL_STATE		    = 2;
    public static final int STATUS_VERIFICATION_FAILED	    = 3;
    public static final int STATUS_INVALID_IMAGE		    = 4;
    public static final int STATUS_INVALID_IMAGE_SIZE	    = 5;
    public static final int STATUS_MORE_DATA			    = 6;
    public static final int STATUS_INVALID_APPID		    = 7;
    public static final int STATUS_INVALID_VERSION		    = 8;

    public static final int STATUS_DISCONNECT               = 9;
    public static final int STATUS_ABORT				    = 10;
    public static final int STATUS_TIMEOUT                  = 11;
    public static final int STATUS_INVALID_DEVICE_ADDRESS   = 12;
    public static final int STATUS_INVALID_FILE_PATH        = 13;
    public static final int STATUS_IO_ERROR				    = 14;
    public static final int STATUS_NOT_BOND				    = 15;
    public static final int STATUS_UNKNOWN				    = 0xFF;

    public static final int COMMAND_PREPARE_DOWNLOAD        = 1;
    public static final int COMMAND_DOWNLOAD                = 2;
    public static final int COMMAND_VERIFY                  = 3;
    public static final int COMMAND_FINISH                  = 4;
    public static final int COMMAND_GET_STATUS              = 5;
    public static final int COMMAND_CLEAR_STATUS            = 6;
    public static final int COMMAND_ABORT                   = 7;

    private static final String TAG = "OtaUpgrader";
    private static final boolean DEBUG      = true;
    private static final boolean DEBUG_DATA = (DEBUG & false);
    private static final boolean DEBUG_CHAR = (DEBUG & false);

    private static final int DATE_BLOCK_SIZE = 20;
    private static final byte[] ENABLE_NOTIFICATION_VALUE = {0x03, 0x00};

    private Context mContext;
    private Callback mCallback;
    private File mFile;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mControlPointCharacteristic;
    private BluetoothGattCharacteristic mDataCharacteristic;

    private StateMachine mStateMachine;
   
    private String mDeviceAddress;
    private int mPatchFileResId;
    private int mPatchSize;
    private int mOffset;
    private CRC32 mCrc32;

    private BufferedInputStream mInputStream;

    private static final int EVENT_CONNECTED                = 1;
    private static final int EVENT_DISCONNECTED             = 2;
    private static final int EVENT_NOTIFY_SENT              = 3;
    private static final int EVENT_DATA_SENT                = 4;
    private static final int EVENT_COMMAND_SENT             = 5;
    private static final int EVENT_ABORT                    = 6;
    private static final int EVENT_TIMEOUT                  = 7;

    public interface Callback {
        public void onProgress(int realSize, int precent);
        public void onFinish(int status);
    }

    public OtaUpgrader(Context context) {
        if (context instanceof Activity) {
            mContext = context;
        } else {
            throw(new RuntimeException("context has too be an Activity"));
        }
    }

    public void setListener(Callback callback) {
        mCallback = callback;
    }

    public void start(String deviceAddress, File file) {
        mDeviceAddress = deviceAddress;
        mFile = file;
        mPatchFileResId = 0;
        start();
    }

    public void start(String deviceAddress, int patchFileResId) {
        mDeviceAddress = deviceAddress;
        mFile = null;
        mPatchFileResId = patchFileResId;
        start();
    }

    private void start() {
        if (DEBUG) {
            Log.d(TAG, "start()");
        }

        if (mStateMachine == null) {
            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();

            Looper looper = handlerThread.getLooper();
            mStateMachine = new StateMachine(looper);
            mStateMachine.start();
        }
    }

    public void stop() {
        if (DEBUG) {
            Log.d(TAG, "stop()");
        }

        if (mStateMachine != null) {
            mStateMachine.stop();
        }
    }

    public int getPatchSize(int fileResId) {
        InputStream ins = mContext.getResources().openRawResource(fileResId);
        int sizeOfInputStram = 0; // Get the size of the stream
        try {
            sizeOfInputStram = ins.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sizeOfInputStram;
    }

    public int getPatchSize(File file) {
        return  Integer.parseInt(String.valueOf(file.length()));
    }

    private int connect() {
        if (DEBUG) {
            Log.d(TAG, "connect()");
        }

        BluetoothManager bluetoothManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "connect(), bluetoothManager = null");
            return STATUS_UNKNOWN;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "connect(), bluetoothAdapter = null");
            return STATUS_UNKNOWN;
        }

        if (DEBUG) {
            Log.d(TAG, "connect(), mDeviceAddress = " + mDeviceAddress);
        }

        if (mDeviceAddress == null) {
            return STATUS_INVALID_DEVICE_ADDRESS;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);

        if (device == null) {
            Log.e(TAG, "connect(), device = null");
            return STATUS_INVALID_DEVICE_ADDRESS;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Method connectGattMethod = null;

            try {
                connectGattMethod = device.getClass().getMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            try {
                if (connectGattMethod != null) {
                    mBluetoothGatt = (BluetoothGatt) connectGattMethod.invoke(device, mContext, false, mGattCallback, 2);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            mBluetoothGatt = device.connectGatt(mContext, true, mGattCallback);
        }

        if (mBluetoothGatt == null) {
            Log.e(TAG, "connect(), mBluetoothGatt = null");
            return STATUS_UNKNOWN;
        }

        return STATUS_OK;
    }

    private int handleConnected() {
        if (DEBUG) {
            Log.d(TAG, "handleConnected()");
        }

        BluetoothGattService gattService = mBluetoothGatt.getService(UUID_UPGRADE_SERVICE);

        if (gattService == null) {
            Log.e(TAG, "handleConnected(), gattService = null");
            return STATUS_UNKNOWN;
        }

        mControlPointCharacteristic = gattService.getCharacteristic(UUID_UPGRADE_CONTROL_POINT);
        mDataCharacteristic = gattService.getCharacteristic(UUID_UPGRADE_DATA);

        if ((mControlPointCharacteristic == null) || 
            (mDataCharacteristic == null)) {
            Log.e(TAG, "handleConnected(), mControlPointCharacteristic = " + mControlPointCharacteristic);
            Log.e(TAG, "handleConnected(), mDataCharacteristic = " + mDataCharacteristic);
            return STATUS_UNKNOWN;
        }

        return STATUS_OK;
    }

    private void disconnect() {
        if (DEBUG) {
            Log.d(TAG, "disconnect()");
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        mControlPointCharacteristic = null;
        mDataCharacteristic = null;
    }

    private int enableNotification() {
        if (DEBUG) {
            Log.d(TAG, "enableNotification()");
        }

        int status = STATUS_UNKNOWN;
        boolean result = mBluetoothGatt.setCharacteristicNotification(mControlPointCharacteristic, true);

        if (result) {
            BluetoothGattDescriptor clientConfig = mControlPointCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);

            if ((clientConfig != null) && 
                clientConfig.setValue(ENABLE_NOTIFICATION_VALUE) &&
                mBluetoothGatt.writeDescriptor(clientConfig)) {
                status = STATUS_OK;
            }
        }
        
        return status;
    }

    private int sendCommand(int command) {
        byte value[] = {(byte)(command & 0xFF)};

        return sendCommand(value);
    }

    private int sendCommand(int command, short param) {
        byte value[] = {(byte)(command & 0xFF), (byte)(param & 0xFF),	(byte)((param >> 8) & 0xFF)};

        return sendCommand(value);
    }
	
    private int sendCommand(int command, int param) {
        byte value[] = {(byte)(command & 0xFF), (byte)(param & 0xFF), (byte)((param >> 8) & 0xFF), (byte)((param >> 16) & 0xFF), (byte)((param >> 24) & 0xFF)};

        return sendCommand(value);
    }

    private int sendCommand(byte[] value) {
        if (DEBUG) {
            Log.d(TAG, "sendCommand(), value = " + Arrays.toString(value));
        }

        int status = STATUS_UNKNOWN;

        if (mControlPointCharacteristic.setValue(value) && 
            mBluetoothGatt.writeCharacteristic(mControlPointCharacteristic)) {
            status = STATUS_OK;
        }

        return status;
    }

    private int sendUpgradeData(byte[] value) {
        if (DEBUG_DATA) {
            Log.d(TAG, "sendUpgradeData(), value = " + Arrays.toString(value));
        }

        int status = STATUS_UNKNOWN;

        if (mDataCharacteristic.setValue(value) &&
            mBluetoothGatt.writeCharacteristic(mDataCharacteristic)) {
            status = STATUS_OK;
        }

        return status;
    }

    private int initDataTransfer() {
        if (DEBUG) {
            Log.d(TAG, "initDataTransfer(), mPatchFileResId = " + mPatchFileResId);
        }

	    int status = STATUS_OK;

        if (mPatchFileResId == 0) {
            try {
                mInputStream = new BufferedInputStream(new FileInputStream(mFile));
            } catch (Exception e) {
                Log.e(TAG, "initDataTransfer(), e = " + e);
                status = STATUS_INVALID_FILE_PATH;
            }
        } else {
            try {
                InputStream ins = mContext.getResources().openRawResource(mPatchFileResId);
                mInputStream = new BufferedInputStream(ins);
            } catch (Exception e) {
                Log.e(TAG, "initDataTransfer(), e = " + e);
                status = STATUS_INVALID_FILE_PATH;
            }
        }

        mOffset = 0;
        mCrc32 = new CRC32();

        return status;
    }

    private void deinitDataTransfer() {
        if (DEBUG) {
            Log.d(TAG, "deinitDataTransfer()");
        }

        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
        } finally {
            mInputStream = null;
        }
    }

    private int transferDataBlock() {
        if (DEBUG) {
            Log.d(TAG, "transferDataBlock()");
        }

        int ret = STATUS_OK;

        try {
            int length = mPatchSize - mOffset;
            if (length > DATE_BLOCK_SIZE) {
                length = DATE_BLOCK_SIZE;
            }

            byte[] data = new byte[length];

            mInputStream.read(data, 0, length);
        
            mCrc32.update(data, length);
            mOffset += length;

            ret = sendUpgradeData(data);
        } catch (IOException e) {
            Log.d(TAG, "transferDataBlock(), e = " + e);
            ret = STATUS_IO_ERROR;
        }

        return ret;
    }

    private int convertGattStatus(int gattStatus) {
        int status;

        switch (gattStatus) {
            case BluetoothGatt.GATT_SUCCESS:
            status = STATUS_OK;
            break;

            default:
            status = STATUS_UNKNOWN;
            break;
        }

        return status;
    }

    private void handleProgress() {
        if (DEBUG) {
            Log.d(TAG, "handleProgress(), mOffset = " + mOffset + ", mPatchSize = " + mPatchSize);
        }

        if (mCallback != null) {
            final int precent = (mOffset * 100) / mPatchSize;

            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.onProgress(mOffset, precent);
                }
            });
        }
    }

    private void handleFinish(final int status) {
        if (DEBUG) {
            Log.d(TAG, "handleFinish(), status = " + status);
        }

        disconnect();

        if (mCallback != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.onFinish(status);
                }
            });
        }

        mStateMachine = null;
    }

    private final class CRC32 {
        private static final int POLYNOMIAL = 0x04C11DB7;
        private static final int WIDTH      = 32;
        private static final int MSB_BIT    = (1 << (WIDTH - 1));
        private static final int INITIAL_REMAINDER = 0xFFFFFFFF;
        private static final int FINAL_XOR_VALUE = 0xFFFFFFFF;

        private int mCrc32;

        public CRC32() {
            mCrc32 = INITIAL_REMAINDER;
        }

        public void update(byte[] buffer, int nBytes) {            
            // Perform modulo-2 division, a byte at a time.
            for (int i = 0; i < nBytes; i++) {
                // Bring the next byte into the crc32.
                mCrc32 ^= (reflectData(buffer[i]) << (WIDTH - 8));

                // Perform modulo-2 division, a bit at a time.
                for (int j = 8; j > 0; j--) {
                    // Try to divide the current data bit.
                    if ((mCrc32 & MSB_BIT) == 0) {
                        mCrc32 = (mCrc32 << 1);
                    }
                    else {
                        mCrc32 = (mCrc32 << 1) ^ POLYNOMIAL;
                    }
                }
            }
        }

        public int getValue() {
            int crc32 = (reflectReminder(mCrc32) ^ FINAL_XOR_VALUE);
        
            return crc32;
        }

        private int reflectData(int data) {
            return reflect(data, 8);
        }

        private int reflectReminder(int data) {
            return reflect(data, WIDTH);
        }

        private int reflect(int data, int nBits) {
            int reflection = 0x00000000;
        
            // Reflect the data about the center bit.
            for (int i = 0; i < nBits; i++) {
                // If the LSB bit is set, set the reflection of it.
                if ((data & 0x01) == 0x01) {
                    reflection |= (1 << ((nBits - 1) - i));
                }

                data = (data >> 1);
            }

            return reflection;
        }
    }

    private final class StateMachine extends Handler {
        private final State STATE_IDLE                  = new IdleState();
        private final State STATE_CONNECTING            = new ConnectingState();
        private final State STATE_ENABLE_NOTIFICATION   = new EnableNotificationState();
        private final State STATE_PREPRARE_DOWNLOAD     = new PrepareDownloadState();
        private final State STATE_READY_FOR_DOWNLOAD    = new ReadyForDownloadState();
        private final State STATE_DATA_TRANSFER         = new DataTransferState();
        private final State STATE_VERIFICATION          = new VerificationState();
        private final State STATE_FINISH                = new FinishState();

        private static final int MSG_TRANSITION_TO      = 1;
        private static final int MSG_PROCESS_EVENT      = 2;	
        private static final int MSG_TIMEOUT            = 3;	
        private static final int MSG_QUIT               = 4;

        private static final int STATE_TIMEOUT          = 10 * 1000;

        private boolean mIsRunning = false;
        private State mCurrState = STATE_IDLE;

        private class State {
            private int mStatus;
            
            protected State() {
            }

            public void setStatus(int status) {
                mStatus = status;
            }

            public int getStatus() {
                return mStatus;
            }

            public void enter() {
                startTimeout();
            }

            public void exit() {
                stopTimeout();
            }

            public void processEvent(int event, int status) {                
                switch (event) {
                    case EVENT_DISCONNECTED:                    
                    case EVENT_TIMEOUT:
                    case EVENT_ABORT:
                        transitionTo(STATE_FINISH, status);
                        break;

                    default:
                        break;
                }
            }
        }

        private final class IdleState extends State {
            public void enter() {
                quit();
            }

            @Override
            public void processEvent(int event, int status) {
                //Do nothing in Idle state
            }

            @Override
            public void exit() {
                //Do nothing in Idle state
            }

            @Override
            public String toString() {
                return "IdleState";
            }
        }

        private final class ConnectingState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = connect();

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_CONNECTED:
                    {
                        if (status == STATUS_OK) {
                            status = handleConnected();
                        }

                        if (status == STATUS_OK) {
                            transitionTo(STATE_ENABLE_NOTIFICATION, status);
                        } else {
                            transitionTo(STATE_FINISH, status);
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }
            }

            @Override
            public String toString() {
                return "ConnectingState";
            }
        }
		
        private final class EnableNotificationState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = enableNotification();

            	if (status != STATUS_OK) {
            		transitionTo(STATE_FINISH, status);
            	}
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_NOTIFY_SENT:
                    {
                        if (status == STATUS_OK) {
                            transitionTo(STATE_PREPRARE_DOWNLOAD, status);
                        } else {
                            transitionTo(STATE_FINISH, status);
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }
            }

            @Override
            public String toString() {
                return "EnableNotificationState";
            }
        }
		
        private final class PrepareDownloadState extends State {
            @Override
            public void enter() {
                super.enter();

                if (mPatchFileResId == 0) {
                    mPatchSize = getPatchSize(mFile);
                } else {
                    mPatchSize =  getPatchSize(mPatchFileResId);
                }
                int status = sendCommand(COMMAND_PREPARE_DOWNLOAD, (short)mPatchSize);

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_COMMAND_SENT:
                    {
                        if (status == STATUS_OK) {
                            transitionTo(STATE_READY_FOR_DOWNLOAD, status);
                        } else {
                            transitionTo(STATE_FINISH, status);							
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }		
            }

            @Override
            public String toString() {
                return "PrepareDownloadState";
            }
        }
		
        private final class ReadyForDownloadState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = sendCommand(COMMAND_DOWNLOAD);

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_COMMAND_SENT:
                    {
                        if (status == STATUS_OK) {
                            transitionTo(STATE_DATA_TRANSFER, status);
                        } else {
                            transitionTo(STATE_FINISH, status);
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }		
            }

            @Override
            public String toString() {
                return "ReadyForDownloadState";
            }
        }
		
        private final class DataTransferState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = initDataTransfer();
            
                if (status == STATUS_OK) {
                    status = transferDataBlock();
                }

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);                         
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_DATA_SENT:
                    {
                        stopTimeout();

                        if (status == STATUS_OK) {
                            if (mPatchSize == mOffset) {
                                transitionTo(STATE_VERIFICATION, status);
                            } else {
                                startTimeout();
                                status = transferDataBlock();
                            }
                        }

                        if (status == STATUS_OK) {
                            handleProgress();
                        } else {
                            transitionTo(STATE_FINISH, status);							
                        }
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }	
            }

            @Override
            public void exit() {
                deinitDataTransfer();
            }

            @Override
            public String toString() {
                return "DataTransferState";
            }
        }
		
        private final class VerificationState extends State {
            @Override
            public void enter() {
                super.enter();

                int status = sendCommand(COMMAND_VERIFY, mCrc32.getValue());

                if (status != STATUS_OK) {
                    transitionTo(STATE_FINISH, status);                    
                }
            }

            @Override
            public void processEvent(int event, int status) {
                switch (event) {
                    case EVENT_COMMAND_SENT:
                    {
                        transitionTo(STATE_FINISH, status);
                        break;
                    }

                    default:
                        super.processEvent(event, status);
                        break;
                }	
            }

            @Override
            public String toString() {
                return "VerificationState";
            }
        }
		
        private final class FinishState extends State {
            @Override
            public void enter() {
                transitionTo(STATE_IDLE, STATUS_OK);
            }

            @Override
            public void exit() {
                int status = getStatus();

                handleFinish(status);
            }

            @Override
            public String toString() {
                return "FinishState";
            }
        }

        public StateMachine(Looper looper) {
            super(looper);
        }

        public void start() {
            if (!mIsRunning) {
            	mIsRunning = true;
                transitionTo(STATE_CONNECTING, STATUS_OK);
            }
        }

        public void stop() {
            postEvent(EVENT_ABORT, STATUS_ABORT);
        }

        public void quit() {
            Message msg = obtainMessage(MSG_QUIT);
            sendMessage(msg);
        }
	
        public void postEvent(int event, int status) {
            Message msg = obtainMessage(MSG_PROCESS_EVENT, event, status);
            sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TRANSITION_TO:
                {
                    State state = (State)msg.obj;
                    int status = msg.arg1;

                    handleTransitionTo(state, status);
                    break;
                }

                case MSG_PROCESS_EVENT:
                {
                    handleProcessEvent(msg.arg1, msg.arg2);
                    break;
                }

                case MSG_TIMEOUT:
                {
                    handleProcessEvent(EVENT_TIMEOUT, STATUS_TIMEOUT);
                    break;
                }

                case MSG_QUIT:
                {
                	handleQuit();
                    break;
                }

                default:
                    break;
            }		
        }

        private void transitionTo(State state, int status) {
            if (DEBUG) {
                Log.d(TAG, "transitionTo state = " + state);
            }

            Message msg = obtainMessage(MSG_TRANSITION_TO);
            msg.arg1 = status;
            msg.obj = state;
            sendMessageDelayed(msg, 200);
        }

        private void startTimeout() {
            Message msg = obtainMessage(MSG_TIMEOUT);
            sendMessageDelayed(msg, STATE_TIMEOUT);
        }

        private void stopTimeout() {
            removeMessages(MSG_TIMEOUT);
        }

        private void handleTransitionTo(State state, int status) {
            if (DEBUG) {
                Log.d(TAG, "handleTransitionTo mCurrState = " + mCurrState + ", state = " + state);
            }

            mCurrState.exit();
            mCurrState = state;
            mCurrState.setStatus(status);
            mCurrState.enter();
        }

        private void handleProcessEvent(int event, int status) {
            if (DEBUG) {
                Log.d(TAG, "handleProcessEvent mCurrState = " + mCurrState + ", event = " + event + ", status = " + status);
            }

            if (mCurrState != null) {
                mCurrState.processEvent(event, status);
            }
        }
        
        private void handleQuit() {
            getLooper().quit();
            mStateMachine = null;
        	mIsRunning = false;
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (DEBUG) {
                Log.d(TAG, "onConnectionStateChange(), status = " + status + ", newState = " + newState);
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mStateMachine.postEvent(EVENT_DISCONNECTED, STATUS_DISCONNECT);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (DEBUG) {
                Log.d(TAG, "onServicesDiscovered(), status = " + status);
            }

            int ret = convertGattStatus(status);
            mStateMachine.postEvent(EVENT_CONNECTED, ret);
        }

        @Override
        public void onCharacteristicRead(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        int status) {
        }

        @Override
        public void onCharacteristicChanged(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();

            if (DEBUG_CHAR) {
                Log.d(TAG, "onCharacteristicChanged(), uuid = " + uuid);
            }

            if (uuid.equals(UUID_UPGRADE_CONTROL_POINT)) {
                byte[] value = characteristic.getValue();
                int status = value[0];
                mStateMachine.postEvent(EVENT_COMMAND_SENT, status);
            }
        }

        @Override
        public void onCharacteristicWrite(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        int status) {
            UUID uuid = characteristic.getUuid();

            if (DEBUG_CHAR) {
                Log.d(TAG, "onCharacteristicWrite(), uuid = " + uuid + ", status = " + status);
            }

            if (uuid.equals(UUID_UPGRADE_DATA)) {
                int ret = convertGattStatus(status);
                mStateMachine.postEvent(EVENT_DATA_SENT, ret);
            } 
        }

        @Override
        public void onDescriptorWrite(
                        BluetoothGatt gatt,
                        BluetoothGattDescriptor descriptor,
                        int status) {
            UUID uuid = descriptor.getUuid();

            if (DEBUG_CHAR) {
                Log.d(TAG, "onDescriptorWrite(), uuid = " + uuid + ", status = " + status);
            }

            if (uuid.equals(UUID_CLIENT_CONFIGURATION)) {
                int ret = convertGattStatus(status);
                mStateMachine.postEvent(EVENT_NOTIFY_SENT, ret);
            }
        }
    };
}
