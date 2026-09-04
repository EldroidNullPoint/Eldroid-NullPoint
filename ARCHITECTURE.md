# Architecture – Model-View-Presenter (MVP)

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
│   ├── AuthRepository.kt        interface the presenters depend on
│   ├── FirebaseAuthRepository.kt production implementation (only file that imports Firebase Auth)
│   ├── SessionUser.kt           framework-free "who is signed in"
│   ├── AuthError.kt             user-safe error messages
│   └── Injection.kt             hands Activities the shared repository
├── model/User.kt                Firestore document users/{uid}
├── util/Validators.kt           all input rules (pure Kotlin, no android.util.Patterns)
├── splash/                      MainActivity + SplashPresenter  (router)
├── login/                       LoginActivity + LoginPresenter
├── register/                    RegisterActivity + RegisterPresenter
├── forgotpassword/              ForgotPasswordPresenter (+ Activity in the UI pass)
├── changepassword/              ChangePasswordPresenter (+ Activity in the UI pass)
└── home/                        HomeActivity + HomePresenter + DashboardData
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

`HomePresenter` loads `users/{uid}` and produces a `DashboardData`:
full name, email, initials, sign-in provider, member-since date, last login
date/time, login count and whether the account can change its password.
`loginCount` and `lastLoginAt` are updated on every successful login by the
repository.

## Tests

`app/src/test` contains a `FakeAuthRepository` and one test class per presenter
plus `ValidatorsTest`. Run them with:

```
./gradlew :app:testDebugUnitTest
```
