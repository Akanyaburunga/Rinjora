# Offline-First Architecture — Android Client Guide

The app is **offline-first**: every screen renders from a local ObjectBox cache *instantly*, then
refreshes in the background. Screens never depend on the network to show content.

The driver home screen (`HomeFragment` + `DriverHomeRepository` + `HomeSnapshot` + `HomeSyncWorker`)
is the reference implementation. The driver profile screen (`ProfileFragment` +
`DriverProfileRepository` + `DriverProfile`/`DriverTrip` + `ProfileSyncWorker`) follows the same
pattern with a small twist: its `latest_trips` list is persisted as a **second entity**
(`DriverTrip`, keyed by `driverId`, replaced wholesale each sync) rather than a JSON blob. Follow
both for all future screens.

## Principles

1. **Cache first.** Store the latest server response for a screen as an ObjectBox entity. Render
   from that entity synchronously on screen open — no network required, no blank state.
2. **Sync in the background.** Keep the cache fresh with:
   - a **WorkManager periodic worker** for data that should stay fresh app-wide, and
   - a **foreground poll** (e.g. every 60s) for time-critical data while its screen is visible.
3. **Never block the UI on the network.** Volley requests run async; loading/error states only
   appear when there is *no* cache to show.
4. **Be honest about staleness.** Show a sync status line ("Updated 5m ago", "Offline · last
   update 2h ago") instead of silently showing old data.
5. **Treat 401/403 as auth failure.** Deactivate the stored user and send the user to
   `AuthActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.

## How the home screen does it

### 1. Entity — `entities/HomeSnapshot.java`
- One row per driver (indexed by `driverId`), overwritten on every sync.
- Fields are flat copies of the API payload (`driver`, `vehicle`, `position`, `nearest_place`,
  `assigned_staff`, `trip`) plus:
  - `fetchedAt` (epoch ms of the last successful sync) — used for staleness,
  - `hasActiveTrip` (false when the server answered 404),
  - `rawJson` — the full payload kept for future screens/debugging.
- Add new entities to the same package; ObjectBox generates `MyObjectBox` automatically on build.

### 2. Repository — `data/DriverHomeRepository.java`
- `fetch(context, callback)` — GET with the bearer token (from the active `User`), parses,
  saves to ObjectBox, then calls `onSuccess(snapshot, idle)`, `onAuthError()` (401/403), or
  `onError(message)` (network / parse failure).
- `getCached(context)` — latest snapshot by `fetchedAt`, or null when never synced.
- **404 (idle)**: keeps the last-known identity fields and only flips `hasActiveTrip` to false,
  so the idle screen can still show driver/vehicle info. Never clears the cache on a 404.
- All `opt*` reads, so missing/`null` sections never crash the parser.

### 3. Worker — `data/HomeSyncWorker.java`
- A plain `Worker` (Volley is async, so it blocks on a `CountDownLatch` with a 45s timeout).
- Returns `Result.success()` regardless of the fetch outcome — a failed sync keeps the cache and
  is simply retried on the next period.
- Scheduled once in `MyApp.onCreate()`:
  `enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, 15.minute, network-connected)`.

### 4. UI — `ui/home/HomeFragment.java`
- `onViewCreated`: render from cache (`renderCached()`), then `fetch(!hasCache)` — show the
  spinner only when there is nothing cached yet.
- `onResume` starts a 60s poll (`fetch(false)` + `postDelayed`); `onPause`/`onDestroyView` stops
  it. All callbacks check `binding != null`.
- A refresh button forces `fetch(true)`; error state + Retry button appear only when there is no cache.
- Rendering is fully defensive: sections (trip / position / fuel / dispatcher) hide when their
  data is absent.

## Conventions for new screens

| Concern | Rule |
|---------|------|
| Cache entity | One entity per screen/aggregate; `fetchedAt` on every entity; flat fields, `rawJson` optional |
| Network access | Only through a `data/*Repository` class; never call Volley from a Fragment/Activity |
| Auth | Repository reads the token via `User_` box query (`isActive == true`); 401/403 → `onAuthError()` |
| Loading | Show a spinner only when cache is empty; otherwise background-refresh silently |
| Staleness | Always surface a "last updated" / offline indicator |
| Polling | 60s poll only while the screen is visible; WorkManager for everything else |
| Callbacks | Guard with `if (binding == null) return;` — Volley delivers after view destruction |

## Troubleshooting

- **Worker never runs**: periodic WorkManager has a minimum 15-minute interval and OS-level
  throttling; debug with `WorkManager` logging or trigger a one-off `OneTimeWorkRequest` while developing.
- **ObjectBox entity not found**: `HomeSnapshot_` / `MyObjectBox` are generated at build time —
  run a build before compiling against them.
- **`is_stale` position**: show "Signal lost · last seen …" (see `DateUtils.relative`), don't
  pretend the marker is live.
