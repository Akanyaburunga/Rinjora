package org.kazinduzi.rinjora.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Converts ISO-8601 date/time strings returned by the API (e.g. "2026-08-11T09:00:00.000000Z")
 * into human-friendly text. All methods fall back to the raw string when parsing fails so the
 * UI never shows empty values.
 */
public final class DateUtils {

    private DateUtils() {}

    /**
     * "Aug 11, 2026"
     */
    public static String date(String iso) {
        Date d = parse(iso);
        if (d == null) return iso == null ? "" : iso;
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(d);
    }

    /**
     * "Aug 11, 2026 · 9:00 AM"
     */
    public static String dateTime(String iso) {
        Date d = parse(iso);
        if (d == null) return iso == null ? "" : iso;
        return new SimpleDateFormat("MMM d, yyyy \u00b7 h:mm a", Locale.getDefault()).format(d);
    }

    /**
     * "9:00 AM"
     */
    public static String time(String iso) {
        Date d = parse(iso);
        if (d == null) return iso == null ? "" : iso;
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(d);
    }

    /**
     * "Just now", "5m ago", "3h ago", "2d ago", falling back to a full date for older timestamps.
     */
    public static String relative(String iso) {
        Date d = parse(iso);
        if (d == null) return iso == null ? "" : iso;
        long diff = System.currentTimeMillis() - d.getTime();
        long a = Math.abs(diff);
        String result;
        if (a < 60_000) {
            return "Just now";
        } else if (a < 3_600_000) {
            result = (a / 60_000) + "m ago";
        } else if (a < 86_400_000) {
            result = (a / 3_600_000) + "h ago";
        } else if (a < 7L * 86_400_000) {
            result = (a / 86_400_000) + "d ago";
        } else {
            return date(iso);
        }
        return diff < 0 ? "in " + result.replace(" ago", "") : result;
    }

    private static Date parse(String iso) {
        if (iso == null) return null;
        String s = iso.trim();
        if (s.isEmpty()) return null;

        s = s.replace('T', ' ');
        s = s.replaceAll("\\.\\d+", "");                     // strip fractional seconds
        s = s.replaceAll("([+-]\\d{2}):(\\d{2})", "$1$2");  // "+02:00" -> "+0200"
        s = s.trim();

        boolean utc = false;
        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1).trim();
            utc = true;
        }

        String[] formats;
        if (utc) {
            formats = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"};
        } else if (s.matches(".*[+-]\\d{4}$")) {
            formats = new String[]{"yyyy-MM-dd HH:mm:ss Z", "yyyy-MM-dd HH:mm Z"};
        } else if (s.length() <= 10) {
            formats = new String[]{"yyyy-MM-dd"};
        } else {
            formats = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm"};
        }

        TimeZone tz = utc ? TimeZone.getTimeZone("UTC") : TimeZone.getDefault();
        for (String f : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setLenient(false);
                sdf.setTimeZone(tz);
                Date d = sdf.parse(s);
                if (d != null) return d;
            } catch (ParseException ignored) {}
        }
        return null;
    }
}
