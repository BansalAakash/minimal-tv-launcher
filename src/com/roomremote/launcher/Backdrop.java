package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * The background: a flat colour with a few soft colour washes. It is drawn once and kept as a GPU
 * texture, so it costs nothing while you move around (it only repaints when the theme changes).
 */
final class Backdrop extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private boolean dim;

    Backdrop(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    private float lampX, lampY, floorY;
    private boolean lampLit = true;

    /** At night the lamp lights the room; by day there is no lamp and the room is lit by the sky. */
    void setLampLit(boolean on) {
        if (lampLit == on) return;
        lampLit = on;
        invalidate();
    }

    /** Where the lamp stands (pixels): the light of the room is centred on it. */
    void setLamp(float x, float y, float floor) {
        lampX = x; lampY = y; floorY = floor;
        invalidate();
    }

    void setDim(boolean on) {
        if (dim == on) return;
        dim = on;
        invalidate();
    }

    private void wash(Canvas c, float x, float y, float r, int color, int alpha) {
        int rgb = color & 0x00FFFFFF;
        paint.setShader(new RadialGradient(x, y, r, new int[]{(alpha << 24) | rgb, rgb}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (dim) {
            c.drawColor(0xFF161514);
            wash(c, w * 0.78f, h * 0.15f, w * 0.60f, 0xFFD97757, 56);   // clay
            wash(c, w * 0.18f, h * 0.92f, w * 0.55f, 0xFFD4A27F, 40);   // kraft
            wash(c, w * 0.50f, h * 0.50f, w * 0.65f, 0xFF3A3835, 70);   // slate
            for (float[] m : Mood.washes()) wash(c, w * m[0], h * m[1], w * m[2], (int) m[3], (int) m[5]);   // the mood of the day
        } else {
            c.drawColor(0xFF8A7858);
            wash(c, w, 0, w * 0.75f, 0xFF4A3B28, 110);                      // the far corner stays dim
            for (float[] m : Mood.washes()) wash(c, w * m[0], h * m[1], w * m[2], (int) m[3], (int) m[4]);   // the mood of the day
            if (lampLit) {
                float lx = lampX > 0 ? lampX : w * 0.09f, ly = lampY > 0 ? lampY : h * 0.70f, fy = floorY > 0 ? floorY : h * 0.985f;
                // the lamp's light: strongest at the shade, falling off smoothly with distance
                paint.setShader(new RadialGradient(lx, ly, w * 0.85f,
                        new int[]{0xF2FFF2CC, 0xCCFFDE98, 0x80F4C27A, 0x40E0A862, 0x14C99A58, 0x00C99A58},
                        new float[]{0f, 0.09f, 0.28f, 0.52f, 0.78f, 1f}, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, paint);
                wash(c, lx, ly, w * 0.14f, 0xFFFFF6DC, 170);
                // the pool of light on the floor around the foot of the lamp
                paint.setShader(new RadialGradient(0, 0, w * 0.10f, 0xB0FFE3A6, 0x00FFE3A6, Shader.TileMode.CLAMP));
                c.save();
                c.translate(lx, fy);
                c.scale(3.2f, 0.26f);
                c.drawCircle(0, 0, w * 0.10f, paint);
                c.restore();
            } else {                                                          // daylight: a broad warm wash from the sky
                wash(c, w * 0.50f, h * 0.30f, w * 0.90f, 0xFFE9D6A8, 40);                // the sun itself lights the room (see SunLight)
            }
            paint.setShader(null);
        }
    }
}
