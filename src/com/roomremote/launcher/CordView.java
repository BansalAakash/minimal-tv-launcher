package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** The lamp's pull cord: focusable, press it to turn the lamp (and so the light/dim theme) on or off. */
public class CordView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float ropeLen, bead;
    private float pull;
    private boolean lampOn = true;

    public CordView(Context c, float ropeLen, float bead) {
        super(c);
        this.ropeLen = ropeLen;
        this.bead = bead;
        setFocusable(true);
        setClickable(true);
    }

    void setLampOn(boolean v) { if (lampOn != v) { lampOn = v; invalidate(); } }

    void setPull(float px) { if (pull != px) { pull = px; invalidate(); } }

    @Override protected void onFocusChanged(boolean gained, int dir, android.graphics.Rect prev) {
        super.onFocusChanged(gained, dir, prev);
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f;
        float y = ropeLen + pull;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, bead * 0.28f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(lampOn ? 0xFFC9A15E : 0xFF8A7352);
        c.drawLine(cx, 0, cx, y, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(lampOn ? 0xFFE4BE7A : 0xFF9B8460);
        c.drawCircle(cx, y + bead, bead, p);
        p.setColor(lampOn ? 0xFFFFE9B8 : 0xFFB8A27C);
        c.drawCircle(cx - bead * 0.3f, y + bead * 0.7f, bead * 0.35f, p);
        // an always-visible halo and label, so it reads as something you can press
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, bead * 0.22f));
        p.setColor(lampOn ? 0x66D97757 : 0x88D97757);
        c.drawCircle(cx, y + bead, bead * 1.9f, p);
        if (isFocused()) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, bead * 0.4f));
            p.setColor(0xFFD97757);
            c.drawCircle(cx, y + bead, bead * 2.4f, p);
        }
    }
}
