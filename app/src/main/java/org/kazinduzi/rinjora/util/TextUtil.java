package org.kazinduzi.rinjora.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Display-only text helpers for the lenient answer UX (parity plan §1.1).
 *
 * <p>The backend matcher accepts free word order, accents/case-insensitivity,
 * "/"-alternatives, synonyms and small typos. This util is used ONLY to normalise
 * what we <b>echo back</b> to the player (e.g. "we accepted/accepted was …") so the
 * UI looks consistent. It is never used for scoring — the client passes the raw
 * user text through to the API unchanged.
 */
public final class TextUtil {

    private TextUtil() {}

    /** Lowercase, strip accents, collapse punctuation to spaces, trim. */
    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")            // strip combining marks
                .replaceAll("[\\p{Punct}]+", " ")    // punctuation -> space
                .replaceAll("\\s+", " ").trim();
        return n.toLowerCase(Locale.ROOT);
    }

    /** True when the typed text is (a case/accents-insensitive) "ndaguhaye" = I give up. */
    public static boolean isConcede(String raw) {
        return "ndaguhaye".equals(normalize(raw));
    }
}
