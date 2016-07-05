# Loopd Beacon SDK for Android

## Description
The Loopd Beacon SDK provides apis to interact with the Loopd Beacons from Android/iOS devices, and includes ranging, connecting, and writing and reading data between Loopd Beacons.

## Installation
1. Create `libs` directory inside your project and copy there [aar](https://storage.googleapis.com/android-beacon-sdk/loopd-beacon-1.0.1.aar) file
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
    compile(name:'loopd-beacon-1.0.4', ext:'aar')
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
`BeaconManager` is a basic manager helping developers to control Loopd Beacons.
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
|COMMAND_TURN_ON_BOTH_LEDS| 0xFF | Switch on both LEDs |
|COMMAND_FORCE_DISCONNECT| 0x11 | Disconnect Connection |
|COMMAND_CHANGE_ADVERTISEMENT_FREQUENCY| 0xA0 | Change advertisement frequency |
|COMMAND_READ_DATA| 0x07 | Read data |

## Author

Evan Lin, evan.lin@getloopd.com

## License

android-loopd-sdk is available under the MIT license. See the LICENSE file for more info.
