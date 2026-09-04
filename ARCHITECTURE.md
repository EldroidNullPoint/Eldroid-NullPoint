# SmartDock Borrower App – Architecture (MVP)

SmartDock is an IoT, RFID-based equipment borrowing system (see the team's
Functional Requirements Specification). This Android app is the **borrower
mobile application**: it authenticates borrowers and shows equipment
availability, the borrower's own borrowed items with due times, and recent
borrow/return activity synced from the SmartDock tower through Firebase.

The app follows MVP strictly: every screen is a `Contract` with a `View`
interface and a `Presenter` interface, an Activity that implements the View,
and a Presenter class that holds all the logic. Presenters contain **no Android
imports** and talk to Firebase only through the `AuthRepository` interface, so
each one is covered by plain JVM unit tests.

```
                 user taps                 calls interface methods
   Activity  ─────────────────▶  Presenter  ──────────────────────▶  AuthRepository
   (View)    ◀─────────────────             ◀──────────────────────  (Model)
             renders via View               Result<T> callback         │
             interface only                                            ▼
                                                              FirebaseAuthRepository
                                                              (Firebase Auth + Firestore)
```

## Package layout

```
com.example.eldroid_nullpoint
├── base/
│   ├── BaseView.kt              showLoading / hideLoading / showMessage
│   └── BasePresenter.kt         attachView / detachView + null-safe `view`
├── data/                        MODEL
│   ├── AuthRepository.kt        auth interface the presenters depend on
│   ├── FirebaseAuthRepository.kt production implementation (only file that imports Firebase Auth)
│   ├── EquipmentRepository.kt   equipment + transaction interface
│   ├── FirestoreEquipmentRepository.kt  reads equipment/ and transactions/, seeds sample boxes
│   ├── SessionUser.kt           framework-free "who is signed in"
│   ├── AuthError.kt             user-safe error messages
│   └── Injection.kt             hands Activities the shared repositories
├── model/
│   ├── User.kt                  Firestore document users/{uid}
│   ├── Equipment.kt             Firestore document equipment/{id} (one monitored box)
│   └── EquipmentTransaction.kt  Firestore document transactions/{id}
├── util/Validators.kt           all input rules (pure Kotlin, no android.util.Patterns)
├── splash/                      MainActivity + SplashPresenter  (router)
├── login/                       LoginActivity + LoginPresenter
├── register/                    RegisterActivity + RegisterPresenter
├── forgotpassword/              ForgotPasswordActivity + ForgotPasswordPresenter
├── changepassword/              ChangePasswordActivity + ChangePasswordPresenter
└── home/                        HomeActivity + HomePresenter + DashboardData + adapters
```

## Responsibilities

| Layer | Does | Never does |
|---|---|---|
| **View** (Activity) | Reads text from fields, forwards clicks to the presenter, shows/hides errors, loading and toasts, navigates | Validate input, call Firebase, decide what happens next |
| **Presenter** | Validates via `Validators`, calls the repository, decides which View method to call | Import `android.*`, hold a Context, touch a widget |
| **Model** (`AuthRepository`) | Firebase Auth + Firestore calls, maps Firebase exceptions to friendly `AuthError`s | Know about screens |

### Lifecycle

```kotlin
override fun onCreate(...) {
    presenter = LoginPresenter(Injection.provideAuthRepository(this))
    presenter.attachView(this)
}
override fun onDestroy() {
    presenter.detachView()      // late Firebase callbacks find view == null and are ignored
    super.onDestroy()
}
```

## Validation rules (`util/Validators.kt`)

| Field | Rules |
|---|---|
| Email | required · ≤ 254 chars · `local@domain.tld` shape · 2+ letter TLD · no `..` · local part cannot start/end with `.` |
| First / last name | required · 2–40 chars · letters only (any language) · single spaces, `-` and `'` allowed between words · no digits/symbols |
| Password (register & change) | required · no spaces · 10–64 chars · uppercase + lowercase + digit + special · no char repeated 3+ times in a row (`aaa`) · no 4-char straight or keyboard sequence (`1234`, `abcd`, `4321`, `qwer`, `asdf`) · not based on a common word even in leet-speak (`P@ssw0rd`, `L3tMe1n`) · must not contain the user's first name, last name or email username |
| Confirm password | required · must equal password |
| Change password | current password required · new password passes all rules above · new ≠ current · re-authenticated against Firebase before the change |
| Login password | required (complexity is only enforced when a password is created) |

`Validators.passwordStrength()` also returns a 0–4 score (WEAK / FAIR / STRONG / VERY_STRONG) for a strength meter.

## Home / Dashboard data

`HomePresenter` chains four repository calls (profile -> seed sample data if
the equipment collection is empty -> equipment -> transactions) and produces a
single `DashboardData`:

| Section | Content |
|---|---|
| Header | initials avatar, "Hi, {first name}!", email |
| Overdue banner | shown only when the borrower has overdue items |
| Stats | available boxes, borrowed boxes, the borrower's active items |
| My borrowed items | box number, item, borrow time, "Due in 1h 20m" / "Overdue by 45m" chip |
| Equipment availability | every box with Available / Borrowed / Yours / Overdue chip |
| Recent activity | newest 10 borrow / return / overdue events for this borrower |
| Account | sign-in method, member since, last login, total logins, Change Password |

### Firestore collections

| Collection | Written by | Fields |
|---|---|---|
| `users/{uid}` | the app | firstName, lastName, email, provider, createdAt, lastLoginAt, loginCount |
| `equipment/{id}` | SmartDock tower / Cloud Functions (sample data seeded by the app) | name, category, boxNumber, status (`available`/`borrowed`), borrowedBy, borrowedByName, borrowedAt, dueAt |
| `transactions/{id}` | SmartDock tower / Cloud Functions (sample data seeded by the app) | uid, userName, equipmentId, equipmentName, boxNumber, type (`borrow`/`return`/`overdue`/`alert`), timestamp |

**Sample data:** until the ESP32 tower is connected, the first signed-in user
to open the dashboard seeds six sample boxes and three sample transactions
(one box is borrowed by that user, one is overdue for another borrower).
Delete the `equipment` collection in the Firebase console to re-seed.

## Tests

`app/src/test` contains a `FakeAuthRepository` and one test class per presenter
plus `ValidatorsTest`. Run them with:

```
./gradlew :app:testDebugUnitTest
```
