# ESP32 Course Activities – Context

Notes on the two ESP32 handouts issued for ELDROID, kept here because the
firmware work in [`ESP32_MVP_CHECKLIST.md`](ESP32_MVP_CHECKLIST.md) builds on
exactly the same two steps: get on Wi-Fi, then talk to Firebase.

Source PDFs live outside this repo, in
`Desktop/skool/1st_sem_26-27/ELDROID/Ass/`.

---

## The two handouts

| | ESP32 + Wi-Fi Connectivity | ESP32 + Wi-Fi + Firebase |
|---|---|---|
| File | `ESP32 and WiFi Connectivity Activity.pdf` | `6.0. ESP32 and Firebase Activity.pdf` |
| Pages | 7 | 6 |
| **Submission** | **Group** | **Individual** |
| Format | Graded worksheet (4 name lines, Parts I–XII, blank answer lines) | Plain step-by-step guide, no worksheet |
| Libraries | `<WiFi.h>` | `<WiFi.h>` + `<FirebaseESP32.h>` |
| Goal | Join a network and prove it | Write/read data in the cloud |

The Wi-Fi activity is the stated prerequisite for the Firebase one ("complete
previous guide").

---

## 1. Wi-Fi Connectivity Activity (group)

Board setup in Arduino IDE (Tools → Board → ESP32 Dev Module, then Tools → Port),
then a sketch that connects and reports.

Key calls taught:

| Call | Purpose |
|---|---|
| `WiFi.begin(ssid, password)` | Start the connection |
| `WiFi.status() != WL_CONNECTED` | Busy-wait until associated |
| `WiFi.SSID()` | Network actually joined |
| `WiFi.localIP()` | Address assigned on the LAN |
| `WiFi.RSSI()` | Signal strength in dBm |

RSSI reference table from the handout (closer to 0 is stronger):

| RSSI | Interpretation |
|---|---|
| -30 dBm | Excellent |
| -50 dBm | Very Good |
| -60 dBm | Good |
| -70 dBm | Fair |
| -80 dBm | Weak |
| -90 dBm | Very Weak |

**Graded work (group):**

- [ ] Test 1 – correct credentials; record network, IP, signal strength.
- [ ] Test 2 – wrong password; describe what the Serial Monitor does, then restore.
- [ ] Test 3 – kill the router mid-session, observe, bring it back.
- [ ] Part IX challenge – auto-reconnect in `loop()` (suggested solution given:
      on `!= WL_CONNECTED`, call `WiFi.disconnect()` then `WiFi.begin()` again).
- [ ] Part X – 8 guide questions in own words.

**Submissions (group):**

- [ ] Screenshot of the code.
- [ ] Screenshot of a successful compile/upload.
- [ ] Screenshot of the Serial Monitor showing *Wi-Fi Connected / SSID / IP /
      signal strength*.
- [ ] Written answers to the Part X guide questions.
- [ ] All four names filled in on the worksheet.
- [ ] Checked: no real Wi-Fi password visible in any screenshot (handout warns
      about this explicitly).

Upload tip from Part V: if it hangs on `Connecting...`, hold **BOOT** and release
once upload starts.

---

## 2. Firebase Activity (individual)

Assumes Wi-Fi already works. Adds the cloud layer.

**Setup (individual):**

- [ ] Install **Firebase ESP Client** (mobizt) and **ArduinoJson** (Benoit Blanchon).
- [ ] Create/select a Firebase project; enable Realtime Database or Firestore in
      test mode.
- [ ] Collect project ID, database URL (`https://<project-id>.firebaseio.com`),
      and credentials.

Auth, two options:

- **Legacy token** – Project Settings → Service Accounts → Database Secrets.
  Simpler; what the sample code uses via `config.signer.tokens.legacy_token`.
- **Service account** – recommended. Use `client_email`, `project_id`, and
  `private_key` from the downloaded JSON; keep the `\n` escapes in the key.

Test rules from the handout — **testing only**, replace before anything real:

```json
{ "rules": { ".read": true, ".write": true } }
```

Sketch shape: configure `FirebaseData` / `FirebaseAuth` / `FirebaseConfig`, call
`Firebase.begin(&config, &auth)`, then loop on `Firebase.setInt(fbdo,
"/test/data", 42)` and `Firebase.getInt(fbdo, "/test/data")`, printing
`fbdo.errorReason()` on failure.

Troubleshooting table from the handout:

| Symptom | Check |
|---|---|
| Authentication failed | Credentials; private-key `\n` formatting |
| Connection timeout | Wi-Fi strength; Firebase host URL |
| Permission denied | Database security rules |

**Graded work (individual):**

- [ ] Sketch compiles and uploads with the Firebase config in place.
- [ ] `Firebase.begin(&config, &auth)` authenticates (no auth error on Serial).
- [ ] `Firebase.setInt(fbdo, "/test/data", 42)` succeeds.
- [ ] `Firebase.getInt(fbdo, "/test/data")` reads the value back.
- [ ] `fbdo.errorReason()` printed on the failure path.
- [ ] `/test/data` visible in the Firebase Console.

**Submissions (individual):** not specified in the PDF.

- [ ] **Confirm the required format with the instructor** — this is the open
      question; everything below is an assumption until then.
- [ ] *(assumed)* Screenshot of the code.
- [ ] *(assumed)* Screenshot of a successful compile/upload.
- [ ] *(assumed)* Screenshot of the Serial Monitor showing write/read succeeding.
- [ ] *(assumed)* Screenshot of the Firebase Console showing `/test/data`.
- [ ] Checked: no credentials, database secret or private key visible in any
      screenshot.

---

## How this relates to SmartDock

The course activities use **Realtime Database** with a legacy token against
`/test/data`. The SmartDock tower instead targets **Firestore** collections
(`equipment`, `transactions`) authenticated as a device account, per
`ESP32_MVP_CHECKLIST.md` §0. The Wi-Fi half carries over directly — checklist
§1 ("ESP32 boots, joins 2.4 GHz Wi-Fi, prints its IP on Serial") is the Wi-Fi
activity's sketch. The Firebase half is the same idea at a different endpoint,
so treat the activity code as a warm-up, not as firmware to paste in.
