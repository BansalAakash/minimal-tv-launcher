package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * A tripod floor lamp with a burlap drum shade, drawn once: lit in light mode, dark in dim mode.
 * Lit: the shade glows from the inside out (bright hotspot where the bulb is, darker amber toward the
 * edges, light leaking from the open rim). The light it throws on the room is painted in Backdrop.
 */
public class LampView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean on = true;

    public LampView(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setFocusable(false);
    }

    void setOn(boolean v) {
        if (on == v) return;
        on = v;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f;
        float shadeTop = h * 0.075f, shadeBot = h * 0.295f;
        float topHalf = w * 0.37f, botHalf = w * 0.42f;
        float baseY = h * 0.975f;
        float line = Math.max(2f, w * 0.022f);

        // soft contact shadow under the base
        p.setShader(new RadialGradient(cx, baseY, w * 0.5f, on ? 0x55200F00 : 0x66000000, 0x00000000, Shader.TileMode.CLAMP));
        c.save();
        c.scale(1f, 0.16f, cx, baseY);
        c.drawCircle(cx, baseY, w * 0.5f, p);
        c.restore();
        p.setShader(null);

        int frame = on ? 0xFF2B2219 : 0xFF33312F;
        int sheen = on ? 0xFF7A6247 : 0xFF4A4845;
        float hubY = shadeBot + h * 0.012f;
        float footL = cx - w * 0.36f, footR = cx + w * 0.36f, footY = baseY - h * 0.02f;

        // legs: back one first so it sits behind
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(on ? 0xFF1E1812 : 0xFF292826);
        p.setStrokeWidth(line * 0.75f);
        c.drawLine(cx, hubY, cx, footY - h * 0.01f, p);
        p.setColor(frame);
        p.setStrokeWidth(line);
        c.drawLine(cx - w * 0.025f, hubY, footL, footY, p);
        c.drawLine(cx + w * 0.025f, hubY, footR, footY, p);
        p.setColor(sheen);   // a thin highlight along the lit edge of each leg
        p.setStrokeWidth(line * 0.28f);
        c.drawLine(cx - w * 0.025f - line * 0.25f, hubY, footL - line * 0.25f, footY, p);
        c.drawLine(cx + w * 0.025f - line * 0.25f, hubY, footR - line * 0.25f, footY, p);

        // the little round shelf between the legs
        float shY = h * 0.62f;
        float t = (shY - hubY) / (footY - hubY);
        float half = w * 0.025f + (w * 0.36f - w * 0.025f) * t;
        p.setStyle(Paint.Style.FILL);
        p.setColor(frame);
        c.drawOval(new RectF(cx - half * 0.95f, shY - h * 0.011f, cx + half * 0.95f, shY + h * 0.011f), p);
        p.setColor(sheen);
        c.drawOval(new RectF(cx - half * 0.8f, shY - h * 0.009f, cx + half * 0.8f, shY + h * 0.002f), p);
        // hub where the legs meet the shade
        p.setColor(frame);
        c.drawOval(new RectF(cx - w * 0.05f, hubY - h * 0.01f, cx + w * 0.05f, hubY + h * 0.01f), p);
        // flat triangular foot plate
        Path plate = new Path();
        plate.moveTo(footL - w * 0.06f, baseY);
        plate.lineTo(footR + w * 0.06f, baseY);
        plate.lineTo(cx, baseY - h * 0.05f);
        plate.close();
        c.drawPath(plate, p);

        // the drum shade, a little wider at the bottom, with a gently curved lower edge
        Path shade = new Path();
        shade.moveTo(cx - topHalf, shadeTop);
        shade.lineTo(cx - botHalf, shadeBot);
        shade.quadTo(cx, shadeBot + h * 0.035f, cx + botHalf, shadeBot);
        shade.lineTo(cx + topHalf, shadeTop);
        shade.close();
        int[] cols = on
                ? new int[]{0xFFB8621F, 0xFFE59A4C, 0xFFF9CF86, 0xFFE59A4C, 0xFFB35E1D}
                : new int[]{0xFF241B14, 0xFF40332A, 0xFF52433A, 0xFF40332A, 0xFF22190F};
        p.setShader(new LinearGradient(cx - botHalf, 0, cx + botHalf, 0, cols, new float[]{0f, 0.28f, 0.5f, 0.72f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(shade, p);
        p.setShader(null);

        c.save();
        c.clipPath(shade);
        if (on) {   // the bulb: hottest at the middle, fading outward through the fabric
            p.setShader(new RadialGradient(cx, shadeTop + (shadeBot - shadeTop) * 0.42f, botHalf * 1.2f, 0xCCFFF6DA, 0x00FFF6DA, Shader.TileMode.CLAMP));
            c.drawRect(0, shadeTop, w, shadeBot + h * 0.04f, p);
            p.setShader(new LinearGradient(0, shadeTop, 0, shadeBot, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(0, shadeTop, w, shadeBot, p);
            p.setShader(null);
        }
        // burlap weave
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, w * 0.007f));
        p.setColor(on ? 0x38A05A22 : 0x30000000);
        for (float x = cx - botHalf; x < cx + botHalf; x += w * 0.04f) c.drawLine(x, shadeTop, x, shadeBot + h * 0.04f, p);
        p.setColor(on ? 0x2CFFE6B0 : 0x16FFFFFF);
        for (float y = shadeTop; y < shadeBot + h * 0.04f; y += w * 0.04f) c.drawLine(cx - botHalf, y, cx + botHalf, y, p);
        c.restore();
        p.setStyle(Paint.Style.FILL);

        // open top: we look slightly down into the drum
        RectF top = new RectF(cx - topHalf, shadeTop - h * 0.012f, cx + topHalf, shadeTop + h * 0.014f);
        p.setColor(on ? 0xFFFFF3D2 : 0xFF17120E);
        c.drawOval(top, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, w * 0.012f));
        p.setColor(on ? 0xFFD28A3E : 0xFF5B493B);
        c.drawOval(top, p);
        if (on) {   // light leaking from under the rim
            Path rim = new Path();
            rim.moveTo(cx - botHalf, shadeBot);
            rim.quadTo(cx, shadeBot + h * 0.035f, cx + botHalf, shadeBot);
            p.setColor(0xCCFFF1C4);
            p.setStrokeWidth(Math.max(1.5f, w * 0.014f));
            c.drawPath(rim, p);
        }
        p.setStyle(Paint.Style.FILL);
    }
}
