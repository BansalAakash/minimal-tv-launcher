#!/usr/bin/env bash
# Removes Room Home and restores the HDMI-CEC defaults. Usage: scripts/uninstall.sh <tv-ip>
set -euo pipefail
TV="${1:?Usage: $0 <tv-ip>}"
A() { adb -s "$TV:5555" "$@"; }
adb connect "$TV:5555" >/dev/null
A shell settings put secure enabled_accessibility_services ''
A shell cmd hdmi_control cec_setting set tv_wake_on_one_touch_play 0 || true
A shell cmd hdmi_control cec_setting set routing_control 0 || true
A shell cmd hdmi_control cec_setting set power_state_change_on_active_source_lost standby_now || true
A uninstall com.roomremote.launcher
echo "Removed. The stock home screen is back."
