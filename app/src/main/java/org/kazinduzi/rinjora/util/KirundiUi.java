package org.kazinduzi.rinjora.util;

import java.util.Locale;

/**
 * Single source of ALL user-facing Kirundi strings, mirrored verbatim from the
 * prototype's {@code T} and {@code NOMBRES} objects in {@code docs/rinjora.html}.
 *
 * <p>Rule: no user-facing hardcoded English strings anywhere. Everything displayable
 * in the Rinjora game flows through this class so the app stays 100% Kirundi and in
 * lockstep with the reference prototype.
 */
public final class KirundiUi {

    private KirundiUi() {
    }

    // ----- Home -----
    public static final String SLOGAN = "Amayagwa magufi y'Ikirundi";
    public static final String WELCOME = "Kaze ! Hitamwo ico ukunda.";
    public static final String D_SOKWE = "Ibisokozo";
    public static final String D_HERA = "Imyibutsa — Heraheza/Tangura";
    public static final String D_TUJA = "Utujajuro — tube turatwenga";
    public static final String CONTRIB = "✍️ Intererano yawe hano";
    public static final String HIST = "📊 Amateka yawe";
    public static final String ABOUT = "ℹ️ Ivyerekeye Rinjora";
    public static final String F_TAG = "Amayagwa magufi y'Ikirundi";
    public static final String F_CREDIT = "Iciyumviro ca Rivardo Niyonizigiye · "
            + "Cashizwe mu ngiro na Akanyaburunga na Gisabo Tours";

    // ----- Quiz labels -----
    public static final String LAB_SOKWE = "Sokwe !";
    public static final String LAB_HERA = "Heraheza !";
    public static final String SCORE_LAB = "Amanota uronse";
    public static final String PH_SOKWE = "Andika inyishu yawe aha.";
    public static final String PH_HERA = "Heza uyu mugani aha.";
    public static final String CHECK = "Raba ko wabitoye.";
    public static final String SKIP = "Rengana";
    public static final String BACK = "‹ Subira inyuma";
    public static final String GIVE = "Ndaguhaye ! 🤲";
    public static final String NEXT = "Bandanya";
    public static final String QUIT = "Subira ku ntango";
    public static final String ANSWER_INTRO = "Inyishu yari";
    public static final String HERA_INTRO = "Umugani wose ni";
    public static final String LEVEL = "Urugero";

    public static final String[] GOOD = {
            "Urabitoye ! 🎉",
            "Uri intwari ! 💪",
            "Uraciye ubwenge pe ! 🧠✨",
            "Amashi menshi ! 👏"
    };
    public static final String STREAK_MSG = "Amashi menshi cane 👏👏👏";
    public static final String IMPA = "Impa 😉";
    public static final String CONCEDE_MSG = "Ntudebukirwe ! 💪";

    // ----- Tujajure -----
    public static final String J_THINK = "Iyumvire inyishu, uhitemwo 🤔";
    public static final String J_SCORE_LAB = "Ivyo wari uzi";

    // ----- End -----
    public static final String END_TITLE = "Urukino ruraheze !";
    public static final String PERF_TOP = "Turagukeje cane. Uri muri bake bashoboye "
            + "kuronka amanota nk'aya ! Amashi menshi 🎉🔥👏";
    public static final String PERF_MID = "Turagukeje. Ariko ubandanye wiga ibisokozo, "
            + "hanyuma ubitore vyose. 👍📚✨";
    public static final String PERF_LOW = "Wagerageje. Ariko subira kwiga hahaha! 😄📖💪";
    public static final String REPLAY = "Subira ugerageze !";
    public static final String SHARE = "Sangiza abandi";
    public static final String HOME = "Subira ku ntango";

    // ----- Level-up modal -----
    public static final String LVL_CHEER = "Uriko uratsinda neza! 🔥";
    public static final String LVL_Q = "Ushaka gutera intambwe igoye kurusha?";
    public static final String LVL_YES = "Ego 💪";
    public static final String LVL_NO = "Oya";

    // ----- About -----
    public static final String A_TITLE = "Ivyerekeye Rinjora";
    public static final String A_TAG = "Amayagwa magufi y'Ikirundi";
    public static final String A_L1 = "Iciyumviro ca";
    public static final String A_V1 = "Rivardo Niyonizigiye";
    public static final String A_L2 = "Cashizwe mu ngiro na";
    public static final String A_V2 = "Akanyaburunga na Gisabo Tours";

    // ----- History -----
    public static final String H_TITLE = "Amateka yawe";
    public static final String H_SUB = "Ng'aya amanota wironkeye muri Rinjora.";
    public static final String H_TOTAL = "AMANOTA YOSE";
    public static final String H_GAMES = "Incuro umaze gukina";
    public static final String H_BEST = "Amanota menshi";
    public static final String H_EMPTY = "Ntacho urakina.\nTangura umukino kugira ubone amateka yawe ! 🔥";
    public static final String H_RESET = "Futa amateka yose 🗑️";
    public static final String H_ASK = "Vy'ukuri urashaka gufuta vyose ?";
    public static final String H_DONE = "Vyafuswe ! ✅";

    // ----- Mode names / misc -----
    public static final String N_SOKWE = "Sokwe… Niruze !";
    public static final String N_HERA = "Heraheza";
    public static final String N_TUJA = "Tujajure !";
    public static final String N_PLAYED = "incuro";
    public static final String C_COPIED = "Vyakopowe ! 📋";

    // ----- Contribution -----
    public static final String C_TITLE = "Intererano yawe hano";
    public static final String C_SUB = "Uzi igisokozo, umwibutsa canke akajajuro tutari "
            + "dufise ? Twandikire !";
    public static final String C_L1 = "Ni iki utanga ?";
    public static final String C_L2 = "Andika ng'aha";
    public static final String C_L3 = "Inyishu (canke ibindi usigura)";
    public static final String C_L4 = "Izina ryawe (si ngombwa)";
    public static final String C_SEND = "Rungika 📤";
    public static final String C_COPY = "Kopora 📋";
    public static final String C_NOTE = "Tuzobisuzuma tubongere muri Rinjora. Urakoze cane ! 🙏";
    public static final String C_EMPTY = "Andika ikintu mbere yo kurungika.";

    // ----- Types selector -----
    public static final String TYPE_SOKWE = "Igisokozo 🧠";
    public static final String TYPE_HERA = "Umwibutsa 🌾";
    public static final String TYPE_TUJA = "Akajajuro 😂";
    public static final String TYPE_OTHER = "Iyindi ngingo 💡";

    public static final String[] NOMBRES = {
            "Rimwe", "Kabiri", "Gatatu", "Kane", "Gatanu", "Gatandatu",
            "Indwi", "Umunani", "Icenda", "Cumi", "Cumi na rimwe",
            "Cumi na kabiri", "Cumi na gatatu", "Cumi na kane", "Cumi na gatanu",
            "Cumi na gatandatu", "Cumi n'indwi", "Cumi n'umunani", "Cumi n'icenda",
            "Mirongo ibiri"
    };

    /** 1-based ordinal in Kirundi; falls back to the plain number beyond 20. */
    public static String motNombre(int n) {
        return (n >= 1 && n <= NOMBRES.length) ? NOMBRES[n - 1] : String.valueOf(n);
    }

    /** Random good-answer message (prototype {@code pick(T.good)}). */
    public static String goodMessage() {
        int i = (int) (Math.random() * GOOD.length);
        return GOOD[i];
    }

    /** Performance message for a score out of a round length (top≥8, mid≥5). */
    public static String performance(int score, int roundLength) {
        int d = roundLength > 0 ? roundLength : 10;
        double ratio = (double) score / d;
        if (ratio >= 0.8) return PERF_TOP;
        if (ratio >= 0.5) return PERF_MID;
        return PERF_LOW;
    }

    /** Share text (prototype {@code partager}) {@code "Rinjora — <slogan> — x / 10 ⭐"}. */
    public static String shareText(int score, int roundLength) {
        return String.format(Locale.getDefault(),
                "Rinjora — %s — %d / %d ⭐", SLOGAN, score, roundLength);
    }

    /** Contribution copy text (prototype {@code texteContrib}). */
    public static String contributionText(String type, String body, String answer, String who) {
        StringBuilder t = new StringBuilder("RINJORA — ").append(type).append("\n\n").append(body);
        if (answer != null && !answer.trim().isEmpty()) {
            t.append("\n\nInyishu: ").append(answer.trim());
        }
        if (who != null && !who.trim().isEmpty()) {
            t.append("\n\nUwabitanze: ").append(who.trim());
        }
        return t.toString();
    }
}
