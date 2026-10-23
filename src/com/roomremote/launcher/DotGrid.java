package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/** A faint, static dot grid (one tiled bitmap, drawn once) for a blueprint feel. */
final class DotGrid extends View {
    private final Paint paint = new Paint();
    private final int spacing;
    private final float dot;

    DotGrid(Context c, int spacingPx, float dotPx, int color) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        spacing = spacingPx; dot = dotPx;
        setColor(color);
    }

    void setColor(int color) {
        int spacingPx = spacing; float dotPx = dot;
        Bitmap tile = Bitmap.createBitmap(spacingPx, spacingPx, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(tile);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        float c0 = spacingPx / 2f;
        switch (Mood.pattern()) {
            case 1:   // plus signs
                p.setStrokeWidth(dotPx * 1.2f);
                p.setStrokeCap(Paint.Cap.ROUND);
                cv.drawLine(c0 - dotPx * 2.2f, c0, c0 + dotPx * 2.2f, c0, p);
                cv.drawLine(c0, c0 - dotPx * 2.2f, c0, c0 + dotPx * 2.2f, p);
                break;
            case 2: { // diamonds
                android.graphics.Path d = new android.graphics.Path();
                d.moveTo(c0, c0 - dotPx * 2f); d.lineTo(c0 + dotPx * 2f, c0); d.lineTo(c0, c0 + dotPx * 2f); d.lineTo(c0 - dotPx * 2f, c0); d.close();
                cv.drawPath(d, p);
                break;
            }
            case 3:   // rings
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dotPx * 0.8f);
                cv.drawCircle(c0, c0, dotPx * 2.2f, p);
                break;
            case 4:   // short dashes
                p.setStrokeWidth(dotPx * 1.1f);
                p.setStrokeCap(Paint.Cap.ROUND);
                cv.drawLine(c0 - dotPx * 2.4f, c0 + dotPx * 1.2f, c0 + dotPx * 2.4f, c0 - dotPx * 1.2f, p);
                break;
            default:  // dots
                cv.drawCircle(c0, c0, dotPx, p);
        }
        paint.setShader(new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
