package com.roomremote.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * The soft coloured light behind each card. One small pre-drawn picture per card, placed once;
 * focus only changes its opacity, which the GPU does on its own (no per-frame drawing code).
 * They all live in this layer, under every card, so a glow never tints a neighbouring card.
 */
final class GlowLayer extends FrameLayout {
    private static final class Glow {
        View card; ImageView view;
    }

    private static final class Light extends ImageView {
        Light(Context c) { super(c); }
        @Override public boolean hasOverlappingRendering() { return false; }   // opacity without an offscreen pass
    }

    private final List<Glow> glows = new ArrayList<>();
    private final int[] a = new int[2], b = new int[2];
    private float rest = 0.16f, lit = 0.55f;
    private final float cardW, cardH;

    GlowLayer(Context c, int cardWpx, int cardHpx) {
        super(c);
        cardW = cardWpx; cardH = cardHpx;
        setClipChildren(false);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() { place(); }
        });
    }

    private static Bitmap picture(int color) {
        int w = 192, h = 120;
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        int rgb = color & 0x00FFFFFF;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        p.setShader(new RadialGradient(w / 2f, w / 2f, w / 2f,
                new int[]{rgb | 0xFF000000, rgb | 0xAA000000, rgb | 0x44000000, rgb},
                new float[]{0f, 0.4f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        cv.scale(1f, h / (float) w);                 // squash the circle into a soft ellipse
        cv.drawCircle(w / 2f, w / 2f, w / 2f, p);
        return bm;
    }

    void add(View card, int color) {
        Glow g = new Glow();
        g.card = card;
        g.view = new Light(getContext());
        g.view.setImageBitmap(picture(color));
        g.view.setScaleType(ImageView.ScaleType.FIT_XY);
        g.view.setAlpha(rest);
        addView(g.view, new FrameLayout.LayoutParams((int) (cardW * 1.9f), (int) (cardH * 2.2f)));
        glows.add(g);
    }

    void clear() {
        removeAllViews();
        glows.clear();
    }

    private void place() {
        getLocationOnScreen(b);
        for (Glow g : glows) {
            if (g.card.getWidth() == 0) continue;
            g.card.getLocationOnScreen(a);
            float cx = a[0] - b[0] + g.card.getWidth() / 2f, cy = a[1] - b[1] + g.card.getHeight() / 2f;
            g.view.setTranslationX(cx - cardW * 0.95f);
            g.view.setTranslationY(cy - cardH * 1.1f);
        }
    }

    void setFocused(View card, boolean on) {
        for (Glow g : glows) if (g.card == card) g.view.setAlpha(on ? lit : rest);
    }

    void setLevels(float restLevel, float litLevel) {
        rest = restLevel; lit = litLevel;
        for (Glow g : glows) g.view.setAlpha(g.card.isFocused() ? lit : rest);
    }
}
