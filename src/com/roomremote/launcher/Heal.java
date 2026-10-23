package com.roomremote.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

/**
 * Keeps the Home-key watcher switched on. Android drops an accessibility service from its list when the app
 * is force-stopped, and it must be re-added after a restart; this puts it back (needs WRITE_SECURE_SETTINGS,
 * granted once over adb).
 */
final class Heal {
    private Heal() {}

    static void ensure(Context c) {
        try {
            ContentResolver cr = c.getContentResolver();
            String me = c.getPackageName() + "/" + HomeWatcher.class.getName();
            String cur = Settings.Secure.getString(cr, "enabled_accessibility_services");
            boolean listed = cur != null && cur.contains(me);
            if (listed && HomeWatcher.alive()) return;
            String others = "";
            if (cur != null) for (String s : cur.split(":")) if (!s.isEmpty() && !s.equals(me)) others += (others.isEmpty() ? "" : ":") + s;
            if (listed) {   // listed but not running: switch it off and on so Android binds it again
                Settings.Secure.putString(cr, "enabled_accessibility_services", others);
            }
            Settings.Secure.putString(cr, "enabled_accessibility_services", others.isEmpty() ? me : others + ":" + me);
            Settings.Secure.putInt(cr, "accessibility_enabled", 1);
        } catch (Exception ignored) { /* permission not granted: nothing to do */ }
    }
}
