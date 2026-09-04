# Eldroid_NullPoint – Login & Sign Up Setup Guide

This project contains a fully coded Login / Sign Up flow (Kotlin, View Binding,
Firebase Auth + Firestore, Google Sign-In). Everything compiles
and runs, but it is wired to **placeholder credentials** — you must plug in
your own Firebase / Google keys before the network calls will work.
Follow the steps below in order.

## What's included

The code follows the **Model-View-Presenter** pattern; see `ARCHITECTURE.md`
for the full breakdown. In short:

| Package | Purpose |
|---|---|
| `splash/` | `MainActivity` + `SplashPresenter` – routes to Home if already logged in, else Login |
| `login/` | Email/password login, Google login, "Forgot password" |
| `register/` | First/last name, email, password + confirm with strict validation, Google sign up |
| `forgotpassword/` | Sends the Firebase password-reset email |
| `changepassword/` | Re-authenticates, then updates the password (email/password accounts only) |
| `home/` | Dashboard: name, email, provider, member since, last login, login count, Logout |
| `data/` | `AuthRepository` interface + `FirebaseAuthRepository` (the only place Firebase Auth is called) |
| `util/Validators.kt` | Every input rule (email, names, strong passwords, confirmation) |
| `model/User.kt` | Firestore user document model |

On successful sign up / social login, a document is written to the Firestore
collection **`users/{uid}`** with `firstName`, `lastName`, `email`, `provider`,
`createdAt`, `lastLoginAt` and `loginCount`.

---

## 1. Create a Firebase project

1. Go to https://console.firebase.google.com and create a project (or use an existing one).
2. Add an **Android app** to it:
   - Package name: `com.example.eldroid_nullpoint`
   - (Optional) SHA-1 — required for Google Sign-In, see step 3.
3. Download the generated **`google-services.json`** and replace the placeholder
   file at `app/google-services.json` in this project.

## 2. Enable Auth providers

In Firebase Console → **Authentication → Sign-in method**, enable:
- **Email/Password**
- **Google**
- **Facebook** (you'll need the Facebook App ID + App Secret from step 4)

## 3. Google Sign-In — get the Web Client ID

1. In Firebase Console → Authentication → Sign-in method → Google → copy the
   **Web client ID** (also visible in Google Cloud Console → APIs & Services → Credentials).
2. Paste it into `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   ```
3. Add your app's **SHA-1** (and SHA-256) fingerprint to the Firebase Android app
   settings (Project settings → your app → Add fingerprint). Get it with:
   ```
   ./gradlew signingReport
   ```
   Re-download `google-services.json` after adding the fingerprint.

## 4. Facebook Login — get the App ID

1. Create an app at https://developers.facebook.com/apps.
2. Add the **Facebook Login** product, platform **Android**.
3. Under Settings → Basic, copy the **App ID** and **Client Token** (Settings → Advanced → Client Token).
4. Under Facebook Login → Settings, add package name `com.example.eldroid_nullpoint`,
   default activity class name `com.example.eldroid_nullpoint.LoginActivity`, and
   your **key hashes** (generate with the `keytool`/`openssl` command Facebook's
   docs provide for your debug/release keystore).
5. Update `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="facebook_app_id">1234567890123456</string>
   <string name="facebook_client_token">your_client_token</string>
   <string name="fb_login_protocol_scheme">fb1234567890123456</string>
   ```
   (`fb_login_protocol_scheme` = the literal text `fb` followed by your App ID.)
6. Back in Firebase Console → Authentication → Sign-in method → Facebook, paste
   the same App ID and App Secret, then copy the **OAuth redirect URI** Firebase
   shows you into the Facebook app's Login settings ("Valid OAuth Redirect URIs").

## 5. Firestore

1. In Firebase Console → **Firestore Database**, click "Create database" (start
   in test mode while developing, then lock it down — see `firestore.rules` in
   this project for a starting point).
2. No further code changes are needed; `SignupActivity`/`LoginActivity` already
   write to the `users` collection.

## 6. Build & run

Open the project in Android Studio, let Gradle sync (it will download the
Firebase/Google/Facebook libraries), then Run. If you edited
`google-services.json` or `strings.xml`, do a clean build first.

### Common errors

- **"Developer console is not set up correctly" (Google Sign-In)** → SHA-1 not
  added / `google-services.json` not refreshed after adding it.
- **Facebook login opens and immediately closes** → key hash not added to the
  Facebook app, or wrong `fb_login_protocol_scheme`.
- **`PERMISSION_DENIED` on Firestore writes** → your Firestore security rules
  don't allow the signed-in user to write to `users/{uid}`; see `firestore.rules`.
