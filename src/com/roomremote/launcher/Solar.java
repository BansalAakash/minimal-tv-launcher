package com.roomremote.launcher;

import java.util.Calendar;

/** Sunrise and sunset in Indian Standard Time (UTC+5:30) for a place in India: the standard almanac algorithm. */
final class Solar {
    private Solar() {}

    /** {sunrise, sunset} as minutes after midnight IST, or null if the sun never rises or sets. */
    static int[] riseSet(Calendar cal, double lat, double lon) {
        double rise = time(cal, lat, lon, true), set = time(cal, lat, lon, false);
        if (Double.isNaN(rise) || Double.isNaN(set)) return null;
        return new int[]{(int) Math.round(rise * 60), (int) Math.round(set * 60)};
    }

    private static double time(Calendar cal, double lat, double lon, boolean rising) {
        int n = cal.get(Calendar.DAY_OF_YEAR);
        double lngHour = lon / 15.0;
        double t = n + ((rising ? 6.0 : 18.0) - lngHour) / 24.0;
        double m = 0.9856 * t - 3.289;
        double l = m + 1.916 * Math.sin(Math.toRadians(m)) + 0.020 * Math.sin(Math.toRadians(2 * m)) + 282.634;
        l = mod(l, 360.0);
        double ra = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(l))));
        ra = mod(ra, 360.0);
        ra += Math.floor(l / 90.0) * 90.0 - Math.floor(ra / 90.0) * 90.0;
        ra /= 15.0;
        double sinDec = 0.39782 * Math.sin(Math.toRadians(l));
        double cosDec = Math.cos(Math.asin(sinDec));
        double cosH = (Math.cos(Math.toRadians(90.833)) - sinDec * Math.sin(Math.toRadians(lat))) / (cosDec * Math.cos(Math.toRadians(lat)));
        if (cosH > 1 || cosH < -1) return Double.NaN;
        double h = rising ? 360.0 - Math.toDegrees(Math.acos(cosH)) : Math.toDegrees(Math.acos(cosH));
        h /= 15.0;
        double tt = h + ra - 0.06571 * t - 6.622;
        double ut = mod(tt - lngHour, 24.0);
        return mod(ut + 5.5, 24.0);
    }

    private static double mod(double v, double m) { double r = v % m; return r < 0 ? r + m : r; }
}
