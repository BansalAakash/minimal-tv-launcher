# Room Home: instructions for AI assistants

Read this first. It is written so you can set up, change and debug this project for a user who may know nothing about it. Follow it step by step, run the verification command after each step, and only ask the user for what you cannot find or do yourself (marked **ASK**).

## What this is

Room Home is an Android TV launcher (home screen), a single small APK, package `com.roomremote.launcher`. It shows a 3 x 2 grid of app tiles; a lamp, a sun and a moon on India time; a spider that pulls the lamp's cord to switch light and dim themes; and it handles a game console waking the TV. It is built without Gradle using the Android SDK command-line tools.

The user wants it on their own TV. They should not have to understand ADB. You do the work; keep your messages short and plain.

## Prerequisites (check, install if missing)

1. A computer on the same Wi-Fi as the TV, with `adb` on PATH. Check: `adb version`. If missing, install Android platform-tools: https://developer.android.com/tools/releases/platform-tools
2. **Find the TV yourself.** The computer and the TV are on the same Wi-Fi, so run `scripts/find-tv.sh`: it scans the local network and prints the address of every device with ADB turned on (the TV, once step 3 is done). One address: that is the TV. Several: `adb connect <ip>:5555` and check `adb -s <ip>:5555 shell getprop ro.build.characteristics` contains `tv`. Only **ASK** the user for the address if nothing is found after they have done step 3.
3. The user turns on ADB on the TV: Settings > Device Preferences > About, tap **Build** 7 times, then Developer options > **USB debugging** on. They must accept the "Allow USB debugging?" prompt on the TV the first time. Tell them exactly this; you cannot do it for them.
4. An APK: either download `room-home.apk` from the repository's Releases page, or build it (see "Build" below).

## Install

Preferred: run the script. With no address it finds the TV by itself.

```bash
scripts/install.sh room-home.apk          # or: scripts/install.sh <tv-ip> room-home.apk
```

What it does, in case you need to do it by hand (all via `adb -s <tv-ip>:5555`):

```bash
adb connect <tv-ip>:5555
adb -s <tv-ip>:5555 install -r room-home.apk
adb -s <tv-ip>:5555 shell pm grant com.roomremote.launcher android.permission.WRITE_SECURE_SETTINGS
adb -s <tv-ip>:5555 shell settings put secure enabled_accessibility_services com.roomremote.launcher/com.roomremote.launcher.HomeWatcher
adb -s <tv-ip>:5555 shell settings put secure accessibility_enabled 1
adb -s <tv-ip>:5555 shell cmd hdmi_control cec_setting set tv_wake_on_one_touch_play 1
adb -s <tv-ip>:5555 shell cmd hdmi_control cec_setting set routing_control 1
adb -s <tv-ip>:5555 shell cmd hdmi_control cec_setting set power_state_change_on_active_source_lost none
adb -s <tv-ip>:5555 shell am start -n com.roomremote.launcher/.MainActivity
```

**Verify:** `adb -s <tv-ip>:5555 shell dumpsys activity activities | grep topResumedActivity` should show `com.roomremote.launcher/.MainActivity`, and `adb -s <tv-ip>:5555 shell dumpsys accessibility | grep -c "roomremote.launcher"` should print `1` or more. Then ask the user to press Home on the remote: they should land on Room Home.

**Undo:** `scripts/uninstall.sh` (or: clear `enabled_accessibility_services`, restore the three CEC settings to `0`, `0`, `standby_now`, and `adb uninstall com.roomremote.launcher`).

## Build

Needs a JDK and the Android SDK at `~/android-sdk` with `build-tools;34.0.0` and `platforms;android-34` (set up with `sdkmanager`). Then:

```bash
./build.sh   # outputs room-home.apk, signed with a debug key it generates (debug.keystore is git-ignored)
```

If the SDK is somewhere else, edit the `SDK=` line at the top of `build.sh`.

## Configure (ask the user, then edit)

| What | Where | Notes |
|---|---|---|
| City for sunrise/sunset | `LAT`, `LON` in `src/com/roomremote/launcher/MainActivity.java` | Optional, ask once at the end: "Which city are you in?" The default is Bengaluru (12.97, 77.59), close enough for most of India. Times use India time (Asia/Kolkata). |
| Default apps | `APPS` in `MainActivity.java` | Package names. The user can also use the + / - button on the TV. PlayStation uses the token `PS`. |
| Greetings | `GREETINGS` in `MainActivity.java` | No names: more than one person uses the TV. |
| Console wake (HDMI input ids) | `PS5_PORT`, `PS5_CEC` in `HomeWatcher.java` | See below. |
| Daily look | `Mood.java` | Palette, patterns and spider colours rotate by day number. |

After any edit: `./build.sh` then `scripts/install.sh <tv-ip> room-home.apk` (reinstall keeps settings).

### Finding the HDMI input ids for the console wake feature

The two constants are TV-specific. They are the TV Input Framework ids of the HDMI port the console is plugged into (`PS5_PORT`) and of its CEC device (`PS5_CEC`). Find yours with:

```bash
adb -s <tv-ip>:5555 shell dumpsys tv_input | grep -i -E "hdmi|HW|input id"
```

Look for ids like `com.<vendor>.tis/.HdmiInputService/HW4` (a physical port) and `.../HDMI3xxxxx` (a CEC device). Put the console on a known port, wake and sleep it, and watch which id changes state. Also, on the console itself, turn on **HDMI Device Link** and **One-Touch Play** (PS5: Settings > System > HDMI), otherwise it will not tell the TV to wake. If the user has no console, this feature simply never triggers; leave it.

## How it works (so you can change it safely)

- `MainActivity.java`: the screen: tiles, clock and greeting, lamp, cord, sun/moon, daily mood, app editor, the spider animation (`pullCord`).
- `HomeWatcher.java`: an accessibility service that (1) sends the user back to Room Home when something opens the stock home screen, and (2) watches HDMI input state to open the console's screen when it wakes, and return home when it sleeps. `Heal.java`, `HealReceiver.java` and `BootReceiver.java` keep that service enabled and actually connected: at boot, at app start, and from an alarm every ~2 minutes even when the launcher is not on screen. (Android can mark an accessibility service "crashed": still listed as enabled but not connected. `Heal.bound()` checks the real connection, and `Heal.ensure()` switches the service off and on to reconnect it.)
- `PsActivity.java`: shows the console's HDMI input full screen (a `TvView`).
- `Solar.java`, `SunView.java`, `SunLight.java`, `MoonView.java`: the sky. `Backdrop.java`, `DotGrid.java`, `Mood.java`: background. `LampView.java`, `CordView.java`, `SpiderView.java`, `SilkView.java`: the lamp scene.
- Day/night rule: between sunrise and sunset there is no lamp, no cord and no dim mode (always light). At night the lamp is shown and the cord toggles light/dim (saved in preference `dim`).

### Rules that keep it fast on a weak TV (follow them when changing visuals)

The target TV has 1.8 GB RAM and is slow to draw. Draw static things once into a hardware-layer view and move them with `setTranslationX/Y`, `setRotation` or alpha (GPU only). Do not call `invalidate()` on a large view every frame. Avoid long full-screen animations. The theme toggle, focus changes and grid scrolling are intentionally instant.

## Test the sky at any hour

```bash
adb -s <tv-ip>:5555 shell am start -n com.roomremote.launcher/.MainActivity --ei sun 1230   # 20:30 India time
adb -s <tv-ip>:5555 shell am start -n com.roomremote.launcher/.MainActivity --ei sun -1     # back to real time
```

`sun` is minutes after midnight, India time. **Always reset it with `-1` when you are done**, or the TV keeps showing that hour.

## Optional speed-ups for a slow TV (suggest, do not force)

```bash
adb -s <tv-ip>:5555 shell settings put global window_animation_scale 0.5
adb -s <tv-ip>:5555 shell settings put global transition_animation_scale 0.5
adb -s <tv-ip>:5555 shell device_config put activity_manager max_cached_processes 10
```

Undo: set the two scales back to `1.0` and run `device_config delete activity_manager max_cached_processes`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `scripts/install.sh` cannot reach the TV | ADB is off, wrong IP, or the prompt on the TV was not accepted. Re-do the prerequisites. The TV may also be asleep: wake it first. |
| Home key or a fresh TV start shows the stock home screen | The accessibility service is off or "crashed". It reconnects itself within about two minutes; to do it now, re-run `scripts/install.sh`, or check `adb shell dumpsys accessibility | grep -E "Bound|Crashed"`. |
| Console wakes but the TV does not switch | The console's HDMI Device Link / One-Touch Play is off, or `PS5_PORT`/`PS5_CEC` do not match this TV. |
| `adb` commands time out | Some TVs are slow when memory is tight. Retry with a longer timeout; avoid piling up commands. |
| The sun or lamp is at the wrong hour | Wrong `LAT`/`LON`, or a leftover test override: run the `--ei sun -1` command above. |

## Conventions for contributors

- Never commit keys, keystores, APKs or build output (see `.gitignore`).
- Keep changes small and verify on a real TV: take a screenshot with `adb exec-out screencap -p > shot.png`, or record with `adb shell screenrecord`.
- When you change what the screen looks like, update the images in `docs/` and the README.
- Credit third-party work in `CREDITS.md`.
