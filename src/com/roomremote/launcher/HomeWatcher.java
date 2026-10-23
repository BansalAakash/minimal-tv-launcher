package com.roomremote.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.view.accessibility.AccessibilityEvent;

/**
 * Runs in the background (the system keeps accessibility services alive).
 * 1. Sends the Home key to our launcher instead of the stock one.
 * 2. Follows the PS5 over HDMI-CEC: on -> switch to HDMI 3, off -> back to our launcher.
 */
public class HomeWatcher extends AccessibilityService {
    static final String TAG = "RoomHome";
    static final String STOCK_PKG = "com.google.android.apps.tv.launcherx";
    static final String PS5_CEC = "com.mediatek.tis/.HdmiInputService/HDMI300004";
    static final String PS5_PORT = "com.mediatek.tis/.HdmiInputService/HW4";

    private TvInputManager tim;
    private TvInputManager.TvInputCallback callback;
    private int last = -1;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private String lastPkg = "";                // which app was last on screen
    private static HomeWatcher self;
    static boolean alive() { return self != null; }

    /** Log to logcat and to a file on shared storage. */
    static void note(String m) { if (self != null) self.write(m); }

    private void write(String m) { logTo(this, m); }

    /** Append a timestamped line to the log file (also to logcat). Usable from any component of the app. */
    static void logTo(android.content.Context c, String m) {
        Log.i(TAG, m);
        try {
            java.io.File dir = c.getExternalFilesDir(null);
            if (dir == null) return;
            java.io.FileWriter w = new java.io.FileWriter(new java.io.File(dir, "watch.log"), true);
            w.write(new java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.ENGLISH).format(new java.util.Date()) + " " + m + "\n");
            w.close();
        } catch (Exception ignored) { }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        self = this;
        Heal.schedule(getApplicationContext());
        tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        last = tim.getInputState(PS5_CEC);
        lastPort = tim.getInputState(PS5_PORT);
        note("watcher up, PS5 state " + last + ", HDMI 3 port " + lastPort);
        callback = new TvInputManager.TvInputCallback() {
            @Override public void onInputStateChanged(String id, int state) { change(id, state); }
            @Override public void onInputRemoved(String id) { change(id, TvInputManager.INPUT_STATE_DISCONNECTED); }
        };
        tim.registerCallback(callback, new Handler(Looper.getMainLooper()));
    }

    private int lastPort = -1;
    private long lastWake;

    /**
     * Two signals say the PS5 woke: its CEC device input going to "connected", and the HDMI 3 port going to
     * "connected" (its 5 V line coming on). The CEC state can be stale, so either one counts.
     */
    private void change(String id, int state) {
        if (PS5_PORT.equals(id)) {
            note("HDMI 3 port state " + lastPort + " -> " + state + ", pkg=" + lastPkg);
            boolean was = lastPort == TvInputManager.INPUT_STATE_CONNECTED;
            lastPort = state;
            if (state == TvInputManager.INPUT_STATE_CONNECTED && !was) wake();
            return;
        }
        if (!PS5_CEC.equals(id)) return;
        note("PS5 input state " + last + " -> " + state + ", pkg=" + lastPkg);
        boolean wasOn = last == TvInputManager.INPUT_STATE_CONNECTED;
        boolean isOn = state == TvInputManager.INPUT_STATE_CONNECTED;
        last = state;
        if (isOn && !wasOn) wake();
        else if (!isOn && wasOn && PsActivity.active) home();   // do not pull you out of Netflix because the PS5 went to rest
    }

    private void wake() {
        long now = System.currentTimeMillis();
        if (PsActivity.active || now - lastWake < 8000) return;
        lastWake = now;
        note("PS5 woke, opening its screen");
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isInteractive()) {
                note("TV was asleep, waking it");
                pm.newWakeLock(android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK | android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP, "roomhome:ps5").acquire(10000);
            }
        } catch (Throwable t) { note("wake failed " + t); }
        start(new Intent(this, PsActivity.class));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName();
        CharSequence cls = e.getClassName();
        if (pkg != null && !"com.android.systemui".contentEquals(pkg) && !pkg.toString().equals(lastPkg)) { lastPkg = pkg.toString(); note("foreground " + lastPkg); }
        if (pkg != null && STOCK_PKG.contentEquals(pkg) && cls != null && cls.toString().endsWith("HomeActivity")) {
            home();
        }
    }

    private void home() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        start(i);
    }

    private void start(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ex) {
            Log.w(TAG, "start failed", ex);
        }
    }

    @Override public void onInterrupt() { logTo(this, "watcher interrupted by Android"); }

    @Override public boolean onUnbind(Intent intent) {
        logTo(this, "Android disconnected the watcher (onUnbind), pid " + android.os.Process.myPid());
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        logTo(this, "watcher destroyed");
        if (self == this) self = null;
        if (tim != null && callback != null) tim.unregisterCallback(callback);
        super.onDestroy();
    }
}
