package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** The moon, with today's phase: a soft cool glow, the dim unlit disc, and the lit part. Drawn when its phase or brightness changes. */
final class MoonView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path lit = new Path();
    private float phase = 0.5f, strength = 1f;

    MoonView(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setFocusable(false);
    }

    /** 0 new, 0.25 first quarter, 0.5 full, 0.75 last quarter. */
    void set(float ph, float s) {
        if (Math.abs(ph - phase) < 0.004f && Math.abs(s - strength) < 0.02f) return;
        phase = ph;
        strength = s;
        invalidate();
    }

    /** Phase of the moon now, from the average lunar cycle (accurate to about a day). */
    static float phaseNow() {
        double days = (System.currentTimeMillis() - 947182440000L) / 86400000.0;   // since the new moon of 6 Jan 2000
        double ph = (days / 29.530588853) % 1.0;
        return (float) (ph < 0 ? ph + 1 : ph);
    }

    @Override protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = getWidth() * 0.072f;
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx, cy, getWidth() / 2f,
                new int[]{((int) (0x40 * strength) << 24) | 0xC8D8F0, ((int) (0x14 * strength) << 24) | 0xC8D8F0, 0x00C8D8F0},
                new float[]{0.08f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, getWidth() * 0.36f, p);
        p.setShader(null);
        p.setColor(((int) (0x38 * strength) << 24) | 0x4A5262);                       // the unlit part, faintly there
        cv.drawCircle(cx, cy, r, p);

        RectF disc = new RectF(cx - r, cy - r, cx + r, cy + r);
        float c = (float) Math.cos(2 * Math.PI * phase);                              // 1 new .. -1 full
        float rx = r * Math.abs(c);
        RectF term = new RectF(cx - rx, cy - r, cx + rx, cy + r);
        lit.reset();
        if (phase < 0.5f) {                                                           // waxing: lit on the right
            lit.arcTo(disc, -90, 180);
            lit.arcTo(term, 90, c > 0 ? -180 : 180);
        } else {                                                                      // waning: lit on the left
            lit.arcTo(disc, 90, 180);
            lit.arcTo(term, -90, c > 0 ? -180 : 180);
        }
        lit.close();
        p.setColor(((int) (0xE8 * strength) << 24) | 0xF4F1E4);
        cv.drawPath(lit, p);
        cv.save();
        cv.clipPath(lit);
        p.setColor(((int) (0x22 * strength) << 24) | 0x808690);                       // a few maria
        cv.drawCircle(cx - r * 0.3f, cy - r * 0.25f, r * 0.28f, p);
        cv.drawCircle(cx + r * 0.25f, cy + r * 0.15f, r * 0.2f, p);
        cv.drawCircle(cx - r * 0.1f, cy + r * 0.5f, r * 0.14f, p);
        cv.restore();
    }
}
