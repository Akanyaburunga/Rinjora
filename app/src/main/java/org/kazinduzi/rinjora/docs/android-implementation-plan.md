# Android Frontend Implementation Plan — Rinjora Parity Experience

**Source of truth:** `docs/rinjora.html` (the prototype whose user experience we replicate one screen at a time). A byte-identical copy lives at `app/src/main/java/org/kazinduzi/rinjora/docs/rinjora.html`.
**Backend contract:** `backend-implementation-plan.md` (living in the Laravel backend repo, `docs/`).
**Language/UI:** Java + XML Views (classic Android). No Compose, no Kotlin in the feature path.
**SDK:** `minSdk 23`, `targetSdk` = latest stable (34+). No `java.time` — anything time-based uses `java.util`/`SimpleDateFormat` (existing convention).
**Networking:** Retrofit 2 + OkHttp + Gson, `ApiEnvelope { success, data }` JSON envelope.
**Auth:** Sanctum bearer token in `AuthTokenStore`; 401 → force re-login.
**Package:** `org.kazinduzi.rinjora` (existing app conventions — not a greenfield `com.kazinduzi.app`).

This document is the **native Android companion** plan. It mirrors the prototype's 100% Kirundi UI and screen flows while consuming the backend's game-round/history/contribution APIs (defined in the backend plan). Nothing here invents new endpoints; it consumes exactly the round API the backend contract exposes.

---

## 0. Target user experience (from `docs/rinjora.html`) — what we are building

The prototype is a single-page mobile web app (`max-width: 440px`). Its screens, flows, and behaviour are the acceptance spec:

| Screen | Purpose | Key elements |
|--------|---------|--------------|
| `s-home` | Home | Hero SVG, brand **Rinjora**, slogan, **3 game cards** (Sokwe… Niruze ! / Heraheza / Tujajure !) with icons + short labels, footer nav **History / Contribute / About**. |
| `s-quiz` | Sokwe + Heraheza | topbar (progress bar, ⭐ score pill, 🔥 streak pill shown only when streak>0), eyebrow (mode label, count "Rimwe / Cumi", level badge), riddle/proverb card, free-text input, Raba ko wabitoye / Ndaguhaye ! / Rengana / ‹ Subira inyuma / Bandanya / Subira ku ntango, feedback card (ok/no), reveal of the answer only on solve or concede. |
| `s-joke` | Tujajure | setup card, "think" prompt, 4 option buttons, feedback card, Bandanya / Subira ku ntango. |
| `s-end` | End | 🎊, "Urukino ruraheze !", scorecard `score / round`, performance message (top/mid/low), Replay / Share / Home. |
| `s-hist` | History "Amateka" | big total, duo (#games, #best), 3 per-mode rows (icon, name, #games, ⭐points), reset, quit. |
| `s-about` | About "Ivyerekeye Rinjora" | gradient card, logo, tagline, credits. |
| `s-contrib` | Contribution "Intererano" | type selector (Igisokozo/Umwibutsa/Akajajuro/other), body, answer, name, Send/Copy, note. |
| `lvlup` | Level-up modal | mascot, "Uriko uratsinda neza! 🔥", "Ushaka gutera intambwe igoye kurusha?", Yes/No. |
| toast | transient | bottom toast for copy / reset confirmations. |

**Visual language to port:** Fredoka + Nunito fonts; ivory `#FFF6E9` bg; green `#17915A` / gold `#F6B21A` / red `#E14B4B`; choco `#6B4226` / terra `#B4441F` / ochre `#D89B4A`; rounded ~20–26dp cards; hard shadows (`0 5px 0`); `imigongo` striped top/bottom bands; emoji confetti; pop / fadeUp / shake animations; `prefers-reduced-motion` honoured.

---

## 1. Current state vs. target (gap analysis within this Android app)

The current app is a **list-and-single-play** experience; the prototype is a **round-of-10, tiered-level, per-mode-score** game. Each row is a gap to close (mirroring the backend plan's gap table).

| Prototype concept (rinjora.html) | Current Android app | Gap to close |
|---|---|---|
| One Home with 3 mode cards | `RinjoraHomeActivity` shows a legacy summary + many feature buttons, not 3 mode cards | Rebuild Home as the 3-card menu + footer (History/Contribute/About). |
| Round of 10 (`ROUND_SIZE=10`) | `RinjoraPlayActivity` lists all riddles; `RinjoraPlayRiddleActivity` plays a single riddle; proverb detail plays one proverb; `RinjoraJokeRoundActivity` loops one joke at a time | New round-of-10 game screens driven by the round API; replace list browsing with mode-launch. |
| Tiered levels (pool/5, `has_next`) | No round concept; difficulty is a per-item filter | Consume round `level`, `level_available`, `next_level` and drive the level-up modal. |
| In-round `⭐ score` + `🔥 streak` pills | Reputation/streak are lifetime summary stats on Home, not in-round | Add in-round score + streak pills to the game screen. |
| Level-up modal when `score≥8` + harder tier | n/a | `LevelUpDialog` from `complete`'s `level_available`/`next_level`. |
| Feedback card + concede + reveal + free-text input | `AnswerView` (shared lenient input, F1) exists but is wired to single-item flows | Reuse `AnswerView` inside the quiz screen; drive conceded/correct reveal states server-side. |
| Tujajure: setup + 4 shuffled options + highlight | `RinjoraJokeRoundActivity` already implements 4 options + green/red highlight + reveal | Re-skin to the prototype's `s-joke` layout (setup card, think prompt, Bandanya-only-advance, in-round score). |
| End screen (score/round, perf, Replay/Share/Home) | Play screens end with a "Done" and return to list | New dedicated End screen + share text. |
| History (total/games/best/per-mode) | `RinjoraHistoryActivity` shows 3-mode stats + recent-attempt entries | Replace with the prototype's exact History screen (big total, duo, 3 rows) from `GET /api/games/history`. |
| Contribution (type selector + send/copy) | `RinjoraContributeActivity` exists | Align form fields/labels + copy format to the prototype's `texteContrib()` and `POST /api/contributions`. |
| 100% Kirundi UI (`T` object) | Mixed English strings scattered | Consolidate ALL display strings into `KirundiUi` mirroring `T` + `NOMBRES` verbatim. |
| Animations (confetti, pop, shake, fadeUp) | Minimal | Add `ConfettiView`, feedback pop, input shake, screen fadeUp; honour reduced-motion. |

**Design decision (matches backend plan):** rounds/live state are **server-owned**. The Android client is stateless and resumable; it never caches puzzle answers or solved state for the quiz experience. The local persistence used elsewhere (`Rinjora*Snapshot` ObjectBox entities) is **out of scope** for the new round screens unless needed for minimal UX affordances; parity wins over offline-first here.

---

## 2. Kirundi UI strings & numbers — single source

Mirror the prototype's `T` and `NOMBRES` **verbatim** in one class so the app is 100% Kirundi and stays in lockstep with `rinjora.html`. No user-facing hardcoded English strings anywhere.

`app/src/main/java/org/kazinduzi/rinjora/util/KirundiUi.java`
```java
public final class KirundiUi {
    public static final String SLOGAN   = "Amayagwa magufi y'Ikirundi";
    public static final String WELCOME  = "Kaze ! Hitamwo ico ukunda.";
    public static final String dSokwe   = "Ibisokozo";
    public static final String dHera    = "Imyibutsa — Heraheza/Tangura";
    public static final String dTuja    = "Utujajuro — tube turatwenga";
    public static final String contrib  = "✍️ Intererano yawe hano";
    public static final String labSokwe = "Sokwe !";
    public static final String labHera  = "Heraheza !";
    public static final String scoreLab = "Amanota uronse";
    public static final String phSokwe  = "Andika inyishu yawe aha.";
    public static final String phHera   = "Heza uyu mugani aha.";
    public static final String check    = "Raba ko wabitoye.";
    public static final String skip     = "Rengana";
    public static final String back     = "‹ Subira inyuma";
    public static final String give     = "Ndaguhaye ! 🤲";
    public static final String next     = "Bandanya";
    public static final String quit     = "Subira ku ntango";
    public static final String answerIntro = "Inyishu yari";
    public static final String heraIntro   = "Umugani wose ni";
    public static final String[] GOOD   = {"Urabitoye ! 🎉","Uri intwari ! 💪",
        "Uraciye ubwenge pe ! 🧠✨","Amashi menshi ! 👏"};
    public static final String streakMsg = "Amashi menshi cane 👏👏👏";
    public static final String impa     = "Impa 😉";
    public static final String concedeMsg = "Ntudebukirwe ! 💪";
    public static final String jThink   = "Iyumvire inyishu, uhitemwo 🤔";
    public static final String jScoreLab= "Ivyo wari uzi";
    public static final String endTitle = "Urukino ruraheze !";
    public static final String perfTop  = "Turagukeje cane. Uri muri bake bashoboye kuronka amanota nk'aya ! Amashi menshi 🎉🔥👏";
    public static final String perfMid  = "Turagukeje. Ariko ubandanye wiga ibisokozo, hanyuma ubitore vyose. 👍📚✨";
    public static final String perfLow  = "Wagerageje. Ariko subira kwiga hahaha! 😄📖💪";
    public static final String replay   = "Subira ugerageze !";
    public static final String share    = "Sangiza abandi";
    public static final String home     = "Subira ku ntango";
    // ... replicate ENTIRE T object: Contribution (cTitle..cNote, cEmpty),
    //     History (hTitle,hSub,hTotal,hGames,hBest,hEmpty,hReset,hAsk,hDone),
    //     modes (nSokwe,nHera,nTuja,nPlayed), About (aTitle,aTag,aL1,aV1,aL2,aV2),
    //     level-up (lvlCheer,lvlQ,lvlYes,lvlNo), foot (fTag,fCredit),
    //     misc (hist,about,cCopied). Names in nSokwe.. include the "…" ellipses.
    public static final String[] NOMBRES = {
        "Rimwe","Kabiri","Gatatu","Kane","Gatanu","Gatandatu","Indwi","Umunani","Icenda","Cumi",
        "Cumi na rimwe","Cumi na kabiri","Cumi na gatatu","Cumi na kane","Cumi na gatanu",
        "Cumi na gatandatu","Cumi n'indwi","Cumi n'umunani","Cumi n'icenda","Mirongo ibiri"};
    static String motNombre(int n) { // 1-based; fallback to digits beyond 20
        return (n >= 1 && n <= NOMBRES.length) ? NOMBRES[n-1] : String.valueOf(n);
    }
}
```
Rule: **no English strings in layouts or code.** Everything displayable funnels through `KirundiUi` (single source mirrored from the JS `T`).

---

## 3. Architecture & tech stack (following existing app conventions)

- **Language:** Java.
- **UI:** XML layouts + `ViewBinding` (already used across `Rinjora*Activity`).
- **Screens:** `Activity` per top-level game screen, consistent with the existing `rinjora/` package (`RinjoraHomeActivity`, `RinjoraPlayActivity`, `RinjoraHistoryActivity`, etc.). Where multiple modes share a screen, re-use one Activity with a `mode` extra (proto modes are `sokwe|hera|tuja`).
- **Networking:** `RinjoraApi` (Retrofit interface, `RinjoraApiClient` singleton, `AuthInterceptor`, `Retrofit` callbacks). Gson `DTO`s under `network/dto/`. New round DTOs added here.
- **Repositories:** `data/` package, callback-style (`onSuccess / onAuthError / onError`), matching `RinjoraRiddleRepository` / `RinjoraJokeRepository`. The round flow does **not** persist to ObjectBox (server-owned).
- **Auth guard:** `AuthTokenStore.hasValidToken()` gate at the top of every `Rinjora*Activity` (existing pattern) → `RinjoraAuthActivity` on miss/401.
- **DI:** manual (repositories are constructed per-Activity) — no new framework.
- **Async:** Retrofit callbacks on main thread; disable double-taps while in-flight (existing `inFlight` pattern in `RinjoraJokeRoundActivity`).

**Package map additions**
```
org.kazinduzi.rinjora/
  util/KirundiUi.java            // ALL Kirundi strings + NOMBRES + motNombre
  view/ConfettiView.java         // emoji confetti honoring reduced-motion
  network/RinjoraApi.java        // + games round endpoints
  network/dto/  RoundDto, RoundItemDto, RoundAnswerDto,
                RoundCompleteDto, RoundHistoryDto, RoundHistoryRowDto,
                ContributionRequestDto
  data/RinjoraRoundRepository.java   // start/current/answer/skip/complete
  data/RinjoraHistoryRepository.java // getHistory / resetHistory
  data/RinjoraContributionRepository.java
  rinjora/RinjoraHomeActivity.java   // rebuilt as 3-card menu (uses Shell tab hosts for mode launchers)
  rinjora/RinjoraQuizActivity.java   // Sokwe + Heraheza (round-of-10)
  rinjora/RinjoraJokeActivity.java   // Tujajure round (round-of-10) — replaces standalone JokeRound loop
  rinjora/RinjoraEndActivity.java    // End: score/round, perf, Replay/Share/Home
  rinjora/RinjoraHistoryActivity.java// rebuilt to exact proto History
  rinjora/RinjoraContributeActivity.java // aligned form
  rinjora/LevelUpDialog.java          // modal (score>=8 && has next tier)
```

---

## 4. Network contract (endpoints consumed — matches backend plan §8)

DTO fields must match `backend-implementation-plan.md` exactly. Flat answer responses (not the `ApiEnvelope`) mirror the existing `AnswerController` convention.

```
POST /api/auth/register {name,email,password}        -> {success,data:{token,user}}
POST /api/auth/login     {email,password}            -> {success,data:{token,user}}
POST /api/auth/logout    (Bearer)
GET  /api/me             -> name, points, streak, ...

POST /api/games/{mode}/rounds            {level?}  -> {success,data:{round,item}}
GET  /api/games/{mode}/rounds            (recent rounds, for resume)
GET  /api/games/{mode}/rounds/{round}              -> {success,data:{round,item?}}  (resume/back)
POST /api/games/{mode}/rounds/{round}/items/{position}/answer  {answer|option} -> flat
POST /api/games/{mode}/rounds/{round}/items/{position}/skip    {}
POST /api/games/{mode}/rounds/{round}/complete        -> {success,data:{round,performance}}
GET  /api/games/history        -> {success,data:{total,games,best,rows:[{mode,games,points}]}}
DELETE /api/games/history      -> {success,data:{...}}
POST /api/contributions        {type,body,answer?,who?} -> {success,data:{status:'pending'}}
```

**DTO shapes**
- `RoundDto { id, mode, level, item_count, index, score, best_streak, current_streak, completed, level_available, next_level, has_more_levels }`
- `RoundItemDto { type('riddle'|'proverb'|'joke'), id, position, question|setup, category{...}, difficulty, options?[String] }` — parse both `question` (riddle/proverb) and `setup` (joke); options only for tuja.
- `RoundAnswerDto` (flat) `{ correct, conceded, answer?, message, round:{score,item_count,current_streak,best_streak,index,completed,level_available,next_level}, new_achievements:[] }`
- `RoundCompleteDto` `{ round:{...}, performance:'top'|'mid'|'low' }`
- History/Contribution DTOs per the contract above.

**Answers/punchlines are NEVER exposed by the round-start endpoint** — only after solve or concede. The client keeps that contract and never shows an answer early.

---

## 5. Screens & flows (one-to-one with `rinjora.html`)

### 5.1 Home — `RinjoraHomeActivity` (rebuild)
- `imigongo` top band; hero; brand **Rinjora**; slogan `T.slogan`; welcome `T.welcome`.
- **3 mode cards** (icon + `h3` + subtitle + `n` shortcut):
  - **Sokwe… Niruze !** / *Ibisokozo* → `RinjoraQuizActivity(mode=sokwe, level=1)`
  - **Heraheza** / *Imyibutsa — Heraheza/Tangura* → `RinjoraQuizActivity(mode=hera, level=1)`
  - **Tujajure !** / *Utujajuro — tube turatwenga* → `RinjoraJokeActivity(level=1)`
- Footer nav (`T.hist` 📊 Amateka yawe / `T.contrib` ✍️ Intererano yawe hano / `T.about` ℹ️ Ivyerekeye Rinjora) + `imigongo` bottom band.
- The legacy summary buttons that don't map to the prototype are moved behind a secondary/overflow entry (do not delete: Daily, Leaderboard, Favorites, Achievements, Duels remain reachable).

### 5.2 Quiz (Sokwe + Heraheza) — `RinjoraQuizActivity`
1. `POST /api/games/{mode}/rounds` with optional `level`. On success render the first item.
2. Topbar: custom `ProgressBar` (width = `index/item_count`), ⭐ score pill, 🔥 streak pill (**GONE when streak==0** — proto `pill.hidden`). Eyebrow: mode label (`T.labSokwe`/`T.labHera`), count `T.motNombre(index+1) + " / " + item_count`, level badge `T.level + level`.
3. Puzzle card (`question` for riddle, `question` for proverb — both parsed into the same text view). Free-text `EditText` placeholder `T.phSokwe`/`T.phHera` wrapped in the shared `AnswerView`.
4. **Check** (`T.check "Raba ko wabitoye."`) → `POST .../items/{pos}/answer {answer}`:
   - `correct => true`: confetti; feedback ok card with random `T.GOOD` + streak flair (`T.streakMsg` when streak≥2); reveal intro `T.answerIntro : allAns` (for sokwe) — `T.heraIntro : q.replace(/…$/,"") + firstAns` (for hera); enable **Bandanya**.
   - `correct => false, conceded => false`: feedback no card `T.impa` "Impa 😉", shake the input, keep position, user retypes (attempts++ server-side; position unchanged).
   - `conceded => true` (from `POST answer` or the dedicated skip): feedback no card `T.concedeMsg`, reveal answer, `current_streak=0`, enable **Bandanya**.
5. **Ndaguhaye !** (`T.give`) and **Rengana** (`T.skip`) → concede the current item (skip treated as concede per backend plan §4.5). On any form of "ndaguhaye" typed into the input, send it as an answer (proto `estNdaguhaye`).
6. **‹ Subira inyuma** (`T.back`, shown only when `index>0`) → `GET .../rounds/{round}` / back to `index-1`: if that item was already answered, render its solved state with input disabled and feedback shown.
7. **Bandanya** (`T.next`) → advance to next position; on the last item, call `POST .../complete`.
8. **Complete flow:** get `{round, performance}`. If `score ≥ 8` AND `level_available` → show `LevelUpDialog`; **Yes** (`T.lvlYes "Ego 💪"`) → new round at `next_level`; **No** (`T.lvlNo "Oya"`) → `RinjoraEndActivity`. Else → `RinjoraEndActivity` directly.
9. **Subira ku ntango** (`T.quit`) → confirm, discard round, Home. Double-taps disabled while in-flight.

### 5.3 Level-up dialog — `LevelUpDialog`
Mascot, `T.lvlCheer "Uriko uratsinda neza! 🔥"`, `T.lvlQ "Ushaka gutera intambwe igoye kurusha?"`, Yes/No. Yes restarts quiz at `next_level`; No shows the end screen with the pending score.

### 5.4 Tujajure — `RinjoraJokeActivity` (rebuild of `RinjoraJokeRoundActivity`)
1. `POST /api/games/tuja/rounds` → item has `setup` + exactly 4 `options` (shuffled server-side; **never re-shuffled client-side**).
2. Eyebrow labelled **Tujajure !** (red) + count. Setup card. Think prompt `T.jThink` "Iyumvire inyishu, uhitemwo 🤔".
3. 4 option buttons. Tap → `POST .../items/{pos}/answer {option}`:
   - Correct → highlight chosen green, confetti, feedback ok.
   - Wrong → highlight chosen red + reveal correct green (from returned `answer`/punchline), disable all, feedback `T.concedeMsg` (proto treats wrong as the "no" level).
4. **Bandanya** → next joke; last → complete → end flow (level-up applies to tuja too where backend reports `level_available`).
5. **Quit** → Home.

### 5.5 End — `RinjoraEndActivity`
- 🎊, `T.endTitle "Urukino ruraheze !"`, big `score / round` (`T.scoreLab` for quiz, `T.jScoreLab` "Ivyo wari uzi" for tuja).
- performance message from `T.perfTop`/`perfMid`/`perfLow` keyed on backend `performance` (top≥8, mid≥5, low<5).
- **Replay** (`T.replay "Subira ugerageze !"`): same mode & level, new round.
- **Share** (`T.share "Sangiza abandi"`): `"Rinjora — " + T.slogan + " — " + score + " / " + round + " ⭐"` via system share sheet (proto `partager`).
- **Home** (`T.home "Subira ku ntango"`). Confetti when score≥5.

### 5.6 History — `RinjoraHistoryActivity` (rebuild)
`GET /api/games/history` → proto History screen:
- header `T.hTitle "Amateka yawe"` + `T.hSub`.
- `bigstat` total (`T.hTotal "AMANOTA YOSE"`), duo (#games `T.hGames`, #best `T.hBest`).
- 3 per-mode rows `T.nSokwe/nHera/nTuja` + icon, `X T.nPlayed "incuro"`, `⭐ points`.
- Empty state `T.hEmpty`. **Reset** (`T.hReset "Futa amateka yose 🗑️"`) → confirm dialog `T.hAsk` → `DELETE /api/games/history` → toast `T.hDone` → refresh. **Quit** → Home.

### 5.7 About — static
`T.aTitle "Ivyerekeye Rinjora"`, gradient card, logo, `T.aTag`, credits (`T.aL1/aV1`, `T.aL2/aV2`), quit → Home.

### 5.8 Contribution — `RinjoraContributeActivity` (align)
Spinner type (`Igisokozo 🧠` / `Umwibutsa 🌾` / `Akajajuro 😂` / `Iyindi ngingo 💡`), body `T.cL2`, answer `T.cL3`, name `T.cL4` (si ngombwa), note `T.cNote`.
- **Send** (`T.cSend "Rungika 📤"`): validate non-empty body else toast `T.cEmpty`; `POST /api/contributions {type, body, answer?, who?}` → success toast + clear.
- **Copy** (`T.cCopy "Kopora 📋"`): compose exactly the proto `texteContrib()` — `"RINJORA — <type>\n\n<body>"` + optional `"\n\nInyishu: <ans>"` + `"\n\nUwabitanze: <who>"` → clipboard → toast `T.cCopied`.

---

## 6. Error handling, auth, security

- **Strictly online** for the game screens: network failure → friendly toast + retry on the failing screen. No local puzzle cache, no offline solving for rounds.
- **401** → `AuthTokenStore` cleared → `RinjoraAuthActivity` with clear-task flags (existing pattern).
- **Auth guard:** every `Rinjora*` game screen checks `AuthTokenStore.hasValidToken()` in `onCreate` before doing anything.
- **Cleartext:** `network_security_config` permits `10.0.2.2`/`192.168.x.x` only in **debug**; release HTTPS only.
- **Rate limit / double-tap:** `throttle:30,1` server-side; client disables Check/Option buttons while in-flight (existing `inFlight` flag).

---

## 7. Animations & polish (port from CSS)

- **fadeUp**: `ObjectAnimator` translateY + alpha for screen transitions (CSS `fadeUp`).
- **pop**: scale 0.94→1 for feedback cards (CSS `pop`).
- **shake**: translateX ±7dp ×2 on wrong answer (CSS `shake`).
- **confetti**: `ConfettiView` spawning ~14 emoji (`🎉⭐🔥💫🟢🔴🟡🐇`) with vertical fall + rotate via `ValueAnimator` (CSS `confetti`/`fall`). **Skip when reduced-motion** (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` / `ViewConfiguration`).
- **flick/pulse/sway/bob** on hero/mascot only if an animated `VectorDrawable`/Lottie is used (cosmetic; optional).
- MVP: implement confetti on **correct answer**, **level-up**, **end**; pop on feedback; shake on wrong; fadeUp on transitions.

---

## 8. Testing plan (Android, JVM + instrumented)

- **Unit (JVM):** `KirundiUiTest` (NOMBRES bounds + `motNombre` fallback), `RoundDto`/Gson parsing from backend JSON fixtures (representative strings as resources; assert `question` vs `setup` and `options` parsing), score/performance classification.
- **Integration (JVM):** `RinjoraRoundRepositoryTest` with OkHttp `MockWebServer` asserting correct URL/payload + DTO mapping for start/answer/skip/complete; `RinjoraHistoryRepositoryTest` for aggregation + reset; contribution copy-format test (`texteContrib` parity).
- **UI:** `RinjoraQuizActivity` (correct→ok feedback + reveal, wrong→shake + not moving on, concede→`T.concedeMsg` + reveal, back renders solved state, level-up dialog on score≥8), `RinjoraJokeActivity` (4 options, correct green / wrong red + correct green, options never reordered), `RinjoraHistoryActivity` (empty state, 3 rows, reset confirm). Network faked via MockWebServer.
- **Manual QA** on emulator + device against backend staging.

---

## 9. Delivery order (incremental, each milestone shippable; commit per logical unit)

1. **Strings + theme + Home**: `KirundiUi` (full `T` + `NOMBRES`), brand theme (fonts/colors from palette, `brand_*`/`text_*` existing), rebuild `RinjoraHomeActivity` as the 3-card menu + footer nav; keep legacy feature entries reachable.
2. **Quiz skeleton (Sokwe/Heraheza)**: `RinjoraApi` round endpoints + DTOs; `RinjoraRoundRepository`; `RinjoraQuizActivity` start round, render item, Check → correct/wrong feedback via `AnswerView`, reveal, Next, Back, Skip/Give-up, Quit.
3. **End + level-up**: `RinjoraEndActivity` (score/round, perf message, Replay/Share/Home); `LevelUpDialog` wired to `complete`'s `level_available`/`next_level`.
4. **Tujajure**: rebuild `RinjoraJokeActivity` to the proto `s-joke` layout within a round; options + highlight; Bandanya advance; complete → end.
5. **History + About + Contribution**: rebuild `RinjoraHistoryActivity` (total/duo/3 rows/reset), About (static), align `RinjoraContributeActivity` (send/copy + `POST /api/contributions`).
6. **Polish + hardening**: `ConfettiView` + pop/shake/fadeUp + reduced-motion, error/retry handling, double-tap guards, release keystore + HTTPS, final parity pass against `docs/rinjora.html`.

---

## 10. Parity checklist (final acceptance vs `docs/rinjora.html`)

- [ ] Home: 3 mode cards with exact Kirundi titles/subtitles + footer History/Contribute/About.
- [ ] Quiz: progress bar, ⭐ score pill, 🔥 streak pill only when streak>0, level badge, count in Kirundi ordinals.
- [ ] Check / Give-up / Skip / Back / Next / Quit match prototype behaviour incl. feedback card states and emoji messages.
- [ ] Answer revealed only after solve or concede — never before.
- [ ] Tujajure: 4 server-shuffled options, correct=green, wrong=red + correct highlighted, think prompt, Bandanya.
- [ ] End: `score / round`, correct performance message per band, Replay/Share/Home.
- [ ] Level-up dialog appears only on `score ≥ 8` + harder tier exists; Yes continues, No ends.
- [ ] History totals/games/best + 3 per-mode rows + reset confirmation + `T.hDone` toast.
- [ ] Contribution form: type dropdown + send/copy + note; copy format matches `texteContrib()`.
- [ ] All strings 100% Kirundi (mirror `T`/`NOMBRES`); no English leaks.
- [ ] Strictly-online (401 → login, offline → retry), no puzzle caching.
- [ ] Reduced-motion honoured (no confetti/animations).

---

## 11. Non-goals (now; "other improvements later")

- Duels, favorites, daily-riddle gameplay parity, leaderboard — keep as-is (existing screens remain reachable from Home).
- Offline-first solving for the round screens (server-owned by design).
- Server-side localization (strings stay 100% client-side in `KirundiUi`).
- Any change to the existing single-item riddle/proverb/joke endpoints the app already uses.
