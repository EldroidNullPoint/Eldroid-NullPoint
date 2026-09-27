# SmartDock Comprehensive Failure, Edge-Case, Security, Recovery, and Application Behavior Specification

## Purpose

This document expands the SmartDock Functional Requirements
Specification (FRS) into a comprehensive operational and
failure-handling reference. It is intended to guide implementation,
testing, debugging, and AI-assisted development of the SmartDock system.

SmartDock is an IoT-enabled RFID-based equipment management system
composed of several independently developed components:

1.  **SmartDock IoT Tower / ESP32 firmware**
2.  **Firebase cloud/backend services**
3.  **Web Administrator Application**
4.  **Android Borrower/User Application**

The original FRS defines SmartDock as using RFID borrower
authentication, continuously monitored open storage boxes, ESP32,
item-presence sensors, Firebase, and mobile/web monitoring. The FRS also
states that each box is designed for one assigned item and that the
identity of an item is inferred from its assigned box rather than from a
tag attached to the physical equipment.

------------------------------------------------------------------------

# CRITICAL DEVELOPMENT SEPARATION FOR AI / DEVELOPERS

> **IMPORTANT: THE WEB ADMIN APPLICATION AND THE ANDROID BORROWER
> APPLICATION ARE TWO SEPARATE APPLICATIONS.**
>
> They are not the same project, are not developed in the same IDE, and
> may be implemented or maintained by different developers or different
> AI coding sessions. An AI working on one application must **not assume
> that it can directly edit, call, import, or control source code
> belonging to the other application**.
>
> The **Web Admin Application** must be treated as its own frontend
> application intended for authorized administrators/staff.
>
> The **Android Borrower/User Application** must be treated as a
> separate mobile application intended for registered borrowers.
>
> The two applications may share synchronized data through the approved
> Firebase/backend architecture, but they must remain logically and
> technically separated at the client level. Cross-application behavior
> must happen through backend state, authenticated APIs/Firebase
> services, database records, notifications, or shared event
> definitions - **never through direct source-code dependency between
> the Web Admin and Android applications**.
>
> When an AI is instructed to implement a feature in the **Web Admin**,
> it should modify only the Web Admin project and the backend interfaces
> required for that feature. When an AI is instructed to implement a
> feature in the **Android App**, it should modify only the Android
> project and the backend interfaces required for that feature. If a
> feature requires behavior from both applications, implementation
> requirements must be written separately for each application.
>
> The ESP32/IoT firmware is also a separate component. Neither client
> application should pretend to control a physical sensor state
> directly. Physical events must originate from the trusted IoT/backend
> workflow.

------------------------------------------------------------------------

# 1. System-Wide Fail-Safe Principle

SmartDock should follow this rule:

> **When SmartDock cannot confidently determine the validity of an
> equipment state, it must preserve the event history, avoid
> automatically changing borrower accountability, mark the affected
> state as uncertain, and request administrator reconciliation when
> necessary.**

Therefore:

-   Confident event -\> process normally.
-   Temporary connectivity failure -\> preserve locally and synchronize
    later.
-   Hardware failure -\> mark the affected hardware/box unavailable or
    uncertain.
-   Ambiguous physical event -\> do not guess.
-   Security event -\> trigger local alarm immediately and synchronize
    when possible.
-   Data conflict -\> preserve evidence and reconcile.
-   Unknown physical item -\> never claim verified item identity.
-   Failed notification -\> do not reverse a valid transaction.
-   Failed UI -\> reload canonical state from backend when possible.

------------------------------------------------------------------------

# 2. Recommended State Models

## 2.1 Tower State

-   `STARTING`
-   `ONLINE`
-   `OFFLINE`
-   `DEGRADED`
-   `MAINTENANCE`
-   `ERROR`

## 2.2 Box / Equipment State

-   `AVAILABLE`
-   `BORROWED`
-   `OVERDUE`
-   `RETURN_PENDING`
-   `STATUS_UNKNOWN`
-   `SENSOR_FAULT`
-   `MAINTENANCE`
-   `UNCONFIGURED`
-   `LOST`
-   `DAMAGED`
-   `RETIRED`
-   `RETURNED_PENDING_INSPECTION`

## 2.3 Session State

-   `IDLE`
-   `AUTHENTICATING`
-   `BORROW_SESSION`
-   `RETURN_SESSION`
-   `COMPLETED`
-   `EXPIRED`
-   `CANCELLED`
-   `AMBIGUOUS`

## 2.4 Cloud/Event Synchronization State

-   `PENDING`
-   `SYNCING`
-   `SYNCED`
-   `FAILED`
-   `CONFLICT`

## 2.5 Hardware Health State

Each major component should have independent health where practical:

-   Wi-Fi: `ONLINE / OFFLINE`
-   Cloud: `ONLINE / OFFLINE / DEGRADED`
-   RFID Reader: `ONLINE / FAULT`
-   I2C Bus: `ONLINE / FAULT`
-   Individual Sensor: `ONLINE / UNSTABLE / FAULT`
-   ESP32: `ONLINE / RESTARTED / UNSTABLE`
-   Local Queue: `NORMAL / NEAR_CAPACITY / FULL`

Do not reduce all failures to one generic `ERROR`.

------------------------------------------------------------------------

# 3. System-Wide Transaction and Data Rules

## 3.1 Unique Event IDs

Every physical event should have a unique identifier so retries do not
create duplicate borrow/return/security transactions.

Recommended event data:

-   event ID
-   tower ID
-   boot/session identifier
-   sequence number
-   borrower ID/UID reference where applicable
-   equipment ID
-   box ID
-   event type
-   local event timestamp
-   server received timestamp
-   synchronization state
-   previous state
-   resulting state
-   error/reconciliation metadata where applicable

## 3.2 Idempotency

If Firebase/backend receives the same event twice, the second submission
must not create a second transaction.

## 3.3 Event Ordering

Backend processing should not rely only on network arrival order.
Delayed events may arrive after newer events.

Use:

-   event sequence
-   transaction/session ID
-   timestamps
-   expected previous state
-   current canonical state

## 3.4 Auditability

Do not silently rewrite important transaction/security history.

Administrative corrections should record:

-   admin ID
-   original value
-   new value
-   reason
-   timestamp

## 3.5 Authoritative Responsibilities

Recommended responsibility separation:

-   **ESP32 / IoT:** physical sensor observations, RFID tower
    interaction, local session timing, local alarm behavior.
-   **Backend/Firebase:** canonical transactions, synchronized status,
    authorization rules, history, due-time processing, reconciliation
    data.
-   **Web Admin:** monitoring, management, investigation,
    correction/override workflows.
-   **Android App:** borrower-facing personal information, availability,
    transaction confirmation, due information, notifications.

------------------------------------------------------------------------

# 4. Connectivity and Cloud Failure Scenarios

## 4.1 Tower Wi-Fi Connection Is Lost

### IoT / Backend Behavior

-   Continue local sensor monitoring.
-   Enter offline/degraded mode.
-   Continue unauthorized-removal detection locally.
-   Store unsynchronized events in persistent local storage.
-   Preserve event ordering.
-   Attempt automatic reconnection.
-   Synchronize queued events after reconnection.
-   Never silently discard events.
-   Prevent duplicate cloud records through unique event IDs.

### Web Admin Behavior

-   Display tower as `OFFLINE`.
-   Display last successful communication/synchronization time.
-   Previously known equipment states must be labeled as last
    known/stale where appropriate.
-   Do not imply real-time monitoring is active.
-   Highlight that security events may be pending synchronization.

### Android App Behavior

-   If Firebase is reachable but tower is offline, show that
    availability may be based on the tower's last known state.
-   Do not guarantee that an item shown as available is physically
    confirmed in real time.
-   Show appropriate last-updated/offline tower messaging.

------------------------------------------------------------------------

## 4.2 Internet Disconnects During Borrowing

Example: RFID tap succeeds -\> item removed -\> network fails before
Firebase receives the transaction.

### IoT / Backend

-   The local physical transaction must not disappear.
-   Mark event locally as `BORROWED_PENDING_SYNC`.
-   Persist event before considering the operation complete.
-   Retry after connectivity returns.
-   Backend acknowledges event before local queue removes it.
-   Do not reverse physical state simply because Firebase was
    temporarily unavailable.

### Web Admin

-   While tower is offline, show last-known status.
-   After reconnection, display synchronized transaction.
-   If synchronization creates a conflict with cloud state, show a
    reconciliation alert instead of silently overwriting.

### Android App

-   If confirmation cannot be obtained from backend, clearly show
    pending synchronization rather than false cloud-confirmed success.
-   Once synchronized, display normal borrow confirmation with item,
    time, due time, and box information.

------------------------------------------------------------------------

## 4.3 Internet Disconnects During Return

### IoT / Backend

-   Detect physical return locally.
-   Store `RETURN_PENDING_SYNC`.
-   Preserve the active transaction link.
-   Sync when connection returns.
-   Do not lose return evidence.

### Web Admin

-   Display reconciliation/pending state after data becomes available.
-   If backend previously showed overdue but delayed return proves an
    earlier physical return, preserve timestamps and correct state
    through a traceable reconciliation process.

### Android App

-   Show that the physical return was detected locally but cloud
    confirmation may be pending if this status can be safely
    communicated.
-   Do not show a final synchronized state until backend confirms it.

------------------------------------------------------------------------

## 4.4 Wi-Fi Works but Firebase/Cloud Is Unavailable

Possible causes include:

-   Firebase outage
-   DNS failure
-   TLS failure
-   API timeout
-   authentication failure
-   quota exceeded
-   backend configuration error

### IoT

Distinguish:

-   Wi-Fi connected
-   cloud connected

Queue events locally and retry using controlled backoff.

### Web Admin

If the web application cannot reach backend services:

-   show cloud/backend connectivity error;
-   stop claiming real-time state;
-   show last known data only when clearly labeled;
-   provide retry/reconnect behavior.

### Android App

-   show cloud connection failure;
-   retain safe cached display data only as stale information;
-   do not permit client-side assumptions to change inventory state.

------------------------------------------------------------------------

## 4.5 Android Phone Loses Internet

The physical tower must continue functioning independently of the
borrower's phone.

### Android App

-   Enter offline UI state.
-   Display cached information as `Last updated ...`.
-   Clearly state availability may have changed.
-   Disable actions requiring live backend validation.
-   Automatically refresh after reconnection.

### Web Admin

No special behavior is required merely because one borrower phone is
offline.

------------------------------------------------------------------------

## 4.6 Web Admin Browser Loses Internet

### Web Admin

-   Display `Connection lost`.
-   Show last update time.
-   Do not silently continue showing old records as live.
-   Retry connection safely.
-   Reload/reconcile canonical state when connection returns.

### Android App

No behavior should depend on an administrator's browser remaining
connected.

------------------------------------------------------------------------

## 4.7 Firestore Quota or Cloud Write Failure

### IoT / Backend

-   Do not discard events.
-   Queue locally when possible.
-   Mark cloud write failure.
-   Retry according to safe backoff rules.
-   Protect against queue overflow.

### Web Admin

-   Show backend/cloud write problem when detectable.
-   Surface affected tower/event synchronization state.
-   Provide admin visibility into pending/failed events.

### Android App

-   Do not claim a transaction is fully synchronized when backend
    confirmation failed.

------------------------------------------------------------------------

## 4.8 Local Offline Queue Near Capacity or Full

### IoT

-   Track local queue usage.
-   Alert before storage is exhausted.
-   Never silently overwrite unsynchronized transaction/security events.
-   If integrity can no longer be guaranteed, disable new borrowing and
    enter safe/degraded mode while retaining security monitoring where
    possible.

### Web Admin

After communication returns, show a device storage/queue warning and
require investigation if events were at risk.

### Android App

If borrowing is disabled due to tower integrity/storage condition, show
tower temporarily unavailable.

------------------------------------------------------------------------

# 5. Sensor and Physical Detection Failures

## 5.1 Sensor Suddenly Malfunctions

Possible conditions:

-   disconnected sensor
-   blocked beam
-   dirty sensor
-   misalignment
-   broken wire
-   stuck HIGH
-   stuck LOW
-   unstable/intermittent output

### IoT

Do not automatically interpret every abnormal state as theft or return.

Set affected box to:

-   `SENSOR_FAULT`
-   `STATUS_UNKNOWN`

where appropriate.

Attempt sensor/I2C recovery when possible.

### Web Admin

Show:

-   affected box;
-   sensor health;
-   time fault started;
-   current last-known physical state;
-   maintenance/reconciliation requirement.

### Android App

Do not show the affected item as confidently available.

Display something such as:

-   `Temporarily unavailable`
-   `Status unavailable`

Avoid exposing technical hardware details unnecessarily to borrowers.

------------------------------------------------------------------------

## 5.2 Sensor Rapidly Flickers

Possible causes:

-   vibration
-   hand movement
-   electrical noise
-   poor alignment
-   partially blocked beam

### IoT

Use debounce/stabilization.

A state should remain stable for a defined period before becoming a
valid physical event.

Do not create multiple transactions from one physical action.

### Web Admin

If repeated instability exceeds a threshold, show `SENSOR_UNSTABLE`.

### Android App

Treat affected equipment as temporarily uncertain/unavailable if
reliability is compromised.

------------------------------------------------------------------------

## 5.3 Sensor Is Permanently Blocked

A person may deliberately cover the sensor or an object may accidentally
block it.

### IoT

-   Detect suspicious long-duration or impossible patterns where
    feasible.
-   Do not assume sensor presence proves equipment identity.

### Web Admin

-   Surface suspicious sensor condition.
-   Require physical inspection.

### Android App

Do not claim verified item identity.

------------------------------------------------------------------------

## 5.4 Environmental Light Interferes With IR Sensor

Testing should include:

-   fluorescent lighting
-   sunlight
-   darkness
-   shadows
-   reflective objects

If readings become unstable:

-   classify sensor as unstable;
-   avoid creating false security transactions;
-   require maintenance/calibration.

------------------------------------------------------------------------

## 5.5 Loose Wiring Produces Intermittent Events

Use:

-   physical strain relief;
-   secured wiring;
-   debounce;
-   fault thresholds.

Repeated abnormal transitions should produce a maintenance warning
instead of many false security alarms.

------------------------------------------------------------------------

## 5.6 Equipment Shape Does Not Reliably Trigger Sensor

Thin cables, remotes, or incorrectly positioned items may fail to break
the beam.

Mitigation:

-   placement guides;
-   marked item position;
-   box-specific sensor alignment;
-   calibration;
-   repeated physical testing.

The software must not pretend this hardware limitation does not exist.

------------------------------------------------------------------------

# 6. Wrong Item / Wrong Box / Object Identity Scenarios

## 6.1 Wrong Item Placed in Correct Box

Example: Box 1 belongs to a microphone, but an HDMI cable is placed
inside.

The current IR sensor can detect an object/presence condition but cannot
prove that the object is the microphone.

### IoT

-   Record physical presence only.
-   Do not claim physical identity verification.

### Web Admin

-   Where relevant, label identity as inferred from box assignment.
-   Support manual inventory reconciliation.

### Android App

-   Do not expose misleading wording such as "verified microphone
    detected."
-   Normal borrower-facing status may remain based on system state, but
    implementation documentation must acknowledge identity is inferred.

### Limitation

The current architecture cannot reliably solve this without additional
item-level identification such as RFID/NFC tags or another recognition
mechanism.

------------------------------------------------------------------------

## 6.2 Item Returned to Wrong Box

Example: microphone assigned to Box 1 is placed in Box 3.

### IoT

During a valid return session:

-   compare expected return box against the box that changed to present;
-   if mismatched, do not close the expected equipment transaction;
-   flag unexpected placement;
-   provide red/error feedback.

Because the object itself is not tagged, SmartDock can detect an
unexpected placement but cannot prove which object was inserted.

### Web Admin

Display:

-   borrower/session involved;
-   expected box;
-   unexpected box;
-   time;
-   `RETURN_MISMATCH` / reconciliation required.

### Android App

Tell borrower:

-   incorrect box;
-   expected box location;
-   return is not complete.

------------------------------------------------------------------------

## 6.3 Random Object Placed in Empty Box

A book, hand, bag, or unrelated item may satisfy the sensor.

### IoT

Treat as presence event, not verified equipment identity.

If no valid return session exists, flag as unverified placement.

### Web Admin

Show `UNVERIFIED_PLACEMENT` and require inspection when relevant.

### Android App

Do not automatically tell a borrower that their equipment was
successfully returned unless the return workflow and expected box
conditions are valid.

------------------------------------------------------------------------

## 6.4 Two Items Are Physically Swapped

If both sensors remain `PRESENT`, the current hardware may not detect
that the actual equipment was swapped.

This is a known architectural limitation.

### Web Admin

Support periodic physical inventory checks.

### Android App

No reliable detection is possible from the current hardware alone.

### Future Improvement

Tag each equipment item individually using RFID/NFC or another identity
mechanism.

------------------------------------------------------------------------

# 7. Borrowing Session Edge Cases

## 7.1 Borrower Removes Wrong Item During Valid Session

The current FRS uses first-removal item selection.

Therefore the first valid box removal during the session becomes the
borrowed item.

If future versions add pre-selection, then a different box removal
should become an error.

### Web Admin

Record the item actually bound by the first valid removal.

### Android App

Show the actual borrowed item after successful transaction.

------------------------------------------------------------------------

## 7.2 Borrower Removes Two Items During One Session

Recommended behavior:

1.  First valid removal becomes the authorized borrow.
2.  Successful first removal immediately closes the session.
3.  Second removal is not authorized by the original session.
4.  Trigger unauthorized-removal handling.

### Web Admin

Show both the legitimate transaction and separate security event.

### Android App

Show only the legitimate borrowed item. If appropriate, inform the
borrower that the session is complete.

------------------------------------------------------------------------

## 7.3 Two Boxes Change at Nearly the Same Time

Possible causes:

-   two items removed together;
-   multiple people;
-   sensor noise.

### IoT

If ordering cannot be determined reliably:

-   set session to `AMBIGUOUS`;
-   do not arbitrarily assign the borrower;
-   preserve both events.

### Web Admin

Show `TRANSACTION_AMBIGUOUS` with all affected boxes and timestamps.

### Android App

Tell borrower the transaction could not be confirmed and requires staff
assistance.

------------------------------------------------------------------------

## 7.4 Two People Use Tower at Same Time

Recommended rule:

> Only one active borrower session may exist per SmartDock tower.

### IoT

While session is active:

-   reject another RFID session;
-   indicate tower busy.

### Web Admin

Show current active session status if needed.

### Android App

If applicable, show `Tower currently in use. Please try again.`

------------------------------------------------------------------------

## 7.5 Someone Else Removes Item During Cardholder's Session

RFID proves whose card was used, not who physically moved the item.

Mitigation:

-   one active session;
-   very short timeout;
-   close after first valid action;
-   physical supervision for prototype environments;
-   precise audit timestamps.

This remains a shared-space authentication limitation.

------------------------------------------------------------------------

## 7.6 Session Expires Before Removal

### IoT

-   close session;
-   create no borrowing transaction;
-   require another RFID tap.

### Web Admin

Optionally record expired session for diagnostics.

### Android App

Do not show a borrow transaction.

------------------------------------------------------------------------

## 7.7 Removal Occurs Exactly at Session Expiration

The ESP32 should be the authority for local session timing.

Use one local timing source to determine whether the event happened
inside or outside the valid session.

Do not allow Web, Android, and ESP32 to independently decide
authorization.

------------------------------------------------------------------------

## 7.8 Borrower Taps but Changes Mind

If no equipment is removed:

-   allow session to expire;
-   create no borrow transaction.

------------------------------------------------------------------------

## 7.9 Borrower Removes and Immediately Returns During Same Borrow Session

Recommended:

-   successful removal closes borrow session;
-   return requires a new RFID tap and separate return session.

This keeps state transitions predictable.

------------------------------------------------------------------------

# 8. Return Edge Cases

## 8.1 Return Without RFID Tap

The current FRS requires an RFID return session.

### IoT

If an object appears in an empty box without a valid return session:

-   do not automatically attribute it to a borrower;
-   flag `UNVERIFIED_PLACEMENT`.

### Web Admin

Show physical presence but transaction remains unresolved.

### Android App

Do not mark borrower's transaction returned merely because an object
appeared without valid authentication.

------------------------------------------------------------------------

## 8.2 RFID Is Tapped After Item Was Already Returned

Do not retroactively authorize an earlier placement unless a deliberate
reconciliation window is designed.

Recommended current behavior:

-   earlier placement remains unverified;
-   later RFID tap opens a new session;
-   staff reconciliation may be needed.

------------------------------------------------------------------------

## 8.3 Borrower Attempts to Return Item They Did Not Borrow

Backend should verify that the authenticated borrower has an active
transaction for the expected equipment.

If not:

-   `RETURN_MISMATCH`;
-   do not close another borrower's transaction.

### Web Admin

Show mismatch and allow authorized reconciliation.

### Android App

Tell borrower that the return cannot be matched and to contact staff.

------------------------------------------------------------------------

## 8.4 Admin Manually Processes a Return

Allow controlled override.

Require:

-   admin identity;
-   borrower/equipment;
-   reason;
-   timestamp.

Never silently rewrite the original history.

------------------------------------------------------------------------

## 8.5 Item Was Marked Lost but Later Returned

Allow:

`LOST -> RECOVERED/RETURNED`

Preserve history showing that it was previously marked lost.

------------------------------------------------------------------------

## 8.6 Returned Equipment Is Damaged

Presence does not mean usable.

Optional enhanced workflow:

`BORROWED -> RETURNED_PENDING_INSPECTION -> AVAILABLE/DAMAGED/MAINTENANCE`

### Web Admin

Staff can mark condition.

### Android App

Do not show item available until inspection is complete if this workflow
is enabled.

------------------------------------------------------------------------

# 9. Unauthorized Removal and Security Events

## 9.1 Item Removed Without RFID Session

### IoT

-   trigger buzzer;
-   red indicator;
-   create unauthorized event;
-   preserve event locally if offline.

### Web Admin

-   real-time security alert when connected;
-   show affected box/equipment;
-   timestamp;
-   tower;
-   status of event.

### Android App

No system-wide security details should be exposed to ordinary borrowers
unless directly relevant to their transaction.

------------------------------------------------------------------------

## 9.2 Unauthorized Removal While Internet Is Down

### IoT

Local alarm must still function.

Store `UNSYNCED_SECURITY_EVENT`.

### Web Admin

Receive event after reconnection with original local event timestamp.

### Android App

No dependency.

------------------------------------------------------------------------

## 9.3 Item Removed Then Immediately Replaced Without Authorization

The return does not erase the security event.

Keep immutable history of unauthorized removal.

------------------------------------------------------------------------

## 9.4 Item Removed Before RFID Tap, Then Card Is Tapped

A later RFID tap must not retroactively authorize an earlier removal.

Event ordering must be preserved.

------------------------------------------------------------------------

# 10. RFID Authentication Failures and Abuse

## 10.1 Unregistered RFID Card

### IoT

-   reject UID;
-   no session;
-   red/error feedback;
-   optional unknown-card attempt log.

### Web Admin

May show unknown RFID attempts for security monitoring.

### Android App

No transaction should appear.

------------------------------------------------------------------------

## 10.2 Disabled/Suspended RFID Account

Do not open a new borrowing session.

Recommended policy:

-   borrowing may be blocked;
-   returning existing equipment should remain possible.

### Web Admin

Show reason/account state to authorized staff.

### Android App

Show general user-friendly message such as
`Borrowing unavailable. Contact administrator.` Do not expose sensitive
internal details.

------------------------------------------------------------------------

## 10.3 Lost RFID Card

### Web Admin

Allow administrator to:

-   disable old UID;
-   assign replacement UID;
-   retain borrower history.

### Android App

Borrower account/history remains intact.

------------------------------------------------------------------------

## 10.4 Stolen RFID Card

Current UID-based design may treat possession of the card as
authentication.

Mitigations:

-   rapid deactivation;
-   borrower transaction notifications;
-   audit logs;
-   optional future PIN/mobile confirmation.

Document this limitation.

------------------------------------------------------------------------

## 10.5 Duplicate RFID Tap

Use cooldown/debounce.

Same UID repeated within a short interval should not create duplicate
sessions.

------------------------------------------------------------------------

## 10.6 Borrower Repeatedly Taps Card

If a session is already active:

-   do not create another session;
-   provide `Session already active` feedback.

------------------------------------------------------------------------

## 10.7 RFID UID Cloning

UID-based RFID identification should not be described as high-security
cryptographic authentication.

Future stronger authentication may be required for higher-risk
deployments.

------------------------------------------------------------------------

# 11. Power, ESP32, and Hardware Controller Failures

## 11.1 Power Failure

On restart:

1.  initialize ESP32;
2.  test RFID reader;
3.  test I2C;
4.  test sensors;
5.  inspect current physical box states;
6.  reconnect Wi-Fi;
7.  reconnect cloud;
8.  restore persisted pending events;
9.  retrieve/reconcile backend state;
10. flag physical/cloud inconsistencies.

Do not invent a borrower if equipment disappeared while power was off.

------------------------------------------------------------------------

## 11.2 Power Failure During Transaction

Persist critical transaction/event state using ESP32 nonvolatile storage
where feasible.

After reboot:

-   recover pending state;
-   compare physical box state;
-   synchronize/reconcile.

------------------------------------------------------------------------

## 11.3 Equipment Removed During Complete Power Outage

The system cannot observe events while unpowered.

After restart, if previously present equipment is absent:

-   flag `POST_OUTAGE_INVENTORY_MISMATCH`;
-   do not assign a borrower automatically.

### Web Admin

Require physical investigation/reconciliation.

### Android App

Affected availability should not be presented as confidently known.

------------------------------------------------------------------------

## 11.4 ESP32 Freezes

Use watchdog timer.

After watchdog restart:

-   health check;
-   reconnect;
-   restore pending state;
-   reconcile.

### Web Admin

Show unexpected restart where detectable.

------------------------------------------------------------------------

## 11.5 ESP32 Repeatedly Restarts

Example: multiple restarts in a short period.

Set:

`DEVICE_UNSTABLE`

Possible causes:

-   unstable power;
-   firmware crash;
-   short circuit;
-   memory issue.

Disable normal borrowing if system integrity cannot be guaranteed.

------------------------------------------------------------------------

## 11.6 MCP23017 / I2C Failure

Attempt automatic reconnection as required by the FRS.

If recovery fails:

-   mark sensor subsystem offline;
-   affected boxes become `STATUS_UNKNOWN` or `SENSOR_FAULT`;
-   disable borrowing from affected boxes.

### Web Admin

Show I2C/sensor subsystem health.

### Android App

Affected items become temporarily unavailable.

------------------------------------------------------------------------

## 11.7 RFID Reader Failure

Set `RFID_READER_FAULT`.

Authenticated borrow/return must be disabled.

Sensor security monitoring may continue.

### Web Admin

Show tower hardware fault.

### Android App

Show tower temporarily unavailable for borrowing/returning.

------------------------------------------------------------------------

## 11.8 Buzzer Failure

Unless feedback circuitry exists, software can know that a buzzer
command was issued but cannot prove that sound was produced.

Document buzzer as a non-self-verifying actuator.

Do not rely solely on buzzer for security notification.

------------------------------------------------------------------------

## 11.9 LED Failure

Same principle as buzzer.

Cloud/admin alerts should remain available.

------------------------------------------------------------------------

## 11.10 Power Supply Brownout

Use adequate regulated power.

ESP32 may reboot under voltage drop.

Treat repeated brownout/restart as device instability.

------------------------------------------------------------------------

# 12. Database and State Conflict Scenarios

## 12.1 Database Says AVAILABLE but Sensor Says ABSENT

Set:

`STATUS_CONFLICT`

Do not silently create a borrower.

### Web Admin

Show:

-   backend state;
-   physical sensor state;
-   last event;
-   reconciliation action.

### Android App

Do not show confidently available.

------------------------------------------------------------------------

## 12.2 Database Says BORROWED but Sensor Says PRESENT

Possible causes:

-   return without RFID;
-   wrong object inserted;
-   missed event;
-   sensor fault;
-   stale database.

Set:

`RETURN_RECONCILIATION_REQUIRED`

Do not automatically clear borrower accountability.

------------------------------------------------------------------------

## 12.3 Duplicate Firebase Transaction

Backend must recognize duplicate event ID and ignore duplicate mutation.

------------------------------------------------------------------------

## 12.4 Events Arrive Out of Order

Use event ordering metadata.

A delayed old event must not overwrite newer valid state.

------------------------------------------------------------------------

## 12.5 ESP32 and Backend Disagree After Reconnection

Use:

-   event IDs;
-   sequence;
-   timestamps;
-   physical sensor state;
-   active transaction;
-   canonical history.

If unresolved:

`MANUAL_RECONCILIATION_REQUIRED`

------------------------------------------------------------------------

## 12.6 Old Event Arrives Hours Later

Validate chronology before applying state.

Store event for audit even if it cannot safely mutate current equipment
status.

------------------------------------------------------------------------

## 12.7 Impossible State Transition

Reject invalid transitions such as:

-   returning an already available item;
-   borrowing an already borrowed item;
-   creating duplicate active loan;
-   closing nonexistent transaction.

Use an explicit state machine.

------------------------------------------------------------------------

# 13. Time and Due-Date Failures

## 13.1 ESP32 Clock Incorrect

Synchronize trusted time after internet connection.

If time cannot be trusted:

`CLOCK_UNVERIFIED`

Use sequence information and backend timestamps for reconciliation.

------------------------------------------------------------------------

## 13.2 User Changes Android Phone Clock

Phone time must not be authoritative for due status.

Use backend/server timestamps.

------------------------------------------------------------------------

## 13.3 ESP32 Clock Resets After Power Loss

Resynchronize time after boot.

Persist sequence/event identifiers so offline events can still be
ordered.

------------------------------------------------------------------------

## 13.4 Wrong Due-Time Configuration

Keep due policy centralized in backend configuration.

Do not separately hard-code conflicting rules in:

-   ESP32;
-   Android;
-   Web Admin.

Store actual due timestamp on each transaction.

------------------------------------------------------------------------

## 13.5 Admin Changes Due Policy While Loan Is Active

Recommended:

-   existing transaction retains stored due timestamp;
-   new policy applies to future loans unless administrator deliberately
    changes a specific active transaction.

Audit any manual due-date modification.

------------------------------------------------------------------------

# 14. Notification Failures

## 14.1 Due Notification Fails

A failed notification does not change the actual due time or transaction
state.

Retry where appropriate.

------------------------------------------------------------------------

## 14.2 Duplicate Notification

Notification processing should be idempotent.

Track whether:

-   due reminder sent;
-   overdue notification sent;
-   escalation sent.

------------------------------------------------------------------------

## 14.3 Notification Arrives Late

Backend transaction state remains authoritative.

The application should display current due/overdue state when opened.

------------------------------------------------------------------------

## 14.4 Notification Sent to Wrong Borrower

Before sending, verify recipient is linked to the transaction borrower.

Treat cross-user notification/data exposure as a security defect.

------------------------------------------------------------------------

# 15. Account and Data Security

## 15.1 Borrower Attempts to Read Another Borrower's Information

Enforce Firebase/backend authorization server-side.

Never rely only on hidden Android UI.

Borrowers should access only permitted personal records and
public/authorized equipment availability.

------------------------------------------------------------------------

## 15.2 Modified Android App Sends Forged Requests

Never trust client-supplied inventory state.

Critical physical status changes should originate from trusted
device/backend workflows.

Security Rules/backend validation must reject unauthorized writes.

------------------------------------------------------------------------

## 15.3 Web Admin Account Is Compromised

Use:

-   least privilege;
-   individual admin accounts;
-   strong authentication;
-   MFA if feasible;
-   audit logs;
-   session expiration;
-   reauthentication for sensitive actions;
-   account disable capability.

------------------------------------------------------------------------

## 15.4 Admin Leaves Browser Logged In

Use inactivity/session expiration where appropriate.

Sensitive operations may require reauthentication.

------------------------------------------------------------------------

## 15.5 ESP32 Credentials Are Exposed

Do not embed unrestricted administrative credentials.

Device permissions should be limited to only required
collections/actions.

Credential compromise must not grant full admin access.

------------------------------------------------------------------------

## 15.6 Firebase Security Rules Misconfiguration

Explicitly test that:

-   unauthenticated users cannot read sensitive data;
-   borrowers cannot read other borrower records;
-   borrowers cannot directly change equipment state;
-   borrowers cannot forge transaction history;
-   admin-only collections are protected;
-   IoT/device writes are appropriately restricted.

------------------------------------------------------------------------

## 15.7 Malformed Payload

Reject:

-   invalid box ID;
-   invalid borrower ID;
-   invalid equipment ID;
-   invalid timestamp;
-   missing required fields;
-   duplicate event ID;
-   impossible state;
-   excessively old/replayed event where inappropriate.

Do not partially mutate inventory state.

------------------------------------------------------------------------

## 15.8 Logs Are Altered

Prefer append-oriented transaction/security history.

Corrections should create traceable correction records rather than
silently destroying original evidence.

------------------------------------------------------------------------

## 15.9 Personal Information Exposure

Web Admin may view system-wide borrower information only when
authorized.

Android borrowers must not be able to enumerate:

-   other borrowers;
-   other users' borrowing history;
-   other users' overdue status;
-   internal identifiers not required by the UI.

------------------------------------------------------------------------

# 16. Administrative Data Management Edge Cases

## 16.1 Admin Deletes Borrower With Active Loan

Prefer account deactivation over hard deletion.

Do not allow destructive deletion that breaks active
transactions/history.

------------------------------------------------------------------------

## 16.2 Admin Deletes Equipment With Active Loan

Reject destructive deletion.

Use lifecycle states such as:

-   active;
-   inactive;
-   lost;
-   retired.

Preserve transaction history.

------------------------------------------------------------------------

## 16.3 Two Items Assigned to One Box

Reject configuration.

One active assigned equipment item per box.

------------------------------------------------------------------------

## 16.4 One Item Assigned to Two Boxes

Reject configuration.

One equipment ID should have only one active box assignment.

------------------------------------------------------------------------

## 16.5 Box Has No Assigned Equipment

Set `UNCONFIGURED`.

Sensor changes must not create normal borrow records.

------------------------------------------------------------------------

## 16.6 Equipment Under Maintenance

Set `MAINTENANCE`.

Presence in box does not automatically mean borrowable.

### Web Admin

Allow authorized staff to change maintenance status.

### Android App

Show unavailable.

------------------------------------------------------------------------

## 16.7 Equipment Is Lost

Authorized staff may transition:

`BORROWED/OVERDUE -> LOST`

Do not make empty box available simply because item was marked lost.

------------------------------------------------------------------------

## 16.8 Multiple Administrators Modify Same Record

Use backend transactions/version validation where appropriate.

Audit:

-   who;
-   what;
-   old value;
-   new value;
-   when.

------------------------------------------------------------------------

# 17. Application Failure Scenarios

## 17.1 Android App Crashes

On restart:

-   authenticate safely;
-   reload canonical backend state;
-   do not depend solely on in-memory transaction state.

------------------------------------------------------------------------

## 17.2 Web Admin Crashes or Browser Reloads

Reload canonical backend state.

Do not depend on browser memory for critical transaction history.

------------------------------------------------------------------------

## 17.3 User Repeatedly Presses UI Button

Client should prevent duplicate submissions while request is pending.

Backend must still enforce idempotency.

------------------------------------------------------------------------

## 17.4 Android Shows Stale Availability

Show:

-   last update time;
-   offline status;
-   tower connectivity where useful.

Do not imply stale data is guaranteed current.

------------------------------------------------------------------------

## 17.5 Admin Dashboard Opens While Tower Offline

Separate device health from equipment state.

Example:

-   Equipment: `Last known available`
-   Tower: `Offline since 2:43 PM`

------------------------------------------------------------------------

## 17.6 Borrower Sees Available Item but Someone Takes It First

Availability is not reservation.

Unless reservations are implemented, Android must not imply that viewing
an available item reserves it.

------------------------------------------------------------------------

## 17.7 Borrower Has No Mobile Phone

RFID tower borrowing should still function according to the FRS
architecture.

Android monitoring is not required to physically authenticate at the
tower.

------------------------------------------------------------------------

## 17.8 Borrower Uninstalls App

Backend account/history remains.

Uninstalling must not delete transaction records.

------------------------------------------------------------------------

## 17.9 Borrower Logs In on Multiple Phones

If allowed:

-   all devices display the same backend state;
-   permissions remain server-enforced;
-   no phone becomes inventory authority.

------------------------------------------------------------------------

# 18. WEB ADMIN APPLICATION - SEPARATE IMPLEMENTATION REQUIREMENTS

> **AI IMPLEMENTATION BOUNDARY:** Everything in this section belongs to
> the Web Administrator application unless a backend dependency is
> explicitly mentioned. Do not implement Android UI, Android navigation,
> Android local storage, or Android-specific behavior in the Web Admin
> project.

The Web Admin exists for authorized administrators/staff to monitor
SmartDock, investigate anomalies, manage records/configuration, and
perform controlled reconciliation.

## 18.1 Web Admin Must Display

-   all equipment status;
-   box status;
-   borrower information allowed by role;
-   borrowing history;
-   security alerts;
-   due/overdue state;
-   tower connectivity;
-   cloud synchronization health;
-   sensor faults;
-   RFID reader fault;
-   I2C subsystem fault;
-   pending synchronization;
-   state conflicts;
-   ambiguous transactions;
-   wrong-box return events;
-   unverified placement;
-   post-outage inventory mismatch;
-   device restart/instability warnings;
-   last communication timestamp.

## 18.2 Web Admin Must Distinguish Status Types

Never combine all conditions into `Error`.

Distinguish:

-   physical item state;
-   transaction state;
-   box/sensor health;
-   tower connectivity;
-   cloud connectivity;
-   synchronization state;
-   security state.

## 18.3 Web Admin Reconciliation

Authorized admins should be able to investigate unresolved states.

Examples:

-   physical/database mismatch;
-   return without RFID;
-   wrong-box placement;
-   ambiguous simultaneous sensor event;
-   post-power-outage mismatch;
-   delayed offline transaction;
-   borrower dispute;
-   lost/recovered equipment.

Any manual correction must require a reason and be audited.

## 18.4 Web Admin Overrides

Potential controlled actions:

-   disable borrower/card;
-   replace RFID association;
-   mark equipment maintenance;
-   mark equipment damaged;
-   mark lost;
-   mark recovered;
-   process authorized manual return;
-   correct configuration;
-   resolve conflict.

Do not silently delete original transaction/security evidence.

## 18.5 Web Admin Connectivity UI

When backend connection is lost:

-   show connection banner;
-   show last update time;
-   label stale records;
-   retry safely.

When one tower is offline:

-   do not imply all cloud services are offline;
-   show tower-specific status.

## 18.6 Web Admin Security

-   administrator authentication;
-   role authorization;
-   server-side enforcement;
-   optional MFA;
-   session expiration;
-   sensitive-operation reauthentication;
-   no shared privileged credentials;
-   admin action audit trail.

## 18.7 Web Admin Data Integrity

Admin UI must not directly bypass backend validation.

Backend must reject:

-   impossible transitions;
-   duplicate assignments;
-   deletion of active critical records;
-   unauthorized changes.

------------------------------------------------------------------------

# 19. ANDROID BORROWER/USER APPLICATION - SEPARATE IMPLEMENTATION REQUIREMENTS

> **AI IMPLEMENTATION BOUNDARY:** Everything in this section belongs to
> the Android Borrower/User application unless a backend dependency is
> explicitly mentioned. Do not implement Web Admin pages, admin
> management screens, administrator reconciliation tools, or
> web-specific code inside the Android project.

The Android app is borrower-facing. It should provide personal
monitoring and clear guidance without exposing system-wide
administrative information.

## 19.1 Android App Should Display

-   equipment availability;
-   borrower's own current borrowed equipment;
-   borrow time;
-   due time;
-   overdue state;
-   successful borrow confirmation;
-   successful return confirmation after backend validation;
-   relevant reminders/notifications;
-   box location;
-   offline/stale-data indicators;
-   temporary unavailability when tower/box health prevents safe use.

## 19.2 Android App Must Not Act as Physical Inventory Authority

Android must not directly decide:

-   sensor state;
-   equipment presence;
-   unauthorized removal;
-   whether a physical item is truly the assigned item;
-   whether a return physically occurred.

Those decisions originate from IoT/backend validation.

## 19.3 Android Offline Mode

If phone loses internet:

-   show cached information only when useful;
-   label cached data with last update;
-   warn that availability may have changed;
-   disable live-dependent actions;
-   refresh automatically after reconnection.

## 19.4 Android Borrow Confirmation

Only show finalized cloud-confirmed transaction when backend confirms
it.

If architecture exposes a pending synchronization state safely, Android
may show:

`Transaction detected - synchronization pending`

instead of falsely claiming completion.

## 19.5 Android Return Guidance

For wrong box:

-   tell borrower return is not complete;
-   identify expected box/location;
-   ask borrower to place item correctly.

For return mismatch:

-   do not close transaction;
-   tell borrower to contact staff if necessary.

## 19.6 Android Privacy

Borrowers must not access:

-   other borrowers' transaction history;
-   other borrowers' overdue information;
-   admin-only security events;
-   system-wide account data;
-   administrative correction controls.

## 19.7 Android Notifications

Notification failure must not change canonical transaction status.

When app opens, it must load current backend state rather than assuming
notification delivery was successful.

## 19.8 Android Multiple Devices

If a borrower uses multiple Android devices:

-   all should synchronize from the same backend account;
-   no device should independently own the canonical inventory state.

------------------------------------------------------------------------

# 20. WEB ADMIN AND ANDROID COMMUNICATION RULE

> **The Web Admin and Android application must never directly depend on
> each other's source code.**

Correct communication model:

`ESP32 / IoT -> Firebase/Backend -> Web Admin`

and

`ESP32 / IoT -> Firebase/Backend -> Android App`

and for authorized client actions:

`Web Admin -> Firebase/Backend -> validated state`

`Android -> Firebase/Backend -> borrower-authorized requests/read operations`

Incorrect architecture:

`Android App -> directly edits Web Admin`

`Web Admin -> directly edits Android local state`

`Android -> directly changes sensor truth`

`Web Admin -> directly pretends a physical sensor changed`

Backend synchronization is the boundary between applications.

------------------------------------------------------------------------

# 21. Component Failure Independence

One component failure must not automatically disable unrelated
capabilities.

Examples:

## RFID Reader Fault

-   borrowing/return authentication unavailable;
-   sensor security monitoring may continue;
-   Web Admin shows RFID fault;
-   Android shows tower unavailable for authenticated transaction.

## Sensor Fault

-   affected box unavailable;
-   other healthy boxes may remain operational if architecture safely
    supports it;
-   Web Admin sees affected sensor;
-   Android sees affected equipment unavailable.

## Android Offline

-   tower and Web Admin continue.

## Web Admin Offline

-   tower and Android/backend continue.

## Cloud Offline

-   local tower security monitoring continues;
-   events queue locally;
-   both client apps show stale/offline state.

------------------------------------------------------------------------

# 22. Additional Operational Vulnerabilities

## 22.1 Entire Tower Is Stolen

Current FRS has no GPS and no physical box locks.

System cannot locate or physically prevent theft of the tower.

Document as limitation.

## 22.2 Sensor Cable Intentionally Disconnected

Detect where technically possible.

Set affected box unavailable and alert admin.

## 22.3 RFID Reader Wiring Loose

SPI failures must not be interpreted as valid UID data.

Enter RFID fault mode.

## 22.4 I2C Address/Configuration Conflict

Fail hardware initialization for affected subsystem.

Do not enter normal monitoring mode until sensor bus is reliable.

## 22.5 Firebase Data Accidentally Deleted

Use:

-   restricted admin permissions;
-   backups/export where appropriate;
-   append-oriented history;
-   avoid destructive deletion.

## 22.6 Cached Offline Authorization Becomes Stale

If offline borrowing is ever allowed using cached users:

-   cache must have expiration/version;
-   after excessive time without backend synchronization, disable new
    borrowing;
-   local security monitoring may continue.

## 22.7 Borrowed Item Is Borrowed Again

Backend must reject a second active loan for the same equipment.

## 22.8 Borrower Is Overdue and Attempts New Borrow

The FRS flags overdue accounts but does not define whether new borrowing
is blocked.

Recommended policy to confirm with instructor/team:

-   flagged/overdue borrower may still return equipment;
-   new borrowing may be blocked until cleared.

This is a policy decision and should be formally approved before
implementation.

------------------------------------------------------------------------

# 23. Testing Matrix

The team should test at minimum:

## Connectivity

-   disconnect Wi-Fi before tap;
-   disconnect Wi-Fi after tap;
-   disconnect during removal;
-   disconnect during return;
-   restore connection;
-   Firebase unavailable while Wi-Fi works;
-   Android offline;
-   Web Admin offline;
-   long offline period;
-   queue near capacity.

## Sensors

-   normal remove;
-   normal return;
-   rapid flicker;
-   sensor disconnected;
-   sensor blocked;
-   sensor misaligned;
-   sensor stuck;
-   environmental light interference;
-   loose wiring;
-   wrong object;
-   wrong box.

## RFID

-   valid UID;
-   invalid UID;
-   disabled UID;
-   duplicate tap;
-   repeated tap;
-   lost/replaced card;
-   RFID reader disconnected.

## Transactions

-   session timeout;
-   no item removed;
-   two removals;
-   two simultaneous box changes;
-   return without tap;
-   tap after return;
-   return by wrong borrower;
-   remove before tap;
-   remove then immediately replace;
-   overdue borrower attempts borrow.

## Power

-   normal restart;
-   power loss while idle;
-   power loss after tap;
-   power loss during removal;
-   power loss during return;
-   equipment removed while tower unpowered;
-   repeated ESP32 restart.

## Backend

-   duplicate event;
-   delayed event;
-   out-of-order event;
-   malformed payload;
-   impossible state transition;
-   database/sensor conflict;
-   security-rule unauthorized access;
-   notification failure;
-   cloud write failure.

## Web Admin

-   stale data display;
-   tower offline display;
-   sensor fault display;
-   reconciliation workflow;
-   admin override audit;
-   simultaneous admins;
-   session expiration;
-   unauthorized role access.

## Android

-   stale availability;
-   app crash/reopen;
-   multiple devices;
-   notification delayed;
-   notification missing;
-   wrong-box guidance;
-   tower unavailable;
-   offline cached display;
-   borrower tries to access another user's data.

------------------------------------------------------------------------

# 24. Highest-Priority Risks

## Critical

1.  Wrong physical object can satisfy a box sensor.
2.  Another person can remove equipment during someone else's RFID
    session.
3.  Offline transaction behavior can lose or corrupt state if not
    persisted.
4.  Power failure can interrupt transactions.
5.  Sensor malfunction can look like legitimate removal/return.

## High

6.  Physical/database state conflicts.
7.  Multiple removals during one session.
8.  Wrong-box returns.
9.  Duplicate/out-of-order cloud events.
10. Device credential compromise.
11. Missing admin audit trail.
12. Sensor obstruction/tampering.
13. Local offline queue exhaustion.

## Medium

14. RFID loss/cloning.
15. Stale Web/Android state.
16. Due/notification failure.
17. Hardware indicator failure.
18. environmental sensor instability.

------------------------------------------------------------------------

# 25. Fundamental Hardware Limitation

The current SmartDock design uses an IR break-beam/photointerrupter to
infer whether the assigned item is present in its designated box.

Therefore:

> **SmartDock detects box occupancy/presence conditions. It does not
> independently verify the physical identity of the object.**

For example:

-   microphone removed;
-   unrelated object placed into microphone box;
-   sensor becomes `PRESENT`.

The sensor alone cannot prove the microphone returned.

For the current prototype this should be treated as a documented
limitation.

A future version could use item-level RFID/NFC tagging or another
recognition mechanism to validate actual equipment identity.

------------------------------------------------------------------------

# 26. Recommended Professional Recovery Rule

Every failure should fall into one of these responses:

### A. Retry Automatically

Use for temporary:

-   Wi-Fi loss;
-   Firebase timeout;
-   I2C interruption;
-   notification retry.

### B. Continue in Degraded Mode

Use when safe:

-   cloud unavailable but local security monitoring works;
-   one noncritical client application is offline.

### C. Disable Affected Function

Use when safety/integrity cannot be guaranteed:

-   RFID reader failure -\> disable authenticated borrowing;
-   sensor failure -\> disable affected box;
-   local queue full -\> disable new transactions if events cannot be
    safely stored.

### D. Require Reconciliation

Use for:

-   wrong box;
-   unknown placement;
-   database/sensor mismatch;
-   post-outage mismatch;
-   ambiguous simultaneous events;
-   delayed conflicting event.

### E. Require Administrator Intervention

Use for:

-   persistent hardware failure;
-   repeated ESP32 restart;
-   lost equipment;
-   damaged equipment;
-   suspicious tampering;
-   unresolved transaction conflict.

------------------------------------------------------------------------

# 27. Final Implementation Principle for AI

When an AI coding assistant reads this document, it must first identify
**which SmartDock component it is currently implementing**:

-   ESP32 / IoT firmware
-   Firebase/backend
-   Web Admin
-   Android Borrower App

It must then implement only that component's responsibilities and
backend contracts.

If a requested feature spans components, separate the work explicitly.

Example:

### Feature: Sensor Failure

**ESP32 task** - detect sensor fault; - stop trusting affected sensor; -
publish hardware-health event.

**Backend task** - store health state; - prevent unsafe state
transitions.

**Web Admin task** - display sensor fault; - provide
maintenance/reconciliation workflow.

**Android task** - show affected equipment as temporarily unavailable.

Do not merge all four implementations into one codebase.

------------------------------------------------------------------------

# 28. Summary

SmartDock should never rely on a single "happy path." A professional
implementation must anticipate:

-   connectivity loss;
-   Firebase/backend failure;
-   stale applications;
-   sensor failure;
-   RFID failure;
-   I2C failure;
-   ESP32 failure;
-   power interruption;
-   incorrect physical placement;
-   wrong object placement;
-   concurrent users;
-   transaction ambiguity;
-   duplicate/out-of-order events;
-   security abuse;
-   data conflicts;
-   notification failure;
-   administrative mistakes;
-   malicious client requests;
-   physical tampering;
-   recovery after prolonged outages.

The most important architectural rule is:

> **Never guess when physical or transactional evidence is ambiguous.
> Preserve the evidence, mark the state as uncertain, and reconcile
> safely.**

The second major rule is:

> **Web Admin and Android Borrower App are separate applications. Their
> responsibilities must be implemented separately and synchronized only
> through the approved backend architecture.**

The third major rule is:

> **Physical presence does not equal verified equipment identity in the
> current prototype.**

These rules should guide implementation, testing, and future revisions
of the SmartDock FRS.
