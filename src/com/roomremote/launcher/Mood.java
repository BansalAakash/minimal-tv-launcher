package com.roomremote.launcher;

import java.util.Calendar;
import java.util.Random;

/**
 * Something new to look at every day, with no text: the day number seeds a colour mood for the background washes,
 * the style of the faint pattern, and which spider comes to pull the cord. All of it is drawn once per day.
 */
final class Mood {
    // earthy accents: clay, kraft, olive, sky, fig, heather, cactus
    static final int[] PALETTE = {0xFFD97757, 0xFFD4A27F, 0xFF788C5D, 0xFF6A9BCC, 0xFFC46686, 0xFFCBCADB, 0xFFBCD1CA};
    // spider bodies: classic black, plum, deep teal, chestnut, navy
    private static final int[] SPIDERS = {0xFF222222, 0xFF3B2A4A, 0xFF1F3D3A, 0xFF4A2E22, 0xFF2A3550};

    private Mood() {}

    /** Days since 1970 in local time: changes at local midnight. */
    static int day() {
        Calendar c = Calendar.getInstance();
        return (int) ((c.getTimeInMillis() + c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)) / 86400000L);
    }

    /** Three washes away from the lamp: {x, y, radius (all as fractions of the screen), colour, alpha in light, alpha in dim}. */
    static float[][] washes() {
        Random r = new Random(day() * 2654435761L + 12345L);
        float[][] w = new float[3][6];
        int first = r.nextInt(PALETTE.length);
        for (int i = 0; i < 3; i++) {
            w[i][0] = 0.38f + r.nextFloat() * 0.62f;
            w[i][1] = r.nextFloat();
            w[i][2] = 0.30f + r.nextFloat() * 0.25f;
            w[i][3] = PALETTE[(first + i * (2 + r.nextInt(2))) % PALETTE.length];
            w[i][4] = 50f + r.nextFloat() * 40f;
            w[i][5] = 28f + r.nextFloat() * 26f;
        }
        return w;
    }

    /** 0 dots, 1 plus signs, 2 diamonds, 3 rings, 4 dashes. */
    static int pattern() { return Math.floorMod(day(), 5); }

    static int spiderBody() { return SPIDERS[Math.floorMod(day() / 1, SPIDERS.length)]; }
}
