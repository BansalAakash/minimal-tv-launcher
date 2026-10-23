package com.roomremote.launcher;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

/**
 * Keeps the Home-key watcher switched on and actually connected. Android drops an accessibility service from its
 * list when the app is force-stopped, and marks it "crashed" (listed but not connected) if it dies; either way it
 * has to be switched off and on again. This does that (needs WRITE_SECURE_SETTINGS, granted once over adb), at app
 * start, after a restart or update, and every couple of minutes from an alarm, even when the launcher is not on screen.
 */
final class Heal {
    private Heal() {}

    /** True if Android has our service connected right now (not just listed as enabled). */
    static boolean bound(Context c) {
        try {
            AccessibilityManager am = (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
            for (AccessibilityServiceInfo i : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (i.getId() != null && i.getId().startsWith(c.getPackageName() + "/")) return true;
            }
        } catch (Throwable ignored) { /* treat as not connected */ }
        return false;
    }

    static void ensure(Context c) {
        try {
            ContentResolver cr = c.getContentResolver();
            String me = c.getPackageName() + "/" + HomeWatcher.class.getName();
            String cur = Settings.Secure.getString(cr, "enabled_accessibility_services");
            boolean listed = cur != null && cur.contains(me);
            if (listed && bound(c)) return;
            HomeWatcher.logTo(c, listed ? "heal: listed as enabled but not connected, reconnecting" : "heal: not in the enabled list, adding it back");
            String others = "";
            if (cur != null) for (String s : cur.split(":")) if (!s.isEmpty() && !s.equals(me)) others += (others.isEmpty() ? "" : ":") + s;
            if (listed) {   // listed but not connected ("crashed"): switch it off and on so Android binds it again
                Settings.Secure.putString(cr, "enabled_accessibility_services", others);
            }
            Settings.Secure.putString(cr, "enabled_accessibility_services", others.isEmpty() ? me : others + ":" + me);
            Settings.Secure.putInt(cr, "accessibility_enabled", 1);
        } catch (Exception ignored) { /* permission not granted: nothing to do */ }
    }

    /** A repeating alarm that re-runs ensure() every two minutes, whether or not the app is running. */
    static void schedule(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = PendingIntent.getBroadcast(c, 1, new Intent(c, HealReceiver.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 60000L, 120000L, pi);
        } catch (Throwable ignored) { /* best effort */ }
    }
}
