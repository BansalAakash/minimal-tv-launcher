package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** A soft sun: a wide warm glow and a bright core. Drawn when its colour changes; the GPU moves it. */
final class SunView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = 0xFFFFE08A;
    private float strength = 1f;

    SunView(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setFocusable(false);
    }

    void setSun(int c, float s) {
        if (c == color && Math.abs(s - strength) < 0.02f) return;
        color = c;
        strength = s;
        invalidate();
    }

    @Override protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = getWidth() / 2f;
        int rgb = color & 0x00FFFFFF;
        p.setShader(new RadialGradient(cx, cy, r,
                new int[]{((int) (0x88 * strength) << 24) | rgb, ((int) (0x44 * strength) << 24) | rgb, ((int) (0x14 * strength) << 24) | rgb, rgb},
                new float[]{0.12f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r, p);
        p.setShader(new RadialGradient(cx, cy, r * 0.2f,
                new int[]{((int) (0xFF * strength) << 24) | 0xFFFFFF, ((int) (0xF0 * strength) << 24) | rgb, rgb},
                new float[]{0f, 0.78f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r * 0.2f, p);
    }
}
