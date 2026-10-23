package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * The light the sun throws on the room: one wide, soft gradient drawn into a small texture and stretched over the
 * whole screen by the GPU, so moving it costs nothing. Brightest at the sun, fading with distance, like the lamp's.
 */
final class SunLight extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private int color = 0xFFFFE9A8;
    private float strength = 1f;

    SunLight(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setFocusable(false);
    }

    void setLight(int c, float s) {
        if (c == color && Math.abs(s - strength) < 0.02f) return;
        color = c;
        strength = s;
        invalidate();
    }

    @Override protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = getWidth() / 2f;
        int rgb = color & 0x00FFFFFF;
        p.setShader(new RadialGradient(cx, cy, r,
                new int[]{((int) (0xD0 * strength) << 24) | rgb, ((int) (0x9A * strength) << 24) | rgb, ((int) (0x58 * strength) << 24) | rgb,
                        ((int) (0x2A * strength) << 24) | rgb, ((int) (0x0C * strength) << 24) | rgb, rgb & 0x00FFFFFF},
                new float[]{0f, 0.08f, 0.25f, 0.5f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r, p);
    }
}
