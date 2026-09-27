# SmartDock Borrower App (Eldroid_NullPoint) – Setup Guide

Native Android (Kotlin, View Binding) borrower app for **SmartDock**, the
RFID-based equipment borrowing system specified in `smartdock (1).docx`.
Backend: Firebase Authentication (Email/Password + Google) and Cloud Firestore.

For how the app is used and navigated, and how every action maps to Firestore,
see **[docs/USER_GUIDE.md](docs/USER_GUIDE.md)**.

## Project layout

| Path | Purpose |
|---|---|
| `MainActivity.kt` | Splash / router (Home if signed in, Login if onboarding done, else Landing) |
| `LandingActivity.kt`, `OnboardingActivity.kt` | Welcome page and 6-step "How it works" tutorial |
| `LoginActivity.kt`, `SignupActivity.kt`, `ForgotPasswordActivity.kt`, `ChangePasswordActivity.kt` | Authentication flows |
| `HomeActivity.kt` | Dashboard & navigation hub; live listeners on `equipment`, `transactions`, `notifications`; one-time "Add sample equipment" |
| `EquipmentListActivity.kt`, `ActivityHistoryActivity.kt`, `NotificationsActivity.kt` | Full-screen lists (share `activity_simple_list.xml`) |
| `EquipmentDetailActivity.kt` | Item detail with **Borrow / Return** actions |
| `BorrowConfirmationActivity.kt` | FR-05 receipt after a borrow |
| `ProfileActivity.kt` | Edit name, RFID card UID, reminder switch, change password, log out |
| `model/` | `User`, `Equipment`, `Transaction`, `AppNotification` Firestore document models |
| `util/SmartDockRepository.kt` | Every Firestore write (borrow/return transactions, seeding, profile, notifications) |
| `util/LoanPolicy.kt` | Pure borrowing rules and document shapes (unit-tested, mirrored by `firestore.rules`) |
| `util/SeedEquipment.kt` | The 10 sample boxes (mirrors `firestore/equipment.seed.json`) |
| `util/Notifier.kt`, `work/DueCheckWorker.kt` | FR-06 due-soon / overdue reminders (notification channel + WorkManager) |
| `util/Validators.kt`, `util/AuthErrors.kt`, `util/TimeFormat.kt`, `util/EquipmentImages.kt` | Helpers |
| `firestore.rules` | Security rules to paste into the Firebase console |
| `docs/USER_GUIDE.md` | Navigation & data-model guide |

## 1. Firebase project

1. https://console.firebase.google.com → create a project (or reuse one).
2. Add an **Android app** with package name `com.example.eldroid_nullpoint`.
3. Download **`google-services.json`** into `app/` (it is git-ignored on purpose).

## 2. Authentication providers

Firebase Console → **Authentication → Sign-in method** → enable
**Email/Password** and **Google**.

For Google Sign-In:
1. Copy the **Web client ID** (Authentication → Google, or Google Cloud →
   Credentials) into `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   ```
2. Add your SHA-1 / SHA-256 to the Android app in Project settings
   (`.\gradlew.bat signingReport` prints them) and re-download `google-services.json`.

## 3. Firestore

1. **Firestore Database → Create database**.
2. **Rules** tab → paste the contents of `firestore.rules` → **Publish**.
   The rules allow every signed-in borrower to read `equipment`, restrict writes to
   valid borrow/return transitions by the caller, and keep `users`, `transactions`
   and `notifications` private to their owner. Until they are published, every
   write fails with *PERMISSION_DENIED* and the app shows
   "Firestore refused the change…".
3. No indexes are required — all queries are equality-only.
4. Data: sign in and tap **Add sample equipment** on the dashboard (or create the
   documents from `firestore/equipment.seed.json` by hand).

## 4. Build & run

Requirements: Android Studio (Hedgehog or newer), JDK 17, Android SDK 34.

```powershell
.\gradlew.bat clean assembleDebug      # debug APK → app\build\outputs\apk\debug\
.\gradlew.bat testDebugUnitTest        # JVM unit tests (LoanPolicy, SeedEquipment, Validators, …)
```

Or open the project in Android Studio, let Gradle sync, and **Run**. Do a clean
build after changing `google-services.json` or `strings.xml`.

## 5. Common errors

- **"Developer console is not set up correctly" (Google Sign-In)** → SHA-1 not
  added / `google-services.json` not refreshed after adding it. Use e-mail login on
  emulators without Play Services.
- **`PERMISSION_DENIED` / "Firestore refused the change"** → `firestore.rules` not
  published, or the write is not a valid transition (e.g. returning an item you
  do not hold).
- **No reminder notifications** → allow notifications for the app (Android 13+),
  keep *Due-date reminders* on in Profile, and remember WorkManager runs at most
  every 15 minutes (opening Home triggers a check immediately).
