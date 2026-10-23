#!/usr/bin/env bash
# Installs Room Home on an Android TV over Wi-Fi ADB and makes it the home screen.
# Usage: scripts/install.sh [tv-ip] [path/to/room-home.apk]
# With no IP it finds the TV on your Wi-Fi by itself. Needs `adb` on your PATH. On the TV: Settings > Device
# Preferences > About, tap "Build" 7 times, then Developer options > turn on USB debugging (this also enables ADB
# over the network). Accept the prompt on the TV the first time.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
TV=""; APK="$HERE/../room-home.apk"
for a in "$@"; do
  case "$a" in
    *.apk) APK="$a" ;;
    *.*.*.*) TV="$a" ;;
  esac
done
command -v adb >/dev/null || { echo "adb not found. Install Android platform-tools: https://developer.android.com/tools/releases/platform-tools"; exit 1; }
[ -f "$APK" ] || { echo "No APK at $APK. Download room-home.apk from the Releases page, or build it with ./build.sh"; exit 1; }

if [ -z "$TV" ]; then
  echo "Looking for your TV on this Wi-Fi ..."
  CANDS="$("$HERE/find-tv.sh")" || exit 1
  if [ "$(printf '%s\n' "$CANDS" | wc -l | tr -d ' ')" -eq 1 ]; then
    TV="$CANDS"
  else
    for c in $CANDS; do
      adb connect "$c:5555" >/dev/null 2>&1 || true
      case "$(adb -s "$c:5555" shell getprop ro.build.characteristics 2>/dev/null || true)" in *tv*) TV="$c"; break ;; esac
    done
    [ -n "$TV" ] || { echo "Several devices answered on this Wi-Fi: $(echo $CANDS). Run: $0 <the TV's ip>"; exit 1; }
  fi
  echo "Found the TV at $TV"
fi

PKG=com.roomremote.launcher
SVC="$PKG/$PKG.HomeWatcher"
A() { adb -s "$TV:5555" "$@"; }

echo "Connecting to $TV ..."
adb connect "$TV:5555" >/dev/null
A get-state >/dev/null || { echo "Could not reach the TV. Is ADB debugging on, and did you accept the prompt on the TV screen?"; exit 1; }

echo "Installing ..."
A install -r "$APK"

echo "Granting the permission it uses to keep its watcher enabled ..."
A shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS

echo "Turning on the accessibility watcher (this is what returns you to Room Home when you press Home) ..."
A shell settings put secure enabled_accessibility_services "$SVC"
A shell settings put secure accessibility_enabled 1

echo "Setting HDMI-CEC so a console waking up can turn the TV on and switch to it ..."
A shell cmd hdmi_control cec_setting set tv_wake_on_one_touch_play 1 || true
A shell cmd hdmi_control cec_setting set routing_control 1 || true
A shell cmd hdmi_control cec_setting set power_state_change_on_active_source_lost none || true

echo "Starting Room Home ..."
A shell am start -n "$PKG/.MainActivity" >/dev/null
echo "Done. Press Home on the remote to check it opens Room Home."
echo "To undo: scripts/uninstall.sh $TV"
