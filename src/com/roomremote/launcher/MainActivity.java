package com.roomremote.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.animation.ValueAnimator;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A quiet home screen: a slow space video, the time, six large icons that glow in their own colour,
 * and two small buttons (Settings, Play Store) in the corner. Nothing animates except the video and
 * the focus, so it costs the TV almost nothing.
 */
public class MainActivity extends Activity {
    // HDMI 3, where the PS5 is plugged in.
    private static final String PS5_INPUT = "com.mediatek.tis/.HdmiInputService/HW4";
    private static final String PS = "@ps5";   // token for the PlayStation card in the saved list
    private static final String[] APPS = {   // default list, in order: row 1 YouTube, Netflix, SonyLIV; row 2 PlayStation, Prime, JioHotstar
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.sonyliv",
        PS,
        "com.amazon.amazonvideo.livingroom",
        "in.startv.hotstar",
    };
    private static final int LAYOUT = 2;      // bump to reset everyone's saved order to APPS

    private static final int CARD_W = 188, CARD_H = 118;   // dp
    private static final int LOGO = 72;                      // dp, every logo or tile sits in the same square box
    private static final int CLOCK_BAND = 110;                 // dp from the top that belong to the clock and corner buttons
    private static final int ACCENT = 0xFFD97757;            // warm terracotta
    
    // Two themes: day is warm beige, dim is warm charcoal for the evening.
    private boolean dim;
    private DotGrid dots;
    private View shade;
    private ImageView psLogo;
    private final List<IconView> appIcons = new ArrayList<>();
    private final List<GradientDrawable> faces = new ArrayList<>();
    private final List<View> faceHosts = new ArrayList<>();
    private FrameLayout root;
    private LinearLayout grid, cornerBox;
    private ScrollView scroll;
    private View editBtn;
    private Drawable editIcon;
    private int faceMark;     // faces before the editor opened: the corner buttons and the grid cards
    private int fixedFaces;   // faces[0..fixedFaces) belong to the corner buttons and survive a grid rebuild
    // Editor overlay; null when closed.
    private FrameLayout editor;
    private final List<String> chosen = new ArrayList<>();
    private final List<Runnable> editPaint = new ArrayList<>();
    private String savedAtOpen;

    private int cardFace()  { return dim ? 0xE6262624 : 0xF2C6B38F; }
    private int cardLit()   { return dim ? 0xF02F2E2B : 0xFFD4C4A0; }
    private int border()    { return dim ? 0xFF3D3D3A : 0xFF84714F; }
    private int textColor() { return dim ? 0xFFFAF9F5 : 0xFF141413; }
    private int muted()     { return dim ? 0xFFB0AEA5 : 0xFF4F4630; }

    private Backdrop backdrop;
    private LampView lamp;
    private CordView cord;
    private SpiderView person;
    private SilkView cable;
    private ValueAnimator walkAnim;
    private boolean walking;
    private GlowLayer glow;
    private TextView greeting, stamp;
    private String greetingText = "";
    private int greetingPeriod = -1;
    private View clockBox;
    private SunLight sunLight;
    private SunView sun;
    private MoonView moon;
    private boolean nightShown = true;
    private int sunTest = -1;   // minutes after midnight IST, set by `am start --ei sun N`, for checking the sun at other hours
    private final android.os.Handler clock = new android.os.Handler();
    private final Runnable ticker = new Runnable() {
        public void run() { tick(); clock.postDelayed(this, 20000); }
    };
    private View first;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dim = wantDim();
        Heal.schedule(this);
        root = new FrameLayout(this);
        root.setClipChildren(false);
        backdrop = new Backdrop(this);
        backdrop.setDim(dim);
        root.addView(backdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // A soft darkening so the clock and glows read against any frame of the video.
        dots = new DotGrid(this, dp(22), dp(1) * 0.7f, dim ? 0x16FAF9F5 : 0x1A141413);
        root.addView(dots, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        shade = new View(this);
        root.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // The sun crosses the sky behind everything, on Indian time.
        sunLight = new SunLight(this);
        sunLight.setVisibility(View.GONE);
        sunLight.setPivotX(dp(180));
        sunLight.setPivotY(dp(180));
        sunLight.setScaleX(6f);
        sunLight.setScaleY(6f);
        root.addView(sunLight, new FrameLayout.LayoutParams(dp(360), dp(360), Gravity.TOP | Gravity.START));
        sun = new SunView(this);
        sun.setVisibility(View.GONE);
        FrameLayout.LayoutParams sunLp = new FrameLayout.LayoutParams(dp(300), dp(300));
        sunLp.gravity = Gravity.TOP | Gravity.START;
        root.addView(sun, sunLp);
        moon = new MoonView(this);
        moon.setVisibility(View.GONE);
        root.addView(moon, new FrameLayout.LayoutParams(dp(300), dp(300), Gravity.TOP | Gravity.START));

        lamp = new LampView(this);
        lamp.setOn(!dim);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(92), dp(190));
        lp.gravity = Gravity.BOTTOM | Gravity.START;
        lp.leftMargin = dp(14);
        lp.bottomMargin = dp(8);
        root.addView(lamp, lp);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        backdrop.setLamp(dp(60), dm.heightPixels - dp(8) - dp(190) + dp(190) * 0.185f, dm.heightPixels - dp(8));

        // The pull cord hangs from the rim of the shade; it is the light/dim switch.
        cord = new CordView(this, dp(40), dp(6));
        cord.setLampOn(!dim);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(90), dp(86));
        cp.gravity = Gravity.BOTTOM | Gravity.START;
        cp.leftMargin = dp(51);
        cp.bottomMargin = dp(55);
        root.addView(cord, cp);
        cord.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pullCord(); }
        });

        cable = new SilkView(this);
        cable.setVisibility(View.GONE);
        FrameLayout.LayoutParams kp = new FrameLayout.LayoutParams(dp(4), dm.heightPixels);
        kp.gravity = Gravity.TOP | Gravity.START;
        root.addView(cable, kp);

        person = new SpiderView(this);
        person.setVisibility(View.GONE);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(dp(85), dp(37));
        pp.gravity = Gravity.TOP | Gravity.START;
        root.addView(person, pp);

        // The glows live inside the scrolling content, under the cards, so they scroll with them.
        glow = new GlowLayer(this, dp(CARD_W), dp(CARD_H));
        FrameLayout content = new FrameLayout(this);
        content.setClipChildren(false);
        content.setPadding(0, 0, 0, dp(CLOCK_BAND - 40));
        content.addView(glow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);
        grid.setClipChildren(false);
        content.addView(grid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setSmoothScrollingEnabled(false);   // focus scrolling stays instant
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setFocusable(false);
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // The grid scrolls inside a host that starts below the clock and clips to its own edges, so extra rows
        // slide away under the clock's line instead of over it.
        FrameLayout scrollHost = new FrameLayout(this);
        scrollHost.setClipChildren(true);
        scrollHost.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams hostLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        hostLp.topMargin = dp(CLOCK_BAND);
        root.addView(scrollHost, hostLp);

        addClock(root);
        addCorner(root);
        fixedFaces = faces.size();
        setContentView(root);
        rebuild();
    }

    /** The saved, ordered list of items: package names plus PS for the PlayStation card. */
    private List<String> loadApps() {
        android.content.SharedPreferences sp = getSharedPreferences("room", MODE_PRIVATE);
        if (sp.getInt("layout", 0) < LAYOUT) sp.edit().remove("apps").putInt("layout", LAYOUT).apply();
        String saved = sp.getString("apps", null);
        List<String> out = new ArrayList<>();
        if (saved == null) {
            for (String a : APPS) out.add(a);
        } else {
            for (String a : saved.split(",")) if (a.length() > 0 && !out.contains(a)) out.add(a);
        }
        return out;
    }

    private static String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String a : l) { if (sb.length() > 0) sb.append(','); sb.append(a); }
        return sb.toString();
    }

    private void saveApps() {
        getSharedPreferences("room", MODE_PRIVATE).edit().putString("apps", join(chosen)).apply();
    }

    /** Throws away the cards and builds them again from the saved list. */
    private void rebuild() {
        grid.removeAllViews();
        glow.clear();
        appIcons.clear();
        trimFaces();
        PackageManager pm = getPackageManager();
        List<View> cells = new ArrayList<>();
        for (final String pkg : loadApps()) {
            if (pkg.equals(PS)) {
                ImageView logo = new ImageView(this);
                psLogo = logo;
                logo.setImageResource(getResources().getIdentifier("ps_logo", "drawable", getPackageName()));
                logo.setColorFilter(0xFFFFFFFF);
                cells.add(card(logo, LOGO, 0xFFFF4F9A, new ViceView(this), new Runnable() { public void run() { openInput(); } }));
                continue;
            }
            Drawable icon;
            try { icon = pm.getApplicationIcon(pkg); } catch (PackageManager.NameNotFoundException e) { continue; }
            if (icon == null) continue;
            View face;
            int tint;
            int vector = pkg.equals("com.google.android.youtube.tv") ? getResources().getIdentifier("logo_youtube", "drawable", getPackageName())
                    : pkg.equals("com.netflix.ninja") ? getResources().getIdentifier("logo_netflix", "drawable", getPackageName()) : 0;
            if (vector != 0) {
                // Their own launcher icons are small logos on a white square; the plain logo reads better and matches the PS one.
                ImageView iv = new ImageView(this);
                iv.setImageResource(vector);
                face = iv;
                tint = pkg.equals("com.netflix.ninja") ? 0xFFE50914 : 0xFFFF2020;
            } else {
                IconView iv = new IconView(this, icon);
                appIcons.add(iv);
                final int r = dp(16);
                iv.setClipToOutline(true);
                iv.setOutlineProvider(new ViewOutlineProvider() {
                    public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r); }
                });
                face = iv;
                tint = dominant(icon);
            }
            cells.add(card(face, LOGO, tint, null, new Runnable() { public void run() { launchApp(pkg); } }));
        }

        int cols = 3;
        LinearLayout line = null;
        for (int i = 0; i < cells.size(); i++) {
            if (i % cols == 0) {
                line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(Gravity.CENTER);
                line.setClipChildren(false);
                grid.addView(line, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            line.addView(cells.get(i));
        }
        first = cells.isEmpty() ? editBtn : cells.get(0);
        applyTheme();
        scroll.scrollTo(0, 0);
        if (first != null) first.requestFocus();
    }

    /** Drops the faces of everything but the corner buttons (old cards, editor rows). */
    private void trimFaces() { trimFaces(fixedFaces); }

    private void trimFaces(int keep) {
        while (faces.size() > keep) {
            faces.remove(faces.size() - 1);
            faceHosts.remove(faceHosts.size() - 1);
        }
    }

    /** A greeting in serif and a quiet monospace date line, top left, inside the TV's safe area. */
    private void addClock(FrameLayout root) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        greeting = new TextView(this);
        greeting.setTextColor(textColor());
        greeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        greeting.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        greeting.setIncludeFontPadding(false);
        greeting.setSingleLine(true);
        greeting.setEllipsize(null);
        stamp = new TextView(this);
        stamp.setTextColor(muted());
        stamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        stamp.setLetterSpacing(0.12f);
        stamp.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(8);
        box.addView(greeting);
        box.addView(stamp, sp);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                dp(660), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        p.setMargins(dp(48), dp(30), 0, 0);
        root.addView(box, p);
        clockBox = box;
        tick();
    }

    /** Light or dim, whichever you last chose (light the first time). */
    private boolean wantDim() {
        return nightNow() && getSharedPreferences("room", MODE_PRIVATE).getBoolean("dim", false);
    }

    /**
     * Someone walks in from the bottom right, pulls the lamp cord (this is when the light/dim switch
     * happens) and walks off again. Ten seconds; presses of the cord during that time are ignored.
     */
    private void pullCord() {
        if (walking) return;
        walking = true;
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        final float screenH = dm.heightPixels;
        final float w = dp(85);
        final float s = w / SpiderView.CW;
        final float bodyTop = SpiderView.Y0 * s;                         // where the thread ties on
        final float x = dp(96) - w / 2f;                                 // hangs straight above the cord's bead
        final float beadY = screenH - dp(96);
        final float yStart = -dp(37) - dp(30);
        final float hang = dp(150);                                      // he stops partway down first
        final float land = beadY - dp(6) - bodyTop;                      // and then drops onto the bead
        final float bob = dp(7), pullMax = dp(10);
        person.setLampOn(!dim);
        person.setBody(Mood.spiderBody());
        person.setPivotX(w / 2f);
        person.setPivotY(bodyTop);
        person.setRotation(0f);
        person.look(17.25f, 17.25f);
        person.setTranslationX(x);
        person.setTranslationY(yStart);
        person.setVisibility(View.VISIBLE);
        cable.setLampOn(!dim);
        cable.setTranslationX(dp(96) - dp(2));
        cable.setTranslationY(-screenH);
        cable.setVisibility(View.VISIBLE);
        final boolean[] toggled = {false};
        final int[] look = {0};
        // 0-1.5 s he drops from the top and stops partway; 1.5-3.3 s he bobs and looks around, then eyes the cord;
        // 3.3-4.0 s he rappels down onto the bead; 4.0-4.7 s his weight yanks the cord (lamp flips ~4.35 s);
        // 5.1-6.9 s he climbs back up the thread.
        walkAnim = ValueAnimator.ofFloat(0f, 6900f);
        walkAnim.setDuration(6900);
        walkAnim.setInterpolator(new LinearInterpolator());
        walkAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                float ms = (Float) a.getAnimatedValue();
                float y = hang, pull = 0f, rot = 0f;
                int wantLook = look[0];
                if (ms < 1500f) {                       // dropping on the thread, slowing down
                    float t = ms / 1500f;
                    y = yStart + (hang - yStart) * (1f - (1f - t) * (1f - t) * (1f - t));
                    rot = 4f * (float) (Math.exp(-2.0 * t) * Math.sin(9 * t));
                } else if (ms < 3300f) {                // bobbing like the pen, glancing right, left, then down at the cord
                    float t = (ms - 1500f) / 2000f;
                    y = hang - bob * (0.5f - 0.5f * (float) Math.cos(2 * Math.PI * t));
                    wantLook = ms < 2100f ? 1 : ms < 2700f ? 2 : 3;
                } else if (ms < 4000f) {                // lowers himself onto the bead, speeding up
                    float t = (ms - 3300f) / 700f;
                    y = hang + (land - hang) * t * t;
                    wantLook = 3;
                } else if (ms < 5100f) {                // his weight pulls the cord down, then lets it spring back
                    y = land;
                    if (ms < 4700f) {
                        float p = (ms - 4000f) / 700f;
                        pull = (p < 0.5f ? p / 0.5f : 1f - (p - 0.5f) / 0.5f) * pullMax;
                        if (p >= 0.5f && !toggled[0]) {
                            toggled[0] = true;
                            toggleTheme();
                            cable.setLampOn(!dim);
                        }
                    }
                    y = land + pull;
                    wantLook = ms < 4700f ? 3 : 4;
                } else {                                // climbs back up, speeding up
                    float t = (ms - 5100f) / 1800f;
                    y = land + (yStart - land) * t * t;
                    wantLook = 4;
                }
                if (wantLook != look[0]) {
                    look[0] = wantLook;
                    person.look(wantLook == 1 ? 17.25f : wantLook == 2 ? 9.55f : wantLook == 3 ? 11f : 13.4f,
                            wantLook == 3 ? 21f : wantLook == 4 ? 11f : 17.25f);
                }
                person.setTranslationY(y);
                person.setRotation(rot);
                cable.setTranslationY(y + bodyTop - screenH);
                cord.setPull(pull);
            }
        });
        walkAnim.addListener(new android.animation.AnimatorListenerAdapter() {
            public void onAnimationEnd(android.animation.Animator a) {
                person.setVisibility(View.GONE);
                cable.setVisibility(View.GONE);
                person.setRotation(0f);
                person.setTranslationY(0f);
                cord.setPull(0f);
                walking = false;
            }
        });
        walkAnim.start();
    }

    private void toggleTheme() {
        dim = !dim;
        getSharedPreferences("room", MODE_PRIVATE).edit().putBoolean("dim", dim).apply();
        applyTheme();
    }

    private void applyTheme() {
        greeting.setTextColor(textColor());
        stamp.setTextColor(muted());
        dots.setColor(dim ? 0x16FAF9F5 : 0x1A141413);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                dim ? new int[]{0x66000000, 0x00000000, 0x00000000, 0x80000000} : new int[]{0x00000000, 0x00000000, 0x00000000, 0x14000000}));
        glow.setLevels(dim ? 0.28f : 0.16f, dim ? 0.85f : 0.55f);
        for (IconView v : appIcons) v.setLift(dim);
        for (int i = 0; i < faces.size(); i++) paintFace(faces.get(i), faceHosts.get(i).isFocused());
        backdrop.setDim(dim);
        updateSky();
        lamp.setOn(!dim);
        editIcon.setTint(dim ? textColor() : ACCENT);
        cord.setLampOn(!dim);
        person.setLampOn(!dim);
        for (Runnable r : editPaint) r.run();
        if (editor != null) editor.setBackgroundColor(scrim());
    }

    private void paintFace(GradientDrawable g, boolean lit) {
        g.setColor(lit ? cardLit() : cardFace());
        g.setStroke(lit ? dp(2) : dp(1), lit ? ACCENT : border());
    }

    // Bengaluru; change these to move the sunrise and sunset to another city.
    private static final double LAT = 12.97, LON = 77.59;

    /** Minutes after midnight in India (or the test time), and today's {sunrise, sunset}. */
    private int[] sunTimes(java.util.Calendar c) { return Solar.riseSet(c, LAT, LON); }

    private int nowIst(java.util.Calendar c) {
        return sunTest >= 0 ? sunTest : c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE);
    }

    /** Between sunset and sunrise in India. By day there is no lamp and no dim mode. */
    private boolean nightNow() {
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        int[] rs = sunTimes(c);
        int now = nowIst(c);
        return rs != null && (now < rs[0] || now >= rs[1]);
    }

    /** Show the lamp only at night: it fades in at sunset and is gone by day. */
    private void applyDayNight(boolean night) {
        if (night == nightShown) return;
        boolean running = lamp.getWidth() > 0;
        nightShown = night;
        backdrop.setLampLit(night);
        lamp.setVisibility(night ? View.VISIBLE : View.GONE);
        cord.setVisibility(night && editor == null ? View.VISIBLE : View.GONE);
        if (night && running) {                                       // the lamp comes on at sunset
            lamp.setAlpha(0f);
            lamp.animate().alpha(1f).setDuration(3000).start();
            cord.setAlpha(0f);
            cord.animate().alpha(1f).setDuration(3000).start();
        } else {
            lamp.setAlpha(1f);
            cord.setAlpha(1f);
        }
        if (!night && walkAnim != null && walking) walkAnim.cancel();
    }

    /** The sun crosses the sky by day, the moon by night, on Indian time. */
    private void updateSky() {
        if (sun == null || moon == null || sunLight == null) return;
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        int now = nowIst(c);
        int[] rs = sunTimes(c);
        boolean night = rs != null && (now < rs[0] || now >= rs[1]);
        applyDayNight(night);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        float horizon = dm.heightPixels * 0.40f;
        if (rs == null) { sun.setVisibility(View.GONE); sunLight.setVisibility(View.GONE); moon.setVisibility(View.GONE); return; }
        int dayLen = rs[1] - rs[0], nightLen = 1440 - dayLen;
        // the sun, with a little twilight either side
        float f = (now - rs[0]) / (float) dayLen;
        if (now < rs[0] - 20 || now > rs[1] + 20) {
            sun.setVisibility(View.GONE);
            sunLight.setVisibility(View.GONE);
        } else {
            float fc = Math.max(0f, Math.min(1f, f));
            float apex = dm.heightPixels * 0.15f;
            float high = (float) Math.sin(Math.PI * fc);
            float edge = Math.max(0f, Math.min(1f, Math.min((f + 0.04f) / 0.08f, (1.04f - f) / 0.08f)));
            float k = high * high * (3f - 2f * high);
            int low = 0xFFFF7A3D, noon = 0xFFFFEDB0;
            int col = 0xFF000000 | ((int) (((low >> 16 & 255) * (1 - k)) + ((noon >> 16 & 255) * k)) << 16)
                    | ((int) (((low >> 8 & 255) * (1 - k)) + ((noon >> 8 & 255) * k)) << 8)
                    | (int) (((low & 255) * (1 - k)) + ((noon & 255) * k));
            sun.setSun(col, edge * (dim ? 0.55f : 0.9f));
            sun.setTranslationX(dm.widthPixels * (0.10f + 0.80f * fc) - dp(150));
            sun.setTranslationY(horizon - high * (horizon - apex) - dp(150));
            sun.setVisibility(edge > 0.02f ? View.VISIBLE : View.GONE);
            // the light the sun throws on the room: warm orange near the horizon, pale at noon, strongest when it is high
            int lowL = 0xFFFF9A5A, noonL = 0xFFFFEBB0;
            int colL = 0xFF000000 | ((int) (((lowL >> 16 & 255) * (1 - k)) + ((noonL >> 16 & 255) * k)) << 16)
                    | ((int) (((lowL >> 8 & 255) * (1 - k)) + ((noonL >> 8 & 255) * k)) << 8)
                    | (int) (((lowL & 255) * (1 - k)) + ((noonL & 255) * k));
            sunLight.setLight(colL, edge * (0.55f + 0.45f * high));
            sunLight.setTranslationX(dm.widthPixels * (0.10f + 0.80f * fc) - dp(180));
            sunLight.setTranslationY(horizon - high * (horizon - apex) - dp(180));
            sunLight.setVisibility(edge > 0.02f && !dim ? View.VISIBLE : View.GONE);
        }
        // the moon, across the whole night
        int since = now >= rs[1] ? now - rs[1] : now + 1440 - rs[1];
        float g = since / (float) nightLen;
        float gc = Math.max(0f, Math.min(1f, g));
        float mhigh = (float) Math.pow(Math.sin(Math.PI * gc), 2.5);   // flatter near the horizon, so it clears the clock
        float medge = Math.max(0f, Math.min(1f, Math.min((g + 0.03f) / 0.06f, (1.03f - g) / 0.06f)));
        float mhorizon = dm.heightPixels * 0.27f, mapex = dm.heightPixels * 0.11f;   // above the icons, so it is never hidden
        if (night && since >= 0 && since <= nightLen) {
            moon.set(MoonView.phaseNow(), medge * 0.7f);
            moon.setTranslationX(dm.widthPixels * (0.10f + 0.80f * gc) - dp(150));
            moon.setTranslationY(mhorizon - mhigh * (mhorizon - mapex) - dp(150));
            moon.setVisibility(medge > 0.02f ? View.VISIBLE : View.GONE);
        } else {
            moon.setVisibility(View.GONE);
        }
    }

    private static final String[][] GREETINGS = {
        // 0 night (9 pm - 5 am)
        {"Still up?", "Night owl hours", "Winding down?", "One more episode?", "Late one tonight", "Good night", "Quiet hours"},
        // 1 morning
        {"Good morning", "Morning", "Rise and shine", "Fresh start", "Hello, early bird", "Coffee first, then TV?", "A new day"},
        // 2 afternoon
        {"Good afternoon", "Afternoon", "Hope your day is going well", "Take a breather", "Lunch break?", "Hello again"},
        // 3 evening
        {"Good evening", "Evening", "Time to unwind", "Welcome home", "Movie night?", "Settle in"},
    };

    private int hourNow() {
        return sunTest >= 0 ? sunTest / 60 : java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
    }

    private int periodNow() {
        int h = hourNow();
        return h < 5 ? 0 : h < 12 ? 1 : h < 17 ? 2 : h < 21 ? 3 : 0;
    }

    /** A new greeting each time you come back to the launcher, never the same one twice in a row. */
    private void pickGreeting() {
        greetingPeriod = periodNow();
        java.util.ArrayList<String> pool = new java.util.ArrayList<String>(java.util.Arrays.asList(GREETINGS[greetingPeriod]));
        int dow = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        if (dow == java.util.Calendar.FRIDAY) { pool.add("Happy Friday"); pool.add("TGIF"); }
        else if (dow == java.util.Calendar.SATURDAY) { pool.add("Happy Saturday"); pool.add("Weekend mode"); }
        else if (dow == java.util.Calendar.SUNDAY) { pool.add("Lazy Sunday"); pool.add("Happy Sunday"); }
        else if (dow == java.util.Calendar.MONDAY) { pool.add("Happy Monday"); }
        else if (dow == java.util.Calendar.WEDNESDAY) { pool.add("Midweek"); }
        String pick;
        int tries = 0;
        do { pick = pool.get(new java.util.Random().nextInt(pool.size())); } while (pick.equals(greetingText) && pool.size() > 1 && ++tries < 8);
        greetingText = pick;
        if (greeting != null) greeting.setText(greetingText);
    }

    private void tick() {
        updateSky();
        if (wantDim() != dim) { dim = wantDim(); if (first != null) applyTheme(); }
        java.util.Calendar c = java.util.Calendar.getInstance();
        if (greetingText.length() == 0 || periodNow() != greetingPeriod) pickGreeting();   // otherwise it stays until you come back
        if (!greetingText.contentEquals(greeting.getText())) greeting.setText(greetingText);
        stamp.setText(String.format(java.util.Locale.ENGLISH, "%tA \u00B7 %<td %<tb \u00B7 %<tl:%<tM %<Tp",
                c, c).toUpperCase(java.util.Locale.ENGLISH).replace("AM", "AM").replace("PM", "PM"));
    }

    /** Theme, Settings, add/remove apps and Play Store, small, top right. */
    private void addCorner(FrameLayout root) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setClipChildren(false);
        cornerBox = box;
        PackageManager pm = getPackageManager();
        try {
            box.addView(smallButton(pm.getApplicationIcon("com.android.tv.settings"), new Runnable() {
                public void run() { start(new Intent(Settings.ACTION_SETTINGS)); }
            }));
        } catch (PackageManager.NameNotFoundException e) { /* none */ }
        editIcon = getResources().getDrawable(getResources().getIdentifier("edit_apps", "drawable", getPackageName()), null).mutate();
        editIcon.setTint(dim ? textColor() : ACCENT);
        editBtn = smallButton(editIcon, new Runnable() {
            public void run() { openEditor(); }
        });
        box.addView(editBtn);
        try {
            box.addView(smallButton(pm.getApplicationIcon("com.android.vending"), new Runnable() {
                public void run() { launchApp("com.android.vending"); }
            }));
        } catch (PackageManager.NameNotFoundException e) { /* none */ }
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        p.setMargins(0, dp(30), dp(48), 0);
        root.addView(box, p);
    }

    private View smallButton(Drawable icon, final Runnable action) {
        final FrameLayout b = new FrameLayout(this);
        final GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(12));
        faces.add(shape);
        faceHosts.add(b);
        paintFace(shape, false);
        b.setBackground(shape);
        b.setFocusable(true);
        b.setClickable(true);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        b.addView(iv, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean has) {
                paintFace(shape, has);
                
            }
        });
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { action.run(); } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(46), dp(46));
        lp.setMargins(dp(8), 0, dp(8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    /** A flat dark card with a hairline border, the icon centred. Focus: accent border, lift, and the app's own glow. */
    private View card(View icon, int iconDp, int color, View art, final Runnable action) {
        final FrameLayout c = new FrameLayout(this);
        c.setClipChildren(false);
        c.setFocusable(true);
        c.setClickable(true);

        final GradientDrawable face = new GradientDrawable();
        face.setCornerRadius(dp(18));
        faces.add(face);
        faceHosts.add(c);
        paintFace(face, false);
        final View plate = new View(this);
        plate.setBackground(face);
        c.addView(plate, new FrameLayout.LayoutParams(dp(CARD_W), dp(CARD_H), Gravity.CENTER));
        if (art != null) {
            final int ar = dp(16);
            art.setOutlineProvider(new ViewOutlineProvider() {
                public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), ar); }
            });
            art.setClipToOutline(true);
            c.addView(art, new FrameLayout.LayoutParams(dp(CARD_W - 4), dp(CARD_H - 4), Gravity.CENTER));
        }
        c.addView(icon, new FrameLayout.LayoutParams(dp(iconDp), dp(iconDp), Gravity.CENTER));
        glow.add(c, harmonize(color));

        c.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean has) {
                glow.setFocused(v, has);
                paintFace(face, has);
                
            }
        });
        c.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { action.run(); } });
        c.setLayoutParams(new LinearLayout.LayoutParams(dp(CARD_W + 28), dp(CARD_H + 28)));
        return c;
    }

    /** Pulls each app's colour toward a soft terracotta clay (a touch warm) so the glows sit together. */
    private static int harmonize(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.min(0.72f, hsv[1] * 0.8f);
        hsv[2] = Math.min(1f, Math.max(hsv[2], 0.8f));
        int soft = Color.HSVToColor(hsv);
        int violet = 0xFFD97757;
        int r = (Color.red(soft) * 65 + Color.red(violet) * 35) / 100;
        int g = (Color.green(soft) * 65 + Color.green(violet) * 35) / 100;
        int b = (Color.blue(soft) * 65 + Color.blue(violet) * 35) / 100;
        return Color.rgb(r, g, b);
    }

    private static int darken(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 0.8f);
        hsv[2] = 0.22f;
        return Color.HSVToColor(hsv);
    }

    /** The colour an icon is mostly "about": a saturation-weighted average, brightened for glow. */
    private static int dominant(Drawable d) {
        int s = 32;
        Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        d.setBounds(0, 0, s, s);
        d.draw(cv);
        float r = 0, g = 0, b = 0, w = 0;
        float[] hsv = new float[3];
        for (int y = 0; y < s; y++) for (int x = 0; x < s; x++) {
            int px = bm.getPixel(x, y);
            if (Color.alpha(px) < 128) continue;
            Color.colorToHSV(px, hsv);
            float wt = hsv[1] * hsv[1] * hsv[2];
            if (wt < 0.02f) continue;
            r += Color.red(px) * wt; g += Color.green(px) * wt; b += Color.blue(px) * wt; w += wt;
        }
        bm.recycle();
        if (w == 0) return 0xFF8FA3C8;
        Color.RGBToHSV((int) (r / w), (int) (g / w), (int) (b / w), hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.1f);
        hsv[2] = Math.max(hsv[2], 0.85f);
        return Color.HSVToColor(hsv);
    }

    private void launchApp(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) { Toast.makeText(this, "Not installed", Toast.LENGTH_SHORT).show(); return; }
        start(i);
    }

    private void openInput() {
        start(new Intent(this, PsActivity.class));
    }

    private void start(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open", Toast.LENGTH_SHORT).show();
        }
    }

    private int scrim() { return dim ? 0xF2141413 : 0xF2A89570; }

    private static final class Entry {
        String key, label; Drawable icon;
    }

    private static Entry psEntry() {
        Entry e = new Entry();
        e.key = PS; e.label = "PlayStation";
        return e;
    }

    /** Every launchable app on the TV, once per package, plus anything already chosen that is still installed. */
    private List<Entry> choices() {
        PackageManager pm = getPackageManager();
        Map<String, Entry> map = new LinkedHashMap<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            List<ResolveInfo> found;
            try { found = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0); }
            catch (Exception e) { continue; }
            if (found == null) continue;
            for (ResolveInfo ri : found) {
                if (ri.activityInfo == null || ri.activityInfo.applicationInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg == null || pkg.equals(getPackageName()) || map.containsKey(pkg)) continue;
                if (!ri.activityInfo.enabled || !ri.activityInfo.applicationInfo.enabled) continue;
                Entry e = new Entry();
                e.key = pkg;
                CharSequence l = null;
                try { l = ri.loadLabel(pm); } catch (Exception x) { /* none */ }
                e.label = l == null || l.length() == 0 ? pkg : l.toString();
                try { e.icon = ri.loadIcon(pm); } catch (Exception x) { /* none */ }
                map.put(pkg, e);
            }
        }
        for (String pkg : chosen) {
            if (pkg.equals(PS) || map.containsKey(pkg)) continue;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                Entry e = new Entry();
                e.key = pkg;
                CharSequence l = pm.getApplicationLabel(ai);
                e.label = l == null || l.length() == 0 ? pkg : l.toString();
                e.icon = pm.getApplicationIcon(pkg);
                map.put(pkg, e);
            } catch (Exception x) { /* gone */ }
        }
        // Chosen ones first in their saved order, then the rest by name.
        List<Entry> out = new ArrayList<>();
        for (String k : chosen) {
            if (k.equals(PS)) out.add(psEntry());
            else if (map.containsKey(k)) out.add(map.remove(k));
        }
        List<Entry> rest = new ArrayList<>(map.values());
        if (!chosen.contains(PS)) rest.add(psEntry());
        Collections.sort(rest, new Comparator<Entry>() {
            public int compare(Entry x, Entry y) { return x.label.compareToIgnoreCase(y.label); }
        });
        out.addAll(rest);
        return out;
    }

    private TextView text(String s, int sp, boolean mono) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(mono ? Typeface.MONOSPACE : Typeface.create("serif", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        if (mono) t.setLetterSpacing(0.12f);
        return t;
    }

    private void openEditor() {
        if (editor != null) return;
        faceMark = faces.size();
        chosen.clear();
        chosen.addAll(loadApps());
        savedAtOpen = join(chosen);
        editPaint.clear();

        editor = new FrameLayout(this);
        editor.setBackgroundColor(scrim());
        editor.setClickable(true);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        final TextView title = text("Edit apps", 36, false);
        final TextView hint = text("SELECT TO ADD OR REMOVE", 14, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(8);
        head.addView(title);
        head.addView(hint, hp);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        p.setMargins(dp(48), dp(30), 0, 0);
        editor.addView(head, p);
        editPaint.add(new Runnable() { public void run() { title.setTextColor(textColor()); hint.setTextColor(muted()); } });

        FrameLayout done = plate(dp(12));
        final TextView dt = text("DONE", 14, true);
        done.addView(dt, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        editPaint.add(new Runnable() { public void run() { dt.setTextColor(ACCENT); } });
        done.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { closeEditor(); } });
        done.setId(View.generateViewId());
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(dp(120), dp(46), Gravity.TOP | Gravity.END);
        dlp.setMargins(0, dp(30), dp(56), 0);
        editor.addView(done, dlp);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(4), dp(6), dp(4), dp(6));
        View firstRow = null;
        for (Entry e : choices()) {
            View row = editRow(e);
            if (firstRow == null) firstRow = row;
            list.addView(row);
        }
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setSmoothScrollingEnabled(false);
        sv.setClipToPadding(false);
        sv.setFocusable(false);
        sv.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        slp.setMargins(dp(120), dp(110), dp(120), dp(20));
        editor.addView(sv, slp);

        if (firstRow != null) {
            firstRow.setNextFocusUpId(done.getId());
            done.setNextFocusDownId(firstRow.getId());
        }
        root.addView(editor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Hidden views cannot take focus, so the D-pad stays inside the editor.
        scroll.setVisibility(View.INVISIBLE);
        cornerBox.setVisibility(View.INVISIBLE);
        cord.setVisibility(View.INVISIBLE);
        clockBox.setVisibility(View.INVISIBLE);
        for (Runnable r : editPaint) r.run();
        (firstRow != null ? firstRow : done).requestFocus();
    }

    /** A rounded face that takes focus and follows the theme; the caller adds the content. */
    private FrameLayout plate(int radius) {
        final FrameLayout b = new FrameLayout(this);
        final GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        faces.add(shape);
        faceHosts.add(b);
        paintFace(shape, false);
        b.setBackground(shape);
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean has) { paintFace(shape, has); }
        });
        return b;
    }

    private View editRow(final Entry e) {
        final FrameLayout r = plate(dp(14));
        r.setId(View.generateViewId());
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(16), 0, dp(20), 0);
        ImageView iv = new ImageView(this);
        int vec = e.key.equals("com.google.android.youtube.tv") ? getResources().getIdentifier("logo_youtube", "drawable", getPackageName())
                : e.key.equals("com.netflix.ninja") ? getResources().getIdentifier("logo_netflix", "drawable", getPackageName()) : 0;
        if (e.key.equals(PS)) {
            iv.setImageResource(getResources().getIdentifier("ps_logo", "drawable", getPackageName()));
            iv.setColorFilter(0xFFFFFFFF);
            iv.setPadding(dp(5), dp(5), dp(5), dp(5));
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF2B0F63, 0xFFB8237F, 0xFFFF9A4D});   // the same sunset as the card
            bg.setCornerRadius(dp(8));
            iv.setBackground(bg);
        } else if (vec != 0) {
            iv.setImageResource(vec);                                 // the same logos as the grid, not the white launcher squares
        } else {
            iv.setImageDrawable(e.icon != null ? e.icon : getPackageManager().getDefaultActivityIcon());
        }
        h.addView(iv, new LinearLayout.LayoutParams(dp(40), dp(40)));
        final TextView label = text(e.label, 20, false);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(16);
        h.addView(label, lp);
        final TextView state = text("", 14, true);
        h.addView(state);
        r.addView(h, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final Runnable paint = new Runnable() {
            public void run() {
                boolean on = chosen.contains(e.key);
                label.setTextColor(textColor());
                state.setText(on ? "ADDED" : "NOT ADDED");
                state.setTextColor(on ? ACCENT : muted());
            }
        };
        editPaint.add(paint);
        r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!chosen.remove(e.key)) chosen.add(e.key);   // new ones go to the end
                saveApps();
                paint.run();
            }
        });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
        rp.setMargins(0, dp(4), 0, dp(4));
        r.setLayoutParams(rp);
        return r;
    }

    private void closeEditor() {
        if (editor == null) return;
        root.removeView(editor);
        editor = null;
        editPaint.clear();
        trimFaces(faceMark);                 // only the editor rows; the grid cards stay so the theme still reaches them
        scroll.setVisibility(View.VISIBLE);
        cornerBox.setVisibility(View.VISIBLE);
        cord.setVisibility(nightShown ? View.VISIBLE : View.GONE);
        clockBox.setVisibility(View.VISIBLE);
        if (!join(chosen).equals(savedAtOpen)) rebuild();
        else editBtn.requestFocus();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onStart() {
        super.onStart();
        clock.post(ticker);
    }

    @Override
    protected void onStop() {
        clock.removeCallbacks(ticker);
        super.onStop();
    }

    // Apps that sit in memory but nobody uses from here; the TV has little RAM, and swapping is what makes it lag.
    private static final String[] TRIM = {
        "com.google.android.apps.tv.launcherx", "com.google.android.katniss", "com.android.vending",
        "com.android.tv.settings", "com.google.android.permissioncontroller", "com.mitv.livetv",
    };
    private final Runnable trim = new Runnable() {
        public void run() {
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            for (String pkg : TRIM) am.killBackgroundProcesses(pkg);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Heal.ensure(this);
        if (greeting != null) pickGreeting();          // a fresh greeting each time you come back
        if (first != null && editor == null) first.requestFocus();
        clock.removeCallbacks(trim);
        clock.postDelayed(trim, 2500);
    }

    @Override
    protected void onPause() {
        clock.removeCallbacks(trim);
        if (walkAnim != null) walkAnim.cancel();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("sun")) { sunTest = intent.getIntExtra("sun", -1); updateSky(); tick(); }
        if (editor != null) closeEditor();
        else if (first != null) first.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (editor != null) closeEditor();   // otherwise this is home; nowhere to go back to
    }
}
