package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** A thin silk thread, drawn once; the GPU slides it. */
public class SilkView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean lampOn = true;

    public SilkView(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setFocusable(false);
    }

    void setLampOn(boolean v) { if (lampOn != v) { lampOn = v; invalidate(); } }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), cx = w / 2f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w * 0.9f);
        p.setColor(lampOn ? 0x33FFFFFF : 0x22FFFFFF);      // a soft sheen around the thread
        c.drawLine(cx, 0, cx, h, p);
        p.setStrokeWidth(Math.max(1.5f, w * 0.34f));
        p.setColor(lampOn ? 0xFFF7F2E6 : 0xFFC9C6BD);
        c.drawLine(cx, 0, cx, h, p);
    }
}
