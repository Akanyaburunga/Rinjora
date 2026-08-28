package org.kazinduzi.rinjora.util;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;

public final class AnalyticsHelper {

    private static FirebaseAnalytics instance;

    private AnalyticsHelper() {}

    private static synchronized FirebaseAnalytics analytics(@NonNull Context context) {
        if (instance == null) {
            instance = FirebaseAnalytics.getInstance(context.getApplicationContext());
        }
        return instance;
    }

    public static void logEvent(@NonNull Context context, @NonNull String name) {
        analytics(context).logEvent(name, null);
    }

    public static void logEvent(@NonNull Context context, @NonNull String name, Bundle params) {
        analytics(context).logEvent(name, params);
    }
}
