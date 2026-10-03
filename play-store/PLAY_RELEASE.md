# Releasing BoardFlow on Google Play

Everything in this folder is ready to paste or upload. The steps marked **(you)** happen in Play Console and need your account.

## 1. Developer account (you)

1. Create a developer account at play.google.com/console. Choose a **personal** account unless you have a company.
2. Pay the one-time registration fee and complete identity verification (an ID document, and for personal accounts a verified phone number).
3. **Testing requirement for new personal accounts:** before the app can go to Production, it must run in a **closed test with at least 12 testers who stay opted in for 14 days in a row**. Plan for that: friends from your board game group are ideal.

## 2. Create the app (you)

- App name: BoardFlow: Board Game Journal. Default language: English. App, Free.
- Accept the declarations.

## 3. App signing: choose before the first upload (you)

Play re-signs your app. You choose with which key:

- **Option A (recommended for you): use your existing key.** In App integrity, App signing, choose to export and upload a key from a Java keystore, and follow Google's steps. It uses Google's PEPK tool to encrypt `app/release.jks`. Then Play builds and your GitHub APK carry the same signature: people can move between them without uninstalling, and the Google sign-in fingerprint stays the same.
- **Option B: let Google generate the key.** Simpler. But Play-installed BoardFlow and the GitHub APK then have different signatures (switching means uninstalling first). You must also add Play's app signing SHA-1 (shown under App integrity) to the OAuth client in Google Cloud Console, or Google sign-in will fail in Play builds.

Either way `app/release.jks` is your **upload key**. Keep it and its passwords backed up somewhere safe outside this PC.

## 4. Upload the build

- Build: `./gradlew.bat :app:bundleRelease -Djavax.net.ssl.trustStoreType=Windows-ROOT`
- File: `app/build/outputs/bundle/release/app-release.aab` (version 6.0.1, code 601, targets Android 16 / API 36)
- **(you)** Testing, Closed testing: create a track, add your testers' Google accounts (or a Google Group), upload the .aab, add release notes, roll out. Share the opt-in link with the testers.
- Each new upload needs a higher `versionCode` in `app/build.gradle.kts`.

## 5. App content forms (you, using these files)

- **Privacy policy:** a public URL. Publish `privacy-policy.md` (in this repo on GitHub, or on GitHub Pages), replacing CONTACT_EMAIL first.
- **Data safety:** see `data-safety.md`.
- **Ads:** No ads.
- **App access:** most features need a BoardGameGeek account. Add instructions for reviewers. You can provide a test BGG account, or explain that the app works offline without one (Log Play, Journal, Quick Guides).
- **Content rating:** fill in the questionnaire. No violence, no gambling with real money, no user-to-user chat: expect a rating of Everyone / PEGI 3.
- **Target audience:** 18+ or 13+. Not directed at children.
- **News app:** No. **Health apps:** No. **Government app:** No. **Financial features:** None.
- **Photo and video permissions:** the app uses `READ_MEDIA_IMAGES` for picking score sheet photos. Play may ask you to justify this, or to switch to the Android photo picker. If it does, tell me and I'll change the app to use the photo picker (no permission needed).

## 5b. Google sign-in on a public app (you, in Google Cloud Console)

BoardFlow asks for two Google scopes: `drive.file` (only the folders and QR images the app creates itself) and `spreadsheets` (the sheet you connect). Neither is a restricted scope, so no third-party security assessment is needed. `spreadsheets` is a **sensitive** scope: before the app is public, submit the OAuth consent screen for verification (app name, logo, the privacy policy URL, and a short video of the sign-in and sync). Until it is verified, sign-in shows an "unverified app" warning and works for at most 100 users. For testing, add your testers as test users on the OAuth consent screen.

On the consent screen, list exactly these scopes: `.../auth/drive.file` and `.../auth/spreadsheets`. Remove the full `.../auth/drive` scope if it is still listed from earlier versions.

Google sign-in is optional in BoardFlow, so the rest of the app works without it.

## 6. Store listing (you, using these files)

- Text: `listing.md`
- Graphics: `graphics/icon-512.png`, `graphics/feature-graphic-1024x500.png`, `graphics/phone/*.png`
- The screenshots use placeholder players (John Doe, Jane Doe, Jack).

## 7. Production

After 14 days of closed testing with 12+ testers, apply for production access in the Dashboard, answer the questions about your test, then promote the release to Production.
