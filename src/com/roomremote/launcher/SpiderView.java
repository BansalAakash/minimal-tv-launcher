package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * A cute spider hanging on a thread: round body, googly eyes whose pupils look around, six curved legs.
 *
 * The design (shapes, proportions, colours, leg angles, the glancing pupils) is ported from the CodePen
 * "Cute spider CSS animation" by webcrafters: https://codepen.io/webcrafterscz/pen/EGYOxa
 * Drawn only when the pupils move; the GPU slides and swings the view.
 */
public class SpiderView extends View {
    static final float CW = 250f, CH = 110f;      // the pen's CSS pixels, with room for the legs
    static final float X0 = 75f, Y0 = 12f;        // where the body's top-left sits in that space
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arch = new Path();
    private final RectF r = new RectF();
    private float lookX = 17.25f, lookY = 17.25f;  // pupil centre inside each eye (eyes are 25x25)
    private boolean lampOn = true;
    private int bodyColor = 0xFF222222;

    public SpiderView(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setFocusable(false);
        arch.moveTo(12f, 14f);                     // one leg: the top border of an 80x40 rounded box
        arch.quadTo(45f, -11f, 78f, 14f);
    }

    void setLampOn(boolean v) { lampOn = v; }

    /** The spider colour of the day. */
    void setBody(int color) { bodyColor = color; invalidate(); }

    private static int lighten(int c, float f) {
        int r = (c >> 16 & 255), g = (c >> 8 & 255), b = (c & 255);
        return 0xFF000000 | ((int) (r + (255 - r) * f) << 16) | ((int) (g + (255 - g) * f) << 8) | (int) (b + (255 - b) * f);
    }

    /** Pupil position inside the eye: x 9.55 (left) .. 17.25 (right), y about 17 (middle) .. 21 (down). */
    void look(float x, float y) { lookX = x; lookY = y; invalidate(); }

    private void leg(Canvas c, float x, float y, float deg) {
        c.save();
        c.translate(x + 45f, y + 22.5f);
        c.rotate(deg);
        c.translate(-45f, -22.5f);
        c.drawPath(arch, p);
        c.restore();
    }

    @Override protected void onDraw(Canvas c) {
        float s = getWidth() / CW;
        c.save();
        c.scale(s, s);
        c.translate(X0, Y0);
        int body = lampOn ? bodyColor : lighten(bodyColor, 0.22f), legs = lampOn ? lighten(bodyColor, 0.08f) : lighten(bodyColor, 0.30f);

        // legs sit behind the body: three arches either side, fanned out
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(5f);
        p.setColor(legs);
        leg(c, -60f, -7f, 10f);
        leg(c, -50f, 18f, -20f);
        leg(c, -40f, 43f, -50f);
        leg(c, 60f, -7f, -10f);
        leg(c, 50f, 18f, 20f);
        leg(c, 40f, 43f, 50f);

        // body
        p.setStyle(Paint.Style.FILL);
        p.setColor(body);
        r.set(0f, 0f, 100f, 80f);
        c.drawOval(r, p);

        // eyes and pupils
        float[] ex = {20f, 55f};
        for (float x : ex) {
            p.setColor(0xFFFFFFFF);
            c.drawCircle(x + 12.5f, 35f + 12.5f, 12.5f, p);
            p.setColor(0xFF222222);
            c.drawCircle(x + lookX, 35f + lookY, 3.5f, p);
        }
        c.restore();
    }
}
