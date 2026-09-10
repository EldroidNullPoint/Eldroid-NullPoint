# SmartDock – Whole-System Checklist

One place to track the **entire** SmartDock project, not just the ELDROID MVP
activity. Split by owner, mapped to every FR / NFR in the team's Functional
Requirements Specification (Sept 2, 2026).

| Owner | Role (from the spec) | Scope |
|---|---|---|
| **Dana** | Mobile App Developer | Borrower Android app (this repo) |
| **Bryce** | Mobile App Developer | Borrower Android app (this repo) |
| **Dariel** | Cloud & Backend Lead | Firebase project, Firestore, rules, Cloud Functions, FCM, admin web dashboard |
| **Han** | IoT / Hardware Lead | Docking tower, ESP32 firmware, sensors |

Status legend: ☑ done · ☐ to do · ◐ partially done.
For the firmware-only detail see `ESP32_MVP_CHECKLIST.md`.

---

## 1. Shared foundation (everyone)

- [x] Firebase project created, Android app registered, `google-services.json` issued.
- [x] Firebase Authentication enabled: Email/Password + Google.
- [x] Firestore database created.
- [ ] `firestore.rules` from this repo published in the Firebase console.
- [ ] Device account for the tower created in Authentication (e.g. `tower-01@smartdock.local`).
- [ ] Admin role decided: either a custom claim (`admin: true`) set by a Cloud
      Function, or an `admins/{uid}` collection. Rules and web dashboard depend on it.
- [ ] Agreed box → item table (`box-01` … `box-06`) written to `equipment/` once and
      treated as the source of truth (see `ESP32_MVP_CHECKLIST.md` §0).
- [ ] Default loan period agreed (checklist assumes **2 hours**) and grace period for
      escalation agreed (suggest **30 minutes**, FR-07).
- [ ] One shared test borrower account + one registered RFID card for demos.
- [ ] GitHub: `main` protected, all work on feature branches, merged by PR.

---

## 2. Firestore data model (Dariel owns, everyone follows)

| Collection | Purpose | Status |
|---|---|---|
| `users/{uid}` | borrower profile: firstName, lastName, email, provider, createdAt, lastLoginAt, loginCount | ☑ implemented in app |
| `users/{uid}.rfidCardUid` | uppercase-hex card UID used by the tower to identify the tapper | ☐ add field |
| `users/{uid}.flagged` / `flaggedReason` | set by overdue escalation (FR-07) | ☐ |
| `users/{uid}.fcmToken` | device token for push notifications | ☐ |
| `equipment/{boxId}` | one doc per box: name, category, boxNumber, status, borrowedBy, borrowedByName, borrowedAt, dueAt | ☑ read by app; ☐ written by tower |
| `transactions/{id}` | append-only events: uid, userName, equipmentId, equipmentName, boxNumber, type (borrow/return/overdue/alert), timestamp | ☑ read by app; ☐ written by tower |
| `alerts/{id}` (optional) | unauthorized-removal / overdue alerts with `acknowledged` flag for the admin dashboard | ☐ decide if separate from `transactions` |
| `sessions/{id}` (optional) | active RFID sessions if the team wants them visible in the cloud | ☐ optional |

- [ ] Security rules cover every collection above (see §5).
- [ ] Composite indexes created for any query the web dashboard needs
      (e.g. `transactions` by `uid` + `timestamp desc`).

---

## 3. Borrower mobile app (Dana, Bryce) – this repo

### 3.1 ELDROID MVP activity (graded)
- [x] Model-View-Presenter on every screen, presenters Android-free and unit-tested.
- [x] Login (email/password + Google).
- [x] Register with strict validation and strong passwords.
- [x] Forgot Password.
- [x] Change Password (email accounts; disabled for Google).
- [x] Home/Dashboard with SmartDock data: stats, my borrowed items with due chips,
      equipment availability, recent activity, account card.
- [x] 64 unit tests passing; debug APK builds.
- [ ] Run on a real device with the real `google-services.json`; smoke-test all five screens.

### 3.2 Real-time monitoring (FR-05, FR-10, NFR-01)
- [ ] Replace one-shot `get()` with Firestore **snapshot listeners** on `equipment`
      and the user's `transactions` so the dashboard updates live.
- [ ] Pull-to-refresh as a fallback.
- [ ] Borrow confirmation state after a tower event: item name, borrow time,
      required return time, box location (FR-05).

### 3.3 Notifications (FR-06, FR-07)
- [ ] Add Firebase Cloud Messaging; save the device token to `users/{uid}.fcmToken`.
- [ ] Handle "due soon", "overdue" and "alert" pushes; tapping opens the dashboard.
- [ ] Request `POST_NOTIFICATIONS` permission on Android 13+.
- [ ] In-app "flagged account" banner when `users/{uid}.flagged == true`.

### 3.4 Borrower features from the use-case diagram
- [ ] **View Transaction History** screen: full paginated list, not just the last 10.
- [ ] **View Borrowed Items and Details** screen: tap an item for borrow time,
      due time, box number, remaining time.
- [ ] **RFID card registration**: borrower enters/scans their card UID, or admin
      assigns it (writes `users/{uid}.rfidCardUid`).
- [ ] Profile screen (name edit) – optional.

### 3.5 Hardening
- [ ] Remove or debug-flag `seedSampleDataIfEmpty` once the tower writes real data.
- [ ] Offline state: show cached data and a "no connection" banner.
- [ ] Release build: signing config, `isMinifyEnabled = true`, ProGuard rules for Firebase.
- [ ] App icon and splash finalised with SmartDock branding.
- [ ] Instrumented/UI test for the login → dashboard flow (optional).

---

## 4. Tower firmware (Han) – ESP32

Detailed steps are in `ESP32_MVP_CHECKLIST.md`; this is the summary.

### 4.1 Hardware (NFR-06)
- [ ] ESP32, MFRC522 (SPI), MCP23017 (I2C), one IR break-beam / photointerrupter per box,
      green + red LEDs, active buzzer, 5 V supply wired and tested individually.
- [ ] Docking tower built: stacked open boxes, one item per box, sensors aligned.
- [ ] Wiring diagram and pin map documented in the repo (`docs/hardware/`).

### 4.2 Firmware – Phase 1 demo
- [ ] Wi-Fi + NTP time; all timestamps in **epoch milliseconds**.
- [ ] Device-account sign-in to Firebase Auth REST; token refresh.
- [ ] RFID tap → look up `users` by `rfidCardUid`; reject unknown cards (NFR-07).
- [ ] Time-boxed borrow session; first removed item bound to borrower (FR-01, FR-03).
- [ ] Return session; matching item placement attributed to borrower (FR-08, FR-09).
- [ ] Unauthorized removal: buzzer + red LED + `alert` transaction (FR-04).
- [ ] Item-presence monitoring loop with debouncing (FR-02, NFR-07).
- [ ] LED / buzzer feedback for tap, borrow, return, reject, alarm (NFR-04).

### 4.3 Firmware – Phase 2
- [ ] Wi-Fi and I2C auto-reconnect without restart; bounded event queue flushed
      in order after reconnect (FR-11, NFR-03).
- [ ] Boot-time reconciliation of sensor state vs. cloud state.
- [ ] Read box → item table from Firestore at boot instead of hard-coding.
- [ ] Duplicate-tap and malformed-payload guards (NFR-07).
- [ ] Optional: WebSocket / Firestore listen for admin-triggered actions.

---

## 5. Cloud & backend (Dariel) – Firebase

### 5.1 Security (NFR-02)
- [ ] Rules: borrowers read all `equipment`, read only their own `transactions`
      and `users/{uid}`; **only the device account and admins write** `equipment`
      and `transactions`; admins read everything.
- [ ] Rules unit-tested with the Firebase emulator (`@firebase/rules-unit-testing`).
- [ ] HTTPS only (default for Firebase SDK and REST); no API keys committed to git.
- [ ] Verification evidence for NFR-02: screenshots of denied unauthenticated reads.

### 5.2 Cloud Functions
- [ ] **Scheduled due-date check** (every 1–5 min): for each `borrowed` box with
      `dueAt < now` create one `overdue` transaction per loan (FR-06).
- [ ] **Reminder** 15 min before `dueAt` (FR-06).
- [ ] **Escalation**: overdue beyond grace period → set `users/{uid}.flagged = true`
      and create an admin alert (FR-07).
- [ ] **Notification fan-out**: on new `transactions` doc, send FCM to the borrower
      (`borrow`, `return`, `overdue`) and to all admins (`alert`, `overdue`).
- [ ] **Admin claims**: callable function to grant/revoke `admin: true`.
- [ ] Optional: HTTPS endpoint the ESP32 can POST events to, so the tower never
      needs Firestore write rules (simpler firmware, stronger security).

### 5.3 Operations
- [ ] Firestore backups / export schedule.
- [ ] Budget alert on the Firebase project.
- [ ] Emulator suite config committed for local development.

---

## 6. Administrator web dashboard (Dariel) – React

Each item maps to the admin use cases in the diagram and FR-10.

- [ ] Project scaffolded (Vite + React + Firebase SDK), deployed to Firebase Hosting.
- [ ] Admin login (Email/Password) gated by the admin claim.
- [ ] **Monitor Live Status and Alerts**: grid of boxes with live status, borrower,
      due time; alert feed with acknowledge button.
- [ ] **Manage Users (Borrowers)**: list, search, assign/replace `rfidCardUid`,
      flag/unflag, disable.
- [ ] **Manage Equipment Items**: create/edit item name and category per box.
- [ ] **Manage Boxes and Sensors**: box numbering, enable/disable a box, last-seen
      heartbeat from the tower.
- [ ] **View All Borrowing Records**: full `transactions` table with filters by
      user, item, type, date range.
- [ ] **Manage Due Dates and Overdue**: extend a due time, mark returned manually,
      see overdue/escalated list.
- [ ] **View Security Events**: unauthorized-removal log.
- [ ] **Generate / Export Reports**: CSV export of transactions per date range.
- [ ] **Manage Preferences**: default loan period, grace period, reminder lead time
      stored in `settings/global` and read by Cloud Functions.

---

## 7. Integration & verification (whole team)

Maps to the spec's "How it will be verified" column.

- [ ] **NFR-01 Performance**: borrow on tower → app and web update within a few
      seconds; record measured latency.
- [ ] **NFR-02 Security**: unauthenticated and wrong-user reads/writes denied (rules tests).
- [ ] **NFR-03 Reliability**: cut Wi-Fi mid-session, restore, confirm queued events sync.
- [ ] **NFR-04 Usability**: 3–5 borrowers run the demo script unaided; note confusion points.
- [ ] **NFR-05 Connectivity**: SPI (RFID), I2C (expander), Wi-Fi/HTTPS verified in logs.
- [ ] **NFR-06 Compatibility**: every listed component tested in the integrated tower.
- [ ] **NFR-07 Data integrity**: unknown card, double tap, malformed payload all rejected;
      Firestore unchanged afterwards.
- [ ] **End-to-end demo (FR-01 → FR-11)**: tap, remove, log, notify, return, update,
      alert – recorded on video for the presentation.

---

## 8. Documentation & submission

- [x] `ARCHITECTURE.md` – app MVP architecture.
- [x] `SETUP.md` – Firebase / Google setup for the app.
- [x] `docs/ESP32_MVP_CHECKLIST.md` – firmware detail.
- [ ] `docs/hardware/` – wiring diagram, pin map, bill of materials.
- [ ] `functions/README.md` – how to deploy Cloud Functions and the emulator.
- [ ] `web/README.md` – how to run and deploy the admin dashboard.
- [ ] Final report / presentation: architecture diagram updated to match what was built,
      traceability table (§11 of the spec) filled with evidence links.
- [ ] Demo video and screenshots of every app screen and dashboard page.

---

## Requirement traceability

| Req | Owner(s) | Where it is satisfied | Status |
|---|---|---|---|
| FR-01 RFID authentication | Han | firmware §4.2 | ☐ |
| FR-02 Item-presence monitoring | Han | firmware §4.2 | ☐ |
| FR-03 Authorized borrow detection | Han | firmware §4.2 | ☐ |
| FR-04 Unauthorized removal alert | Han, Dariel | firmware §4.2 + FCM §5.2 | ☐ |
| FR-05 Borrow confirmation | Dana/Bryce | app §3.2 | ◐ (dashboard shows it after refresh) |
| FR-06 Due/overdue notifications | Dariel, Dana/Bryce | functions §5.2 + app §3.3 | ☐ |
| FR-07 Overdue escalation | Dariel | functions §5.2 + web §6 | ☐ |
| FR-08 Return processing | Han | firmware §4.2 | ☐ |
| FR-09 Automatic availability update | Han | firmware §4.2 | ☐ |
| FR-10 Real-time monitoring & history | Dana/Bryce, Dariel | app §3.2, §3.4 + web §6 | ◐ (app dashboard done, not live) |
| FR-11 Automatic reconnection | Han | firmware §4.3 | ☐ |
| NFR-01 Performance | all | §7 | ☐ |
| NFR-02 Security | Dariel | §5.1 | ◐ (rules drafted) |
| NFR-03 Reliability | Han | §4.3 | ☐ |
| NFR-04 Usability | all | §4.2 feedback + §7 | ◐ (app screens done) |
| NFR-05 Connectivity | Han | §4.1 | ☐ |
| NFR-06 Compatibility | Han | §4.1 | ☐ |
| NFR-07 Data integrity | Han, Dariel | §4.2, §4.3, §5.1 | ☐ |
