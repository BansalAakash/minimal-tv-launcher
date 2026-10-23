package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * An app icon. If the icon sits on a white square (YouTube, Netflix), the white is turned into
 * transparency so only the logo remains, the same in light and dim mode, like the PlayStation one.
 */
final class IconView extends ImageView {
    private boolean bare;

    /** On a dark card, a bare logo's red needs to be brighter to look like the brand's red. */
    void setLift(boolean on) {
        if (!bare || !on) { clearColorFilter(); return; }
        float k = 1.38f;
        setColorFilter(new android.graphics.ColorMatrixColorFilter(new float[]{
                k, 0, 0, 0, 0,  0, k, 0, 0, 0,  0, 0, k, 0, 0,  0, 0, 0, 1, 0}));
    }

    IconView(Context c, Drawable icon) {
        super(c);
        int s = 256;
        Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        icon.setBounds(0, 0, s, s);
        icon.draw(cv);
        bare = whiteBacked(bm);
        if (bare) dropWhite(bm);
        setImageBitmap(bm);
        setScaleType(ScaleType.FIT_XY);
    }

    private static boolean light(int px) {
        return Color.alpha(px) > 230 && Color.red(px) > 235 && Color.green(px) > 235 && Color.blue(px) > 235;
    }

    private static boolean whiteBacked(Bitmap bm) {
        int w = bm.getWidth(), h = bm.getHeight(), m = 14;
        return light(bm.getPixel(m, h / 2)) && light(bm.getPixel(w - m, h / 2))
                && light(bm.getPixel(w / 2, m)) && light(bm.getPixel(w / 2, h - m));
    }

    private static boolean backing(int p) {
        return Color.alpha(p) < 40 || Math.min(Color.red(p), Math.min(Color.green(p), Color.blue(p))) >= 215;
    }

    /**
     * Removes only the white backing around the logo: a flood fill from the edges finds it, so white
     * parts inside the logo (the play triangle) stay white. The fringe gets "colour to alpha" so it is smooth.
     */
    private static void dropWhite(Bitmap bm) {
        int w = bm.getWidth(), h = bm.getHeight();
        int[] px = new int[w * h];
        bm.getPixels(px, 0, w, 0, 0, w, h);
        boolean[] near = new boolean[w * h];
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        for (int x = 0; x < w; x++) { seed(px, near, q, x, w, h); seed(px, near, q, (h - 1) * w + x, w, h); }
        for (int y = 0; y < h; y++) { seed(px, near, q, y * w, w, h); seed(px, near, q, y * w + w - 1, w, h); }
        while (!q.isEmpty()) {
            int i = q.poll(), x = i % w, y = i / w;
            if (x > 0) seed(px, near, q, i - 1, w, h);
            if (x < w - 1) seed(px, near, q, i + 1, w, h);
            if (y > 0) seed(px, near, q, i - w, w, h);
            if (y < h - 1) seed(px, near, q, i + w, w, h);
        }
        for (int pass = 0; pass < 2; pass++) {       // widen by 2 px to catch the anti-aliased fringe
            boolean[] grown = near.clone();
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (near[i]) continue;
                if ((x > 0 && near[i - 1]) || (x < w - 1 && near[i + 1]) || (y > 0 && near[i - w]) || (y < h - 1 && near[i + w])) grown[i] = true;
            }
            near = grown;
        }
        for (int i = 0; i < px.length; i++) {
            if (!near[i]) continue;
            int p = px[i], pa = Color.alpha(p);
            if (pa == 0) continue;
            int r = Color.red(p), g = Color.green(p), b = Color.blue(p);
            int a = 255 - Math.min(r, Math.min(g, b));
            if (a <= 0) { px[i] = 0; continue; }
            int k = 255 - a;
            px[i] = Color.argb(a * pa / 255, Math.max(0, Math.min(255, (r - k) * 255 / a)),
                    Math.max(0, Math.min(255, (g - k) * 255 / a)), Math.max(0, Math.min(255, (b - k) * 255 / a)));
        }
        bm.setPixels(px, 0, w, 0, 0, w, h);
    }

    private static void seed(int[] px, boolean[] near, java.util.ArrayDeque<Integer> q, int i, int w, int h) {
        if (!near[i] && backing(px[i])) { near[i] = true; q.add(i); }
    }
}
