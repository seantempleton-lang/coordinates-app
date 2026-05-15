# CoordSnap

Native Android/Kotlin app that requests precise location, converts coordinates to WGS84 or NZTM2000 / EPSG:2193, copies a ready-to-share text message to the clipboard, and supports Android's share sheet.

## Build in Android Studio

1. Open this folder in Android Studio.
2. Let Gradle sync and download dependencies.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. The debug APK will usually be created at:

```text
%LOCALAPPDATA%\CoordSnapBuild\app\outputs\apk\debug\app-debug.apk
```

## Build from a terminal

```text
gradlew.bat :app:assembleDebug
```

## Notes

- Requires Google Play Services location dependency.
- Requests ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION.
- NZTM2000 conversion is implemented locally/offline.
- The conversion is suitable for practical GPS sharing, not cadastral/survey-grade datum transformation.
