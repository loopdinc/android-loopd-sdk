# Loopd Beacon SDK for Android


## Installation
1. Create `libs` directory inside your project and copy there [aar](https://storage.googleapis.com/android-beacon-sdk/loopd-beacon-1.0.0.aar) file
2. Add to build.gradle:

```groovy
repositories {
    flatDir {
        dirs 'libs'
    }
}
```

```groovy
dependencies {
    ...
    compile(name:'loopd-beacon-1.0.0', ext:'aar')
    ...
}
```

## Usage
### Permissions
Declare the Bluetooth permission(s) in your application manifest file:
```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```
If you want to declare that your app is available to BLE-capable devices only, include the following in your app's manifest:
```xml
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
```

### BeaconManager
`BeaconManager` is a basic manager than help developer to control Loopd Badge.
```java
mBeaconManager = new BeaconManager(getApplicationContext());
```
You need to release instance after use. For example in onDestroy method
```java
mBeaconManager.release();
```

### Ranging
```java
mBeaconManager.startRanging(ScanningConfigs);
```
```java
mBeaconManager.stopRanging();
```
You can instantiate ScanningConfigs with parameters: mode, rssi, beaconId
```java
new ScanningConfigs(ScanningConfigs.SCAN_MODE_ALL, null, null);
```

### Connect
```java
mBeaconManager.connect(Beacon, ConnectListener);
```
```java
mBeaconManager.disconnect();
```

### Write command
```java
// you can replace second parameter to other commands
mBeaconManager.writeCommand(BluetoothGattCharacteristic, BeaconManager.COMMAND_TURN_ON_BOTH_LEDS);
```

## Commands
|name| command | action  | 
|:-------:|:-------:|:-------:|
|COMMAND_TURN_OFF_BOTH_LEDS| 0x00 | Switch off both LEDs |
|COMMAND_TURN_ON_RED_LED| 0x0F | Switch on red LED |
|COMMAND_TURN_ON_YELLOW_LED| 0xF0 | Switch on yellow LED |
|COMMAND_TURN_ON_BOTH_LEDS| 0xFF | Switch on both LEDs |
|COMMAND_CHANGE_ADVERTISEMENT_FREQUENCY| 0xA0 | Change the advertisement Frequency |
|COMMAND_FREE_SPACE_LEFT| 0x14 | Get the amount of free space left |
|COMMAND_CHANGE_TRANSMISSION_POWER| 0x10 | Change Transmission Power |
|COMMAND_FORCE_DISCONNECT| 0x11 | Force the device to disconnect |
|COMMAND_READ_DATA| 0x07 | Read data |

## Author

Evan, evan.lin@getloopd.com

## License

android-loopd-sdk is available under the MIT license. See the LICENSE file for more info.
