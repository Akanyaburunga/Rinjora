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
- **Phase 1 — Identity & scaffolding (committed)**: renamed app to Rinjora, set `applicationId` to
  `org.kazinduzi.rinjora`, renamed theme to `Theme.Rinjora`, applied the deep green + golden amber
  palette. Shell (MainActivity, bottom nav, nav graph) unchanged. Build green. Java package untouched.

---

*Migration doc — companion to `android-app-implementation-plan.md` and the pending open decisions in §7.*
