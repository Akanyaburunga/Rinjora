package org.kazinduzi.rinjora.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * One-shot cross-activity handoff for the guest→account conversion (plan §5/§6):
 * when a guest hits the per-mode cap (403 {@code requires_registration}) the game
 * fragment stores the mode they were about to play here, then launches the auth
 * host. After register + login, {@code MainActivity} selects that tab
 * ({@link #peek}/{@link #clear}) and the fragment auto-starts the round once the
 * session is a real account. Lightweight prefs; never game state.
 */
public final class PendingGameMode {

    private static final String PREFS = "rinjora_pending";
    private static final String KEY_MODE = "mode";
    private static final String KEY_LEVEL = "level";

    private PendingGameMode() {
    }

    public static void set(Context context, String mode, int level) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MODE, mode)
                .putInt(KEY_LEVEL, level)
                .apply();
    }

    /** Reads without clearing (the auth trip may land anywhere before the retry). */
    public static String peekMode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MODE, null);
    }

    public static int peekLevel(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_LEVEL, 1);
    }

    /** Consumed by the fragment that successfully restarts its pending round. */
    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}