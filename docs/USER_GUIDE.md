# SmartDock Borrower App — User & Navigation Guide

SmartDock is an IoT-enabled, RFID-based equipment management system for shared
school equipment (see `smartdock (1).docx`, the Functional Requirements
Specification by team NullPoint). This Android app is the **borrower** side of that
system. It lets a registered borrower:

- see live availability of every SmartDock box (FR-10),
- borrow and return equipment (a stand-in for the RFID tap at the tower while the
  hardware is being built — FR-03 / FR-08 / FR-09),
- get a confirmation with item, box, borrow time and return time (FR-05),
- receive due-soon and overdue reminders (FR-06),
- review their own borrow/return history (FR-10),
- manage their profile and linked RFID card.

Everything the app shows comes from **Cloud Firestore** in real time, and every
action it performs is written back to Firestore, so the Firebase console always
mirrors what is on screen.

---

## 1. Screen map

```
Splash (MainActivity)
 ├─ already signed in ──────────────────────────────► Home
 ├─ signed out, onboarding done ────────────────────► Login
 └─ first launch ─► Landing ─(Get Started)─► Onboarding (6 steps)
                                                ├─ Create Account ─► Sign Up ─► Home
                                                └─ Log in ────────► Login
Login
 ├─ Login / Continue with Google ─► Home
 ├─ Forgot Password? ─► Forgot Password (sends reset e-mail) ─► back to Login
 └─ Sign up ─► Sign Up

Home (dashboard & hub)
 ├─ 🔔 bell (with unread badge) ─► Notifications ─(tap row)─► Equipment Detail
 ├─ 👤 person ─► Profile
 │                ├─ Change Password
 │                └─ Log out ─► Login
 ├─ ↻ refresh (re-subscribes the live listeners)
 ├─ Current Borrowing Status card ─► Equipment Detail
 ├─ My Borrowed Items rows ─► Equipment Detail
 ├─ Equipment Availability rows ─► Equipment Detail
 │     └─ See all ─► Equipment (full list) ─(tap row)─► Equipment Detail
 ├─ Recent Activity rows ─► Equipment Detail
 │     └─ See all ─► My Activity (full history) ─(tap row)─► Equipment Detail
 └─ "Add sample equipment" (only while the equipment collection is empty)

Equipment Detail
 ├─ Borrow (only when the item is available) ─► Borrowing Confirmed ─► Home / View item
 └─ Return (only when *you* hold the item) ─► toast, screen updates live
```

Navigation is hub-and-spoke: **Home** is the hub, every other screen is one or two
taps away, and the **← back arrow** (top-left) or the system Back gesture returns
to the previous screen.

---

## 2. Screen-by-screen

### 2.1 Splash
Shown for a moment on launch. Routes automatically:
- signed in → **Home**
- signed out but you have seen the tutorial before → **Login**
- first launch → **Landing**

### 2.2 Landing → Onboarding ("How it works")
Six illustrated steps explaining the physical SmartDock flow (prepare your card,
tap to start, take your equipment, borrowing confirmed, you're all set, tap &
return). **Skip** or **Next** moves through them; the last step offers **Create
Account** or **Log in**.

### 2.3 Sign Up
First name, last name, e-mail, password + confirmation. Passwords need 8+
characters with upper, lower, number and symbol. **Continue with Google** is also
available. On success a profile document `users/{uid}` is created and you land on
Home.

### 2.4 Login
E-mail/password or Google. **Forgot Password?** opens a screen that e-mails a
reset link. Friendly error messages are shown inline under the fields.

### 2.5 Home (dashboard)
| Area | What it shows | What you can do |
|---|---|---|
| Greeting | "Good morning, ‹first name›" from your profile | — |
| Header icons | 🔔 Notifications (red badge = unread count), 👤 Profile, ↻ Refresh | Tap to open |
| RFID warning (amber) | Appears when no RFID card UID is linked to your account | Link one in Profile |
| **Current Borrowing Status** | Your most urgent borrowed item: photo, box, borrowed at, due at, and a pill reading *Due in 3h 20m / Due soon / Overdue by 2h* | Tap → Equipment Detail |
| **My Borrowed Items** | Appears only when you hold more than one item | Tap a row → detail |
| **Equipment Availability** | First 4 boxes with status pills *Available / Borrowed / Yours / Overdue* and "x of y available" | **See all** → full list; tap a row → detail |
| **Recent Activity** | Your last 4 borrow/return events | **See all** → full history |
| Empty-tower card | "No equipment has been registered… **Add sample equipment**" | Seeds the 10 sample boxes (once) |

All three sections are live Firestore listeners — when anyone borrows or returns,
the dashboard updates without a refresh.

### 2.6 Equipment (See all)
Every SmartDock box ordered by box number, with the same status pills as Home and
a live "x of y available" subtitle. Tap a row for details.

### 2.7 Equipment Detail
Photo, name, status pill, a one-line hint on what to do next, then rows for
Category, SmartDock box, Currently with (You / Another borrower), and — for your
own loans — Borrowed at and Due at.

- **Borrow** button appears when the item is *Available*. Tapping it asks for
  confirmation, then records the loan (due in **3 hours**) and opens the
  **Borrowing Confirmed** screen.
- **Return** button appears when *you* currently hold the item. Confirm → the box
  becomes available again and a toast confirms the return.
- If someone else holds the item, no button is shown.
- If two people race for the same box, the second attempt is refused
  ("Someone else just borrowed this item.") — the write is a Firestore transaction
  that re-checks the box first.

### 2.8 Borrowing Confirmed (FR-05)
A receipt with the item photo and name, **Box location**, **Borrowed at** and
**Return by**, plus a note that reminders will follow. **Back to dashboard** or
**View item**.

### 2.9 My Activity (See all)
Your complete borrow/return history, newest first. Each row shows the event
(Borrowed / Returned …), the box and the time. Tap a row to open the item.

### 2.10 Notifications
Your inbox, newest first. Unread rows have a bold title and a green dot; read rows
are muted. Types:
- **Borrowed** / **Returned** — written the moment you borrow or return.
- **Due soon** — one hour before the due time.
- **Overdue** — once the due time has passed.

Tapping a row marks it read and opens the item. **Mark all read** (top-right)
clears the badge on Home. Due-soon and overdue reminders are also delivered as
Android system notifications (channel *Due-date reminders*); tapping one opens the
item's detail page.

### 2.11 Profile
- Identity card: initials, full name, e-mail, sign-in provider.
- **Personal details**: edit first and last name.
- **RFID card**: status pill (*RFID card registered* / *No RFID card linked*) and a
  field for the card UID (e.g. `04 A3 2B 1C`). Normally the administrator registers
  this; the field exists so demos can show the link.
- **Preferences**: *Due-date reminders* switch (asks for notification permission on
  Android 13+ when turned on).
- **Save changes** writes everything to `users/{uid}`.
- **Account**: *Change Password* (e-mail accounts only) and *Log out*.

### 2.12 Change Password
Enter the current password, then the new one twice. Google accounts do not see
this option because they have no password to re-authenticate with.

---

## 3. How actions map to Firestore

| You do… | Firestore changes (visible in the console) |
|---|---|
| Sign up / first Google login | `users/{uid}` created with `firstName, lastName, email, provider, rfidCardUid, notificationsEnabled, createdAt` |
| Tap **Add sample equipment** | `equipment/box_01 … box_10` created, all `status: "available"` |
| Tap **Borrow** on box 2 | In one transaction: `equipment/box_02` → `status: "borrowed", borrowedBy: <uid>, borrowedByName, borrowedAt, dueAt (+24 h)`; new `transactions/{autoId}` with `type: "borrow"`; new `notifications/{uid}_box_02_{borrowedAt}_borrow` (unread) |
| Tap **Return** on box 2 | In one transaction: `equipment/box_02` → `status: "available"`, loan fields cleared; new `transactions/{autoId}` with `type: "return"`; new `notifications/…_return` |
| Background check finds a loan due within 1 h / past due | `notifications/{uid}_{boxId}_{borrowedAt}_due_soon` or `…_overdue` (created once per loan; a system notification is posted) |
| Tap a notification row / **Mark all read** | `notifications/{id}.read → true` |
| **Save changes** in Profile | `users/{uid}.firstName / lastName / rfidCardUid / notificationsEnabled` updated |

### Data model

**`users/{uid}`**

| Field | Type | Notes |
|---|---|---|
| uid | string | equals the document id |
| firstName, lastName | string | editable in Profile |
| email | string | from Firebase Auth |
| provider | string | `"email"` or `"google.com"` |
| rfidCardUid | string | `""` until a card is linked |
| notificationsEnabled | boolean | due-date reminders on/off |
| createdAt | number | epoch millis |

**`equipment/{boxId}`** (`box_01` … `box_10`)

| Field | Type | Notes |
|---|---|---|
| name, category | string | e.g. "Epson LCD Projector", "Audio Visual" |
| boxNumber | number | 1-based position in the tower |
| status | string | `"available"` or `"borrowed"` |
| borrowedBy | string | borrower uid or `""` |
| borrowedByName | string | display name or `""` |
| borrowedAt, dueAt | number | epoch millis, `0` when available |

**`transactions/{autoId}`**

| Field | Type |
|---|---|
| uid, userName | string |
| equipmentId, equipmentName | string |
| boxNumber | number |
| type | `"borrow"` \| `"return"` (the tower may also log `"overdue"` / `"alert"`) |
| timestamp | number (epoch millis) |

**`notifications/{uid}_{equipmentId}_{borrowedAt}_{type}`**

| Field | Type |
|---|---|
| uid | string |
| type | `"borrow"` \| `"return"` \| `"due_soon"` \| `"overdue"` |
| title, body | string |
| equipmentId, equipmentName | string |
| boxNumber | number |
| read | boolean |
| createdAt | number (epoch millis) |

The deterministic notification id is what guarantees **one** due-soon and **one**
overdue reminder per loan, no matter how often the background check runs.

Queries are equality-only (`uid == me`, `borrowedBy == me`) with client-side
sorting, so no composite indexes need to be created.

---

## 4. Setting up a fresh Firebase project

1. Firebase console → create project → add an Android app with package
   `com.example.eldroid_nullpoint`; download `google-services.json` into `app/`.
2. **Authentication → Sign-in method**: enable *Email/Password* and *Google*. For
   Google, add your debug SHA-1 (`.\gradlew.bat signingReport`) to the Android app
   and re-download `google-services.json`; make sure
   `default_web_client_id` in `app/src/main/res/values/strings.xml` matches the
   web client id in that file.
3. **Firestore Database → Create database** (any region).
4. **Firestore → Rules**: paste the contents of `firestore.rules` from this repo and
   **Publish**. Until this is done every write shows *"Firestore refused the
   change. Ask the administrator to deploy the app's security rules."*
5. Run the app, sign up, and tap **Add sample equipment** on the dashboard.

---

## 5. Demo script (5 minutes)

1. Sign up as borrower A → Home shows the empty-tower card → **Add sample
   equipment** → 10 boxes appear; console shows `equipment/box_01…10`.
2. Open *Wireless Microphone Set* (Box 2) → **Borrow** → Confirm → Borrowing
   Confirmed screen. Console: `box_02.status = borrowed`, a new `transactions` row,
   a new `notifications` row. Home: hero card shows the loan, bell badge shows 1.
3. Sign in as borrower B on another device/emulator → Box 2 reads *Borrowed*, no
   Borrow button. Open any available box and borrow it — both dashboards update
   live.
4. Back as A: Box 2 → **Return** → console flips back to available; *My Activity*
   lists both events.
5. To show reminders quickly, edit `equipment/box_0X.dueAt` in the console to a
   time ~30 minutes ahead (or in the past) while A holds it, then pull down /
   reopen Home — the background check runs on open and posts *Due soon* /
   *Overdue* once, both as a system notification and in the Notifications screen.
6. Profile → change the name and enter an RFID UID → **Save changes** → console
   `users/{uid}` updates; the Home greeting and RFID banner follow.

---

## 6. Limitations & notes

- **Reminders run on the device.** Android schedules the check every 15 minutes
  at most (WorkManager minimum) and may delay it under battery saving. The check
  also runs each time Home opens and right after a borrow. Reminders only cover
  the account signed in on that phone; the FRS foresees Cloud Functions for
  server-side scheduling.
- **Notification permission** is requested once on Android 13+. If denied,
  reminders still appear in the in-app Notifications screen; re-enable from the
  Profile switch or system settings.
- **In-app Borrow/Return** stands in for the ESP32 tower. When the hardware
  exists it will write exactly the same document shapes, so the app needs no
  changes — the buttons can simply be hidden.
- Google Sign-In needs a Play-Services emulator image and a registered SHA-1;
  e-mail/password works everywhere.
- Other borrowers' names are never shown on the borrower app — only "Another
  borrower" — per the FRS split between borrower and administrator views.
