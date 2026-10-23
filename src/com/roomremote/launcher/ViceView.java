package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * A neon-sunset scene in the spirit of a Vice City poster: violet-to-pink-to-orange sky, a low sun,
 * a teal sea with its reflection and palm silhouettes. Drawn from scratch, once, and cached.
 */
final class ViceView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    ViceView(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    private void palm(Canvas c, float x, float base, float h, float lean, float s) {
        p.setShader(null);
        p.setColor(0xFF1A0B33);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(s * 0.9f);
        Path trunk = new Path();
        trunk.moveTo(x, base);
        float tx = x + lean, ty = base - h;
        trunk.quadTo(x + lean * 0.1f, base - h * 0.55f, tx, ty);
        c.drawPath(trunk, p);
        p.setStrokeWidth(s * 0.55f);
        float[][] fronds = {{-1.7f, 0.35f}, {-1.2f, -0.1f}, {-0.5f, -0.6f}, {0.5f, -0.6f}, {1.2f, -0.1f}, {1.7f, 0.35f}};
        for (float[] f : fronds) {
            Path leaf = new Path();
            leaf.moveTo(tx, ty);
            leaf.quadTo(tx + f[0] * s * 3f, ty + f[1] * s * 2f - s * 2f, tx + f[0] * s * 5f, ty + f[1] * s * 6f + s);
            c.drawPath(leaf, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), horizon = h * 0.70f;
        // sky
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, 0, horizon,
                new int[]{0xFF2B0F63, 0xFFB8237F, 0xFFFF5E7A, 0xFFFF9A4D}, new float[]{0f, 0.42f, 0.75f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, horizon, p);
        // sun and its halo
        float sx = w * 0.5f, sr = h * 0.30f;
        p.setShader(new RadialGradient(sx, horizon, sr * 2.1f, new int[]{0x66FFD27A, 0x00FFD27A}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, horizon, p);
        p.setShader(new LinearGradient(0, horizon - sr, 0, horizon, 0xFFFFE08A, 0xFFFF8F5A, Shader.TileMode.CLAMP));
        c.save();
        c.clipRect(0, 0, w, horizon);
        c.drawCircle(sx, horizon, sr, p);
        c.restore();
        // sea
        p.setShader(new LinearGradient(0, horizon, 0, h, 0xFF139AA8, 0xFF0A3F6B, Shader.TileMode.CLAMP));
        c.drawRect(0, horizon, w, h, p);
        // sun reflection: stripes that thin out
        p.setShader(null);
        p.setColor(0xCCFFC46B);
        float y = horizon + h * 0.025f, bar = h * 0.030f, half = sr * 0.95f;
        for (int i = 0; i < 5; i++) {
            float k = 1f - i * 0.17f;
            c.drawRect(sx - half * k, y, sx + half * k, y + bar, p);
            y += bar * 1.9f;
            bar *= 0.78f;
        }
        // palms
        palm(c, w * 0.09f, h * 1.02f, h * 0.70f, h * 0.10f, h * 0.045f);
        palm(c, w * 0.93f, h * 1.02f, h * 0.60f, -h * 0.08f, h * 0.040f);
    }
}
