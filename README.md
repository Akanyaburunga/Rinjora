# Rinjora

Android companion app for **Kazinduzi** — Kirundi word games: riddles (*ibisokozo*),
proverbs (*immyibutsa*) and jokes (*tujajure*). Built in **Java + XML Views** with a
strictly-online, backend-owned game model.

## Games

- **Sokwe… Niruze !** — riddle quiz rounds.
- **Heraheza** — proverb quiz rounds.
- **Tujajure !** — joke rounds with multiple-choice punchlines.

Plus history, contributions, duels, daily challenge, leaderboard, favorites and
achievements screens.

## Tech stack

- Java 11 + XML layouts with **ViewBinding**
- **Retrofit 2 / OkHttp / Gson** for the Kazinduzi (Laravel / Sanctum) API
- **EncryptedSharedPreferences** for bearer-token storage
- Legacy **MyMartin** logistics module: **Volley** + **ObjectBox** offline cache
- **Firebase Analytics + Crashlytics**, **WorkManager** for background sync
- `minSdk 23`, `target/compileSdk 36`

## Getting started

1. Open the project in Android Studio (or run from the CLI).
2. Build a debug APK:

   ```bash
   ./gradlew :app:assembleDebug
   ```

3. The app opens to the auth screen; register or log in against the Kazinduzi
   backend. Development/production base URLs are configured in
   `app/src/main/java/org/kazinduzi/rinjora/network/ApiConfig.java`.

> Cleartext HTTP is enabled (`usesCleartextTraffic`) for LAN/emulator dev.

## Structure

```
app/src/main/java/org/kazinduzi/rinjora/
  auth/        legacy MyMartin login
  data/        repositories (rounds, contributions, …)
  game/        QuizFragment (sokwe/hera), TujajureFragment
  network/     Retrofitt API, DTOs, auth interceptor, token store
  rinjora/     activities & fragments for the Rinjora experience
  ui/          legacy MyMartin UI (repairs, workshop, tickets)
  util/        KirundiUi (100% Kirundi strings), text/score helpers
  docs/        implementation plans + API contracts (source of truth)
```

## Docs

Backend/API contracts and parity plans live in `app/src/main/java/org/kazinduzi/rinjora/docs/`
alongside a reference to the web prototype (`rinjora.html`).

## License

Proprietary — © Kazinduzi.