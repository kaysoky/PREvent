# PREvent Companion App (Android)

## To install:
* Install [Android SDK](http://developer.android.com/sdk/index.html)
* Open the Android SDK Manager
* Install packages from "Android 4.1.2 (API 16)"
* Run: ` android update project --target "android-16" --path . --subprojects `

## Dependencies
The `.jar` files found in the `libs/` folder were not trivial to obtain.  Since Android API-level 18, all these OEM BLE libraries have become deprecated.  Unfortunately, one of our phones is an HTC One.
### HTC BLE Library
* Download [HTC OpenSense SDK for BLE](https://www.htcdev.com/devcenter/opensense-sdk/bluetooth-smart/htc-ble-installation/)
* Extract HTC SDK into the Android SDK's "add-ons" folder.  This is part of the installation process of the HTC SDK.  However, it does not get recognized as a build target.  
* Copy `addon-htc_ble_api-htc-19/libs/com.htc.android.bluetooth.le.jar` into the project's `libs/` folder
### Broadcom BLE API
* Download [Broadcom BLE clone](https://code.google.com/r/naranjomanuel-opensource-broadcom-ble/).  The original project no longer exists.
* Install [Android NDK](http://developer.android.com/tools/sdk/ndk/index.html)
* TODO

## To compile:
```
ant debug
```

## To install: 
```
adb install bin/PREventCompanion-debug.apk
```
