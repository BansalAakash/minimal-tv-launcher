package com.roomremote.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.tv.TvContract;
import android.media.tv.TvView;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Shows the PS5's HDMI input itself, through the TV's own input framework. Unlike the TV's stock input
 * app, this one is told the moment the picture goes away (the PS5 went to rest), so it can hand you back
 * to the launcher within seconds, with no permission prompt and without reading logs.
 */
public class PsActivity extends Activity {
    static final String INPUT = "com.mediatek.tis/.HdmiInputService/HW4";   // HDMI 3
    static volatile boolean active;

    private TvView tv;
    private final Handler handler = new Handler();
    private boolean seenPicture;
    private final Runnable giveUp = new Runnable() {
        public void run() { backToLauncher(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        tv = new TvView(this);
        root.addView(tv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        tv.setCallback(new TvView.TvInputCallback() {
            @Override public void onVideoAvailable(String inputId) {
                seenPicture = true;
                handler.removeCallbacks(giveUp);
                HomeWatcher.note("PS5 picture available");
            }
            @Override public void onVideoUnavailable(String inputId, int reason) {
                HomeWatcher.note("PS5 picture unavailable, reason " + reason + ", seen=" + seenPicture);
                handler.removeCallbacks(giveUp);
                // Not yet showing anything is normal while the PS5 wakes; after it has shown a picture, losing it means rest mode.
                handler.postDelayed(giveUp, seenPicture ? 3000 : 45000);
            }
            @Override public void onConnectionFailed(String inputId) {
                HomeWatcher.note("PS5 input connection failed");
                backToLauncher();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
        seenPicture = false;
        tv.tune(INPUT, TvContract.buildChannelUriForPassthroughInput(INPUT));
        handler.postDelayed(giveUp, 45000);
    }

    @Override
    protected void onStop() {
        active = false;
        handler.removeCallbacks(giveUp);
        tv.reset();
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) { backToLauncher(); return true; }
        return super.onKeyDown(code, e);
    }

    void backToLauncher() {
        HomeWatcher.note("leaving PS5 screen");
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
