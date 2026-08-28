# Migration: Logistics Companion → Language / Culture Games App

This doc is the bridge between the **current** codebase (a driver/mechanic logistics companion,
namespace `rw.martinhardware.mymartin`, app name "MyMartin") and the **target** described in
[`android-app-implementation-plan.md`](android-app-implementation-plan.md) (the "Kazinduzi" Kinyarwanda
language/culture game — riddles, streaks, duels, badges, leaderboards, user submissions).

It answers: **what stays, what is rewritten, and in what order**, so a single developer can convert
this app without losing the reusable scaffolding. The plan doc remains the contract for the final
backend API; this doc is strictly about the Android-side transformation.

> Read this together with `ANDROID_OFFLINE_FIRST.md` (the offline/caching pattern is the same one we
> keep using) and the implementation plan's posed Phases A–L.

---

## 1. Mental model: reuse the shell, replace the features

The two apps share **almost no feature code**, but they share a **lot of scaffolding**. Think of the
migration as swapping the middle of the app out while keeping the edges.

| Layer | Today (logistics) | Reuse? | Target (language game) |
|-------|-------------------|--------|------------------------|
| App skeleton (`Application`, `MainActivity`, BottomNav, nav graph) | Binary | ✅ **Keep** | Same shell, new fragments |
| Build config (Gradle, Maven catalog, Firebase, ProGuard, min/target SDK) | Binary | ✅ **Keep** | Keep, update app name/id |
| Networking transport | Volley | ❌ **Rewrite** | Retrofit + OkHttp + AuthInterceptor + serialization |
| Persistence | ObjectBox (4 entities) | ✅ **Engine keep**, ❌ entities | Game entities, same engine |
| Auth | WhatsApp/Phone/Email `/mobile/auth/*`, token in ObjectBox | ❌ **Rewrite** | `/auth/*` Bearer, EncryptedSharedPreferences |
| Role model | Driver + mechanic (dual role tabs) | ❌ **Remove** | Single player (+ reputation-based curator tier) |
| Screens | Home(trips), Repairs, Workshop, Support, Notifications, Profile | ❌ **Rewrite** | Home, Play, Daily, Leaderboard, Duels, Profile, Achievements, Submissions |
| Background work | WorkManager sync workers | ✅ **Pattern keep** | Streak/daily reminder workers |
| `DateUtils`, `AnalyticsHelper` | Utility | ✅ **Keep** | Reuse as-is |
| Firebase | Analytics + Crashlytics | ✅ **Keep** | Keep (drop phone-auth only) |

---

## 2. What is **reusable** (keep as-is or with light edits)

1. **App shell** — `MainActivity`, `BaseActivity`, `activity_main.xml`, `bottom_nav_menu.xml`.
   Only the fragments referenced by `mobile_navigation.xml` change.
2. **ObjectBox** — keep the engine, the `io.objectbox` Gradle plugin, and the generated-`MyObjectBox`
   flow (`MyApp.getBoxStore()`). This is the offline/caching backbone the plan relies on (§11 K).
3. **The offline-first pattern** — `ANDROID_OFFLINE_FIRST.md` describes **entity + repository +
   sync-worker + defensive UI rendering**. This is the exact data pattern the game screens need
   (cache `/me/summary`, riddle lists, categories; store `hints_revealed` as resume state). Keep the
   pattern, drop the logistics-specific code.
4. **`DateUtils`** — its tolerant ISO-8601 parser is directly reusable for server-authoritative
   timestamps (streaks, daily, duels — plan §11).
5. **`AnalyticsHelper`** — keep for game-event logging.
6. **Maven/Gradle/Firebase/ProGuard/manifest permissions skeleton** — `INTERNET`,
   `ACCESS_NETWORK_STATE`, `CAMERA`, WorkManager, Firebase.

---

## 3. What is **removed** (logistics-only)

Delete or stop wiring these once their screens are replaced:

- `entities/HomSnapshot.java` (typo: `HomSnapshot`), `entities/DriverProfile.java`, `entities/DriverTrip.java`
- `data/DriverHomeRepository.java`, `data/DriverProfileRepository.java`, `data/HomeSyncWorker.java`, `data/ProfileSyncWorker.java`
- `ui/repairs/**` (driver repair requests + detail + create + adapter)
- `ui/workshop/**` (mechanic tasks + detail + adapter)
- `ui/support/**` (support tickets/chat) — *optional*: could be repurposed as "Help/Report" for game
  content, but plan §8 uses `/submissions/riddles` for player feedback instead
- `models/Ticket*`, `models/Repair*` (non-persisted POJOs)
- `AuthSelectionFragment`/`PhoneLoginFragment`/`WhatsAppLoginFragment` and the Firebase-phone / WhatsApp OTP auth paths
- Repair/location-related drawables/icons no longer referenced

---

## 4. What is **rewritten** (the real work)

### 4.1 Identity & branding
- `applicationId` → new (e.g. `rw.kazinduzi.app`), `namespace` → new package, `app_name` → "Kazinduzi".
- Update `themes.xml`, `colors.xml`, launcher icons to a language/culture theme.

### 4.2 Networking layer (plan Phase A — **do first, everything depends on it**)
- Replace Volley with **Retrofit + OkHttp + kotlinx-serialization**.
- Add an `AuthInterceptor` that attaches `Authorization: Bearer <token>`.
- Central `ApiClient` with debug vs release base URL; base URL now points at the **game backend**
  (`https://<domain>/api`), not `martin-logistics.nova.bi`.
- JSON envelope model `{ success, data }` — parse `success=false` as an error even on HTTP 200.
- **Secrets:** move the auth token from ObjectBox to `EncryptedSharedPreferences`.
- `ApiConfig.java` endpoint set → replace `/mobile/*` with `/auth/*`, `/riddles/*`, `/me/*`,
  `/leaderboard`, `/duels/*`, `/submissions/riddles`.

### 4.3 Auth (plan Phase B)
- Register → Login → (verify email in prod) → logout → change password.
- `device_name` = `"Android_" + installId` for stable one-token-per-device (§11).
- 401-interceptor → force re-login; keep an `isAuthenticated` gate in `MainActivity` but key it to
  the new token model (EncryptedSharedPreferences) instead of the ObjectBox `User.isTokenValid()`.

### 4.4 Entities (ObjectBox; engine unchanged)
Replace logistics entities with game models. Follow the offline-first layout from §2 item 3:

| New entity | Backs | Source endpoint |
|------------|-------|-----------------|
| `Player` (id, name, email, reputation, level, streak, freezes) | session + cached auth | `/auth/*`, `/me` |
| `SummarySnapshot` (Home payload + `fetchedAt` + `rawJson`) | Home screen cache | `/me/summary` |
| `Category` (id, name, slug, description, riddles_count) | category filters | `/riddles/categories` |
| `Riddle` (id, question, hints, category, difficulty, type, `hints_revealed`) | play + resume | `/riddles`, `/riddles/{id}`, `/riddles/{id}/hint` |
| `Attempt` (riddle, submitted_answer, is_correct, rewarded, attempted_at) | history | `/riddles/history` |
| `Duel` (opponent, riddle, wager, status, my_attempt, winner) | duels inbox/play | `/duels/*` |
| `Submission` (question, answer, difficulty, status, rejection_reason) | contribution list | `/submissions/riddles` |

> The plan (§2, §6.4) explicitly says `riddle.answer` is **confidential** — it is only in the
> payload after a correct solve or explicit `reveal`. Never write it into an ObjectBox entity from a
> list response, and never log it.

### 4.5 Screens / nav graph (plan Phases C–L)

New top-level nav destinations (5 tabs, matching the existing bottom-nav shape):

1. **Home** → `/me/summary` (avatar, name, level/progress bar, streak chip, badge grid, activity stats)
2. **Play** → riddle list (category/type/difficulty filters) → riddle screen
   (question, progressive hints, answer → `/riddles/{id}/answer`, reveal/learning mode)
3. **Daily** → `/riddles/daily` + streak + `/riddles/daily/status` bell + freeze spend + history archive
4. **Leaderboard** → `/leaderboard` with period tabs, pagination, highlighted "me" row
5. **Profile** → `/me`, `/me/levels`, `/me/achievements`, points ledger `/points`, logout

Detail screens (launched by Intent like the current detail Activities, or as nested nav destinations):
- **Duel screen** (`/duels/{id}`): single answer attempt, "waiting on opponent", winner banner, wager → §7
- **Submissions** ("Contribute a riddle" form → `POST /submissions/riddles`; my-submissions list) → §8

Remove/drop: **Repairs**, **Workshop**, **Support-ticket**. Repurpose **Notifications** into
streak-at-risk + pending-duel alerts (backed by `/riddles/daily/status.pending_challenges`).

---

## 5. Build order (dependency-ordered)

Follow the plan's combined build order; this is the same sequence against the rewritten features:

1. **Identity & scaffolding** — rename app, package, update Gradle/theme/launcher.
2. **Phase A — networking**: Retrofit + envelope + AuthInterceptor + EncryptedSharedPreferences;
   acceptance = debug screen pings `GET /riddles` (after login) and prints JSON.
3. **Phase B — auth**: register/login/verify/logout; session survives restart.
4. **Phase C — Home** from `/me/summary` + ObjectBox cache (offline-first pattern).
5. **Phase D — Play**: riddle list, hint, answer, reveal, history/stats, streak freeze.
6. **Phase E — Daily + streak**: daily screen, history archive, status bell, freeze flow.
7. **Phase F — Leaderboard**.
8. **Phase G — Favorites, share, deep links** (`/riddles/{id}/share`, `/riddles/share/{code}`).
9. **Phase H — Achievements polish**.
10. **Phase I — Duels** (inbox + create + live play).
11. **Phase J — Submissions** (contribute-a-riddle form + my-submissions).
12. **Phase K — Offline & polish**: cache summary/lists/categories, pull-to-refresh, empty/error/
    loading states, 429 backoff.

---

## 6. Gotchas / conventions carried over

- **`{ success, data }` envelope everywhere** — treat `success=false` as an error even on HTTP 200.
- **Never hardcode reputation/points/balances** — always trust the API (plan §9).
- **Daily riddle is deterministic per user/date** — never cache it across dates (plan §11).
- **One answer attempt per duel** — disable the button once sent; show "Waiting on opponent".
- **Server time is authoritative** — use server `created_at`/`solved_at`, not device clock, for
  streaks/duels/daily (plan §11).
- **Keep the offline-first discipline** from `ANDROID_OFFLINE_FIRST.md` for every new screen
  (cache-first render, background refresh, staleness line, 401→relogin).

---

## 7. Decisions (resolved as of Phase 1)

- **Branding**: app name is **Rinjora** (a subproject of the larger Kazinduzi ecosystem). `applicationId`
  is **`org.kazinduzi.rinjora`**. The Java package **stays** `rw.martinhardware.mymartin` for now —
  package migration was deferred until the feature rewrite (choice: keep package, only change `applicationId`).
- **Theme**: **deep green + golden amber** palette (see `values/colors.xml`). `Theme.MyMartin` →
  `Theme.Rinjora`.
- **Package/path migration**: deferred (see above).
- **Support chat**: still open — likely removed, replaced by `/submissions/riddles` for content feedback.
- **Notifications**: still open — proposed to become the streak/duel alert tab.
- **Stub vs full removal** of logistics `ui/` packages: remove as each replacement ships, to keep the build green.

### Phase log
- **Phase 9 — Badges & achievements library (committed)**: added the achievements/badges library
  screen from `GET /me/achievements` (plan Phase H, §4.4), no new ObjectBox entity.
  - `network/dto/` — `BadgeDto` (id/slug/name/description/category/icon/threshold/metric/earned/
    earned_at/progress/goal), `AchievementLibraryDto` (`earned_count`, `total`, `achievements[]`).
  - `network/RinjoraApi` — `GET /me/achievements` (generic `ApiEnvelope`).
  - `data/RinjoraAchievementsRepository` — fetch with 401 → auth error; error-message extraction.
    Offline caching of the library deferred to Phase K.
  - `rinjora/RinjoraAchievementsActivity` + layouts — the badge library: earned badges listed first
    and highlighted (green dot + "EARNED"), locked ones greyed, each with a per-badge horizontal
    progress bar and "progress / goal" text, grouped-ish by category label; a "earned_count of total"
    header. Re-renders on resume.
  - Unlock toasts surfaced at answer time from Phase 5 (`new_achievements[]`); this screen is the
    persistent library view.
  - Entry: Home now has an "Achievements" button; new activity registered in the manifest.
  - **Tests/Lint**: new `RinjoraAchievementsContractTest` (1 test) verifies §4.4 parsing — now
    **17/17** unit tests pass. New Phase 9 files: zero lint *errors* (only style warnings). The
    3 pre-existing lint errors remain unchanged (none in Rinjora code).
- **Phase 8 — Favorites, sharing & progress (committed)**: added save-to-favorites, a native share
  sheet, and re-used the persisted `hints_revealed` resume point (plan Phase G, §6.1–§6.4).
  - `network/dto/` — `ShareDto` (`share_url` + `code`) from `POST /riddles/{id}/share`.
  - `network/RinjoraApi` — added `GET /me/favorites`, `POST /me/favorites/{riddle}`, `DELETE
    /me/favorites/{riddle}` (§6.1), and `POST /riddles/{id}/share` (§6.2, optional `recipient_email` body).
  - `data/RinjoraRiddleRepository` — added `setFavorite(riddleId, boolean, cb)` (add via idempotent
    POST, remove via DELETE) and `shareRiddle(riddleId, cb)`; both handle 401 → auth error.
  - `data/RinjoraFavoritesRepository` — `GET /me/favorites` (solved-marked riddle payloads). Offline
    caching of the favorites list deferred to Phase K.
  - `rinjora/RinjoraPlayRiddleActivity` + layout — added a ♥ favorite toggle and a "Share" button
    (fires an Android `ACTION_SEND` share sheet with the short link). §6.4 resume already handled by
    re-applying `hints_revealed` on load (from Phase 5).
  - `rinjora/RinjoraFavoritesActivity` + layouts — lists favorites, opens a saved riddle for play,
    and lets the user remove it (heart button → DELETE). Re-renders on resume.
  - Entry: Home now has a "♥ Favorites" button; new activity registered in the manifest.
  - **Tests/Lint**: new `RinjoraShareContractTest` (2 tests) verifies §6 ShareDto + favorites-list
    parsing — now **16/16** unit tests pass. New Phase 8 files: zero lint *errors* (only style
    warnings). The 3 pre-existing lint errors remain unchanged (none in Rinjora code).
- **Phase 7 — Leaderboard (committed)**: added the ranked board with period filters, a highlighted
  "me" row, and pagination (plan Phase F, §5.1).
  - `network/dto/` — `LeaderboardEnvelope` (custom top-level shape with `filter`/`data`/`me`/`meta`
    alongside `success`), `LeaderboardEntryDto`, `LeaderboardMeDto`, `LeaderboardMetaDto`.
  - `network/RinjoraApi` — `GET /leaderboard?filter=&page=&per_page=` returning the custom envelope
    (it does not fit the generic `ApiEnvelope`), not the wrapped form.
  - `entities/RinjoraLeaderboardSnapshot` (ObjectBox) — caches the latest envelope per period filter
    (metadata + raw JSON) for offline render.
  - `data/RinjoraLeaderboardRepository` — offline-first fetch + cache per filter, re-parses cached JSON,
    401 → auth error. (String property queried in-memory since ObjectBox has no string `equal`.)
  - `rinjora/RinjoraLeaderboardActivity` + layouts — period filter chips (today/this_week/this_month/
    this_year/all_time, default all_time), a highlighted "me" card (rank, percentile, total players),
    a ranked `RecyclerView` with per-row words/meanings contributions and a highlight on the current
    user's row, a "Load more" button gated by `meta.last_page`, and offline render from cache.
  - Entry: Home now has a "Leaderboard" button; activity registered in the manifest.
  - Pull-to-refresh deferred to Phase K (swiperefreshlayout not currently a dependency); Refresh button provided.
  - **Tests/Lint**: new `RinjoraLeaderboardContractTest` (1 test) verifies §5.1 parsing — now **14/14**
    unit tests pass. New Phase 7 files: zero lint *errors* (only style warnings). The 3 pre-existing
    lint errors remain unchanged (none in Rinjora code).
- **Phase 6 — Daily riddle & streak experience (committed)**: added the daily one-riddle-per-day
  loop with streak-at-risk warnings and streak-freeze spend (plan Phase E §2.4–§2.6, §2.12).
  - `network/dto/` — `DailyRiddleDto` (reuses `StreakDto` + `RiddleDto`), `DailyStatusDto`,
    `FreezeResponseDto`.
  - `network/RinjoraApi` — added `GET /riddles/daily`, `GET /riddles/daily/history`, 
    `GET /riddles/daily/status`, `POST /riddles/streak/freeze`.
  - `entities/RinjoraDailySnapshot` (ObjectBox) — caches the latest daily status + a lightweight
    copy of today's riddle; daily is deliberately <b>not</b> cached as a single "today" — the server
    stays authoritative for date-driven determinism and clocks.
  - `data/RinjoraDailyRepository` — offline-first: merges daily + status into one snapshot (cache-first,
    background refresh, 401 → auth error, 60s re-poll); `freeze()` spends a freeze (422 surfaced as error).
  - `rinjora/RinjoraDailyActivity` + `res/layout/activity_rinjora_daily.xml` — hub showing current/longest
    streak + solved-by count, a streak-at-risk warning card with a "Spend a streak freeze" action,
    a pending-challenges badge, and today's daily riddle. "Solve today" (gated by `daily_available`)
    opens the Phase 5 play screen to answer; returning refreshes state.
  - Entry points: Home now has a "Daily riddle" button (plus the existing "Play riddles"); new activity
    registered in the manifest.
  - **Tests/Lint**: new `RinjoraDailyContractTest` (3 tests) verifies §2.4–§2.6 + §2.12 payload parsing —
    now **13/13** unit tests pass. New Phase 6 files: zero lint *errors* (only style warnings). The
    3 pre-existing lint errors remain unchanged (none in Rinjora code).
- **Phase 5 — Core game loop: play riddles (committed)**: replaced the Phase 3 `/riddles` ping
  harness (which only ever poked the API) with the real play screens (plan Phase D):
  - `network/dto/` — `AnswerResponseDto` (+ nested `AchievementDto`), `RevealDto`, `HintDto`,
    `HistoryEntryDto`, `HistoryStatsDto` (+ `CategoryStatDto`) for §2.8–§2.11.
  - `network/RinjoraApi` — added `GET /riddles/{id}/hint`, `POST /riddles/{id}/answer`,
    `POST /riddles/{id}/reveal`, `GET /riddles/history`, `GET /riddles/history/stats`.
  - `entities/RinjoraRiddleSnapshot` (ObjectBox) — offline cache of the riddle being played;
    the confidential `answer` is **never** persisted (only shown after a correct solve or reveal).
  - `data/RinjoraRiddleRepository` — offline-first: fetch/cache a riddle, progressive hints,
    answer submission, reveal; 401 → auth error. API-23-compatible (no `java.time`).
  - `rinjora/RinjoraPlayActivity` — riddle **list** (`GET /riddles`) with difficulty/type filter
    chips, `solved` markers, opens a riddle.
  - `rinjora/RinjoraPlayRiddleActivity` — single-riddle **play**: progressive hint reveal
    (`/hint`), answer input → `/answer` (correct/points/capped/new-achievements banner, input
    locked once solved), and a no-reward learn/reveal mode (`/reveal`).
  - `rinjora/RinjoraHistoryActivity` — attempt **history** list (`/riddles/history`) + **stats**
    header (`/riddles/history/stats`) with per-row correct/incorrect markers.
  - Home's "Play riddles" button now opens the list; the old `RinjoraRiddlesActivity` debug
    harness and its layout were **removed** (manifest entry cleaned up).
  - **Tests/Lint**: new `RinjoraPlayContractTest` (6 tests) verifies §2.8–§2.11 payload parsing —
    now 10/10 unit tests pass. New Phase 5 files: zero lint *errors* (only style warnings,
    consistent with the app). The 3 pre-existing lint errors remain unchanged (none in Rinjora code).
- **Phase 1 — Identity & scaffolding (committed)**: renamed app to Rinjora, set `applicationId` to
  `org.kazinduzi.rinjora`, renamed theme to `Theme.Rinjora`, applied the deep green + golden amber
  palette. Shell (MainActivity, bottom nav, nav graph) unchanged. Build green. Java package untouched.
- **Phase 2 — Networking layer (committed)**: added a new Retrofit + OkHttp + Gson layer for the
  Rinjora game API, alongside the legacy Volley layer (logistics features still intact).
  - New deps in the version catalog: Retrofit 2.9.0, OkHttp 4.12.0, Gson 2.10.1,
    androidx.security:security-crypto for EncryptedSharedPreferences.
  - `network/ApiConfig` — added `KAZINDUZI_BASE_URL` (build-type aware, ends in `/`),
    pointing at `api.kazinduzi.bi` (prod) / `10.0.2.2:8000` (dev emulator) — TODO provision real host.
  - `network/AuthTokenStore` — EncryptedSharedPreferences (Bearer token, expiry, stable `device_name`).
  - `network/AuthInterceptor` — attaches `Authorization: Bearer <token>`.
  - `network/RinjoraApiClient` — central Retrofit singleton (debug-only logging, 30s timeouts).
  - `network/ApiEnvelope<T>` — `{ success, data, message }` envelope; **RiddleDto deliberately has no
    `answer` field** (secret never bound from list payloads).
  - `network/RinjoraApi` — Retrofit interface: `/auth/{register,login,logout,user}`, `/riddles`,
    `/riddles/categories`, `/riddles/{id}`, `/riddles/next`.
  - `network/dto/` — RiddleDto, CategoryDto, UserDto, LoginResponseDto.
  - Unit tests (`app/src/test/.../RinjoraApiContractTest`) verify envelope + DTO parsing against the
    plan's example payloads and that the confidential `answer` is never exposed. All pass; build green.
  - **Deferred to Phase B**: the "debug screen pings `GET /riddles` after login" acceptance, since it
    needs the auth flow that Phase B provides. Interceptor/envelope/client are ready for it.
- **Phase 4 — Home screen (`/me/summary`, offline-first) (committed)**: the authenticated landing
  screen is now a Rinjora Home that renders the player summary `GET /me/summary` (§4.1) from an
  ObjectBox snapshot (mirroring the logistics `DriverHomeRepository`/`HomeSnapshot` offline-first
  pattern: cache-first, background refresh, staleness line).
  - `network/dto/SummaryDto` + nested `SummaryUserDto`, `PointsDto`, `StreakDto`, `BadgesDto`,
    `ActivityDto` — parse the §4.1 example payload (`RinjoraSummaryContractTest`).
  - `network/RinjoraApi` — added `@GET("me/summary")`.
  - `entities/RinjoraSummarySnapshot` (ObjectBox) — flattened summary fields + `rawJson` + `fetchedAt`,
    one row per `userId`. This adds a new entity to the ObjectBox model.
  - `data/RinjoraSummaryRepository` — offline-first: cache-first, fetch → save to ObjectBox, 401 →
    auth error; API-23-compatible (no `java.time`).
  - `rinjora/RinjoraHomeActivity` + `res/layout/activity_rinjora_home.xml` — renders from cache with a
    60s background poll and staleness line; logout; a "Riddles API debug" button still reaches the
    Phase 3 `/riddles` harness until later phases replace it.
  - `RinjoraAuthActivity` now lands on `RinjoraHomeActivity` when authenticated (was `RinjoraRiddlesActivity`).
  - **ObjectBox model reconciliation**: this commit commits the full regenerated `default.json`, which
    includes the new `RinjoraSummarySnapshot` entity **and** reconciles a pre-existing drift where
    `HomeSnapshot.java` had an uncommitted `hasPosition` field (it was in the committed source but never
    in the committed model, so the model was dirty after every build). This finally syncs the model to
    the committed source and stops the perpetual dirty-model churn.
  - **Tests/Lint**: new `RinjoraSummaryContractTest` (1 test) passes alongside the 3 existing
    `RinjoraApiContractTest` tests (4/4). New Phase 4 files: zero lint *errors* (only style warnings,
    consistent with the rest of the app). The 3 pre-existing lint errors remain unchanged.
- **Phase 3 — Auth (committed)**: added a self-contained Rinjora (Kazinduzi) register/login/logout
  flow wired to `/auth/*`, stored in `AuthTokenStore`, plus the deferred Phase A "ping `/riddles`" screen.
  - `data/RinjoraAuthRepository` — wraps register/login/logout/currentUser; persists the Bearer token
    + stable `device_name`; API-23-compatible expiry parsing (`SimpleDateFormat`, not `java.time`).
  - `viewmodel/RinjoraAuthViewModel` — LiveData state/loading/error mirroring the legacy auth VM.
  - `rinjora/RinjoraAuthActivity` + `RinjoraLoginFragment` + `RinjoraRegisterFragment` (Material 3
    TextInput forms, validation, client-side login/register).
  - `rinjora/RinjoraRiddlesActivity` — **Phase A acceptance**: after login, pings `GET /riddles`
    with the Bearer token and prints the JSON; refresh + logout. Acts as a temporary harness.
  - Entry point: a "Rinjora (Kazinduzi)" card on the existing auth-selection screen launches
    `RinjoraAuthActivity`. The logistics login flow is untouched and still drives `MainActivity`.
  - New activities added to the manifest (`Theme.Rinjora`). New `ic_riddle` icon.
  - Register returns no token (plan §1.1) so it returns to login; the user logs in to get a session.
  - **Lint**: my Phase 3 files are clean. The 3 remaining repo lint errors are pre-existing
    (`AuthActivity.onBackPressed`, `WorkshopTasksFragment.List#sort`) + the local `local.properties`
    file, none introduced here.

---

*Migration doc — companion to `android-app-implementation-plan.md` and the pending open decisions in §7.*
