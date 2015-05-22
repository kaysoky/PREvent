# PREvent Companion App (Android)

## To install:
* Install [Android SDK](http://developer.android.com/sdk/index.html)
* Then run:
```
android update project --target "android-18" --path . --subprojects
```

## To compile:
```
# Recommended if anything in the "res" folder has changed
ant clean

ant debug
```

## To install:
```
adb install -r bin/PREventCompanion-debug.apk
```
