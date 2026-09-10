# SmartDock Tower (ESP32) – MVP Checklist

This is the minimum the ESP32 firmware must do so the **borrower Android app**
(this repo) shows real data instead of the seeded samples. Everything the app
reads comes from two Firestore collections, `equipment` and `transactions`, so
the tower's only job toward the app is to keep those two collections correct.

Requirement IDs (FR-xx / NFR-xx) refer to the team's Functional Requirements
Specification. Tick the **Phase 1** boxes first; that is enough for a working
end-to-end demo. **Phase 2** finishes the spec.

---

## 0. Before writing firmware

- [ ] Get the Firebase **project ID** and **Web API key** from whoever owns the
      Firebase project (same project the app's `google-services.json` points to).
- [ ] Ask the app side to create a **device account** in Firebase Authentication
      (Email/Password, e.g. `tower-01@smartdock.local`). The ESP32 signs in with
      it so the Firestore rules (`signedIn()`) accept its writes.
- [ ] Ask the app side to delete the seeded sample documents in `equipment` and
      `transactions` once the tower can write its own (or reuse the same box IDs
      `box-01` … `box-06` and just overwrite them).
- [ ] Agree on the **box → item mapping** (box number, item name, category) and
      hard-code it in firmware for now. Example:

| Doc ID | boxNumber | name | category |
|---|---|---|---|
| `box-01` | 1 | Wireless Microphone | Audio |
| `box-02` | 2 | HDMI Cable (2 m) | Cables |
| `box-03` | 3 | Presentation Remote | Presentation |
| `box-04` | 4 | 65W USB-C Charger | Power |
| `box-05` | 5 | VGA to HDMI Adapter | Adapters |
| `box-06` | 6 | Portable Speaker | Audio |

---

## 1. Hardware bring-up

- [ ] ESP32 boots, joins 2.4 GHz Wi-Fi, prints its IP on Serial.
- [ ] **MFRC522** on SPI reads a MIFARE card and prints the UID as uppercase hex
      with no separators (e.g. `04A3B2C1`). This exact string is what gets stored.
- [ ] **MCP23017** on I2C is detected (address 0x20) and all box sensor pins read.
- [ ] One **IR break-beam / photointerrupter per box** reads `present` when the
      item is in the box and `absent` when removed. Verify each box individually.
- [ ] Green LED, red LED and buzzer each toggle from code.
- [ ] Time is synced from **NTP** at boot (`configTime`). Every timestamp the
      tower writes must be **Unix epoch in milliseconds** (`time(nullptr) * 1000LL`).
      The app compares `dueAt` against the phone's clock, so seconds or a wrong
      timezone will show every item as overdue or never due.

---

## 2. Firestore data contract (what the app reads)

### `equipment/{boxId}` – one document per box

| Field | Type | Value the tower must write |
|---|---|---|
| `name` | string | item name |
| `category` | string | item category |
| `boxNumber` | integer | 1, 2, 3 … |
| `status` | string | exactly `"available"` or `"borrowed"` (lowercase) |
| `borrowedBy` | string | Firebase **uid** of the borrower, or `""` when available |
| `borrowedByName` | string | borrower's full name, or `""` |
| `borrowedAt` | integer | epoch **ms** when borrowed, `0` when available |
| `dueAt` | integer | epoch **ms** when due, `0` when available |

### `transactions/{autoId}` – one document per event (append-only)

| Field | Type | Value |
|---|---|---|
| `uid` | string | borrower's Firebase uid (`""` for unauthorized alerts) |
| `userName` | string | borrower's full name |
| `equipmentId` | string | the box doc ID, e.g. `box-01` |
| `equipmentName` | string | item name |
| `boxNumber` | integer | box number |
| `type` | string | `"borrow"`, `"return"`, `"overdue"` or `"alert"` |
| `timestamp` | integer | epoch **ms** |

### `users/{uid}` – how the tower maps a card to a person

- [ ] The app side adds an `rfidCardUid` field (uppercase hex string) to each
      borrower's `users/{uid}` document. Until that exists in the app, an admin
      can type it in the Firebase console.
- [ ] On tap, the tower queries `users` where `rfidCardUid == <scanned UID>`.
      If exactly one document matches, use its doc ID as `uid` and
      `firstName + " " + lastName` as `userName`. If none matches, reject the tap
      (red LED + short beep) and write nothing (NFR-07).

---

## 3. Phase 1 – minimum working demo

### Cloud access
- [ ] Sign in the device account with the Firebase Auth REST endpoint
      (`accounts:signInWithPassword`) and keep the `idToken`.
- [ ] Refresh the token before it expires (every 50 minutes) using
      `securetoken.googleapis.com/v1/token`.
- [ ] Write to Firestore with the REST API using `PATCH …/documents/equipment/box-01?updateMask.fieldPaths=status&…`
      so only the changed fields are sent.
- [ ] Create transactions with `POST …/documents/transactions`.

### Borrow flow (FR-01, FR-03, FR-05)
- [ ] Valid card tap → open a **borrow session** for 15 s, green LED on.
- [ ] Ignore a second tap of the same card within 2 s (debounce, NFR-07).
- [ ] The **first box** whose sensor goes `present → absent` during the session
      is the borrowed item. Close the session immediately (one item per tap).
- [ ] Write `equipment/{box}`: `status="borrowed"`, `borrowedBy`, `borrowedByName`,
      `borrowedAt=now`, `dueAt=now + 2 h` (agree the default loan period).
- [ ] Append a `transactions` doc with `type="borrow"`.
- [ ] Session expires with no removal → green LED off, nothing written.

### Return flow (FR-08, FR-09)
- [ ] Card tap while that user has a borrowed item → open a **return session** (15 s).
- [ ] A box whose sensor goes `absent → present` and whose `borrowedBy` equals
      the tapping user is the returned item.
- [ ] Write `equipment/{box}`: `status="available"`, `borrowedBy=""`,
      `borrowedByName=""`, `borrowedAt=0`, `dueAt=0`.
- [ ] Append a `transactions` doc with `type="return"`.
- [ ] If the user has a borrowed item **and** wants to borrow another, decide
      the rule (simplest: a tap always opens a return session first if they
      hold something; if nothing is placed back within the window, it becomes
      a borrow session).

### Unauthorized removal (FR-04)
- [ ] Any box going `present → absent` with **no active session** → red LED on,
      buzzer for 3 s, append a `transactions` doc with `type="alert"`, `uid=""`,
      `userName=""`.
- [ ] Do **not** change the box's `status` (the item is still assigned; it is
      just missing). The admin dashboard handles it.

### Local feedback (NFR-04)
- [ ] Green LED: session open. Red LED + buzzer: rejected card or alarm.
- [ ] Short beep on accepted tap, double beep on successful borrow/return.

### Sensor hygiene
- [ ] Debounce every sensor change (ignore changes shorter than ~200 ms).
- [ ] On boot, read all boxes once and reconcile with Firestore: if a box is
      `borrowed` in the cloud but the item is present, treat it as returned by
      the recorded `borrowedBy` (write a `return` transaction).

---

## 4. Phase 2 – complete the spec

- [ ] **Overdue detection (FR-06, FR-07):** every minute, for each `borrowed`
      box with `dueAt < now`, append one `transactions` doc with `type="overdue"`
      (only once per loan; remember which boxes were already flagged).
      *Alternative:* a Firebase Cloud Function on a schedule does this instead of
      the tower, which is what the spec proposes.
- [ ] **Reminder before due** (FR-06): same loop, 15 min before `dueAt`.
- [ ] **Wi-Fi / cloud reconnection (FR-11, NFR-03):** if Wi-Fi drops, keep
      sensing, queue events in RAM (bounded, e.g. 20), and flush them in order
      once reconnected. Reconnect the I2C bus if the MCP23017 stops answering.
- [ ] **Push notifications:** the tower does not need to send them. A Cloud
      Function that listens to new `transactions` docs and sends FCM to the
      borrower is the intended design.
- [ ] **Multi-box borrow** (if the team wants it): allow more than one removal
      per session, each producing its own `borrow` transaction.
- [ ] Store the box → item table in Firestore (`equipment` already has it) and
      read it at boot instead of hard-coding, so admins can rename items from
      the web dashboard.

---

## 5. Test script for the demo

Run these with the app open on the dashboard and pull-to-refresh
(or reopen Home) after each step.

1. Tap a **registered** card → green LED. Remove the microphone from box 1.
   App: box 1 shows **Yours / Due in 2h**, "My borrowed items" lists it,
   Recent activity shows "Borrowed Wireless Microphone".
2. Tap the same card → return session. Put the microphone back.
   App: box 1 **Available**, activity shows "Returned Wireless Microphone".
3. With no tap, remove the HDMI cable from box 2 → red LED + buzzer.
   App: box 2 still **Available** for now (alert is admin-side); Firestore has
   an `alert` transaction.
4. Tap an **unregistered** card → red LED, nothing written.
5. (Phase 2) Set `dueAt` in the past on a borrowed box. App: red overdue banner,
   chip says **Overdue by …**, activity shows "… is overdue".

---

## 6. App-side follow-ups (Dana)

These are needed on the Android/Firebase side to support the tower; they are
not firmware tasks.

- [ ] Add `rfidCardUid: String = ""` to `model/User.kt` and a way to set it
      (admin web dashboard or Firebase console for now).
- [ ] Create the tower's device account in Firebase Authentication.
- [ ] Tighten `firestore.rules` so only the device account (and admins) can
      write `equipment` and `transactions`; borrowers should be read-only there.
- [ ] Remove the `seedSampleDataIfEmpty` call from `HomePresenter` once real
      data is flowing, or keep it but guarded by a debug flag.
- [ ] Add a Firestore snapshot listener (or pull-to-refresh) on the dashboard so
      status changes appear without reopening the screen (NFR-01).
