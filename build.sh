#!/bin/bash
# Builds room-home.apk with the Android SDK in ~/android-sdk (no Gradle).
set -euo pipefail
cd "$(dirname "$0")"
SDK=$HOME/android-sdk; BT=$SDK/build-tools/34.0.0; JAR=$SDK/platforms/android-34/android.jar
rm -rf build && mkdir -p build/classes build/dex
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link --auto-add-overlay -o build/app.unsigned.apk -R build/res.zip -I "$JAR" --manifest AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 34
javac --release 8 -Xlint:-options -cp "$JAR" -d build/classes src/com/roomremote/launcher/*.java
$BT/d8 --lib "$JAR" --min-api 26 --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q -u ../app.unsigned.apk classes.dex)
$BT/zipalign -f 4 build/app.unsigned.apk build/app.aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias key -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Room Home" >/dev/null 2>&1
$BT/apksigner sign --ks debug.keystore --ks-pass pass:android --out room-home.apk build/app.aligned.apk
ls -la room-home.apk
