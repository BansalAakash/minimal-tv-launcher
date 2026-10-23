package com.roomremote.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** The alarm's target: checks that the Home-key watcher is connected and puts it back if not. */
public class HealReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) { Heal.ensure(c); }
}
