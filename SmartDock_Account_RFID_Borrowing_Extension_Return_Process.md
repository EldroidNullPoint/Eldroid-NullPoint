# SmartDock Account, RFID Issuance, Borrowing, Extension, and Return Process Specification

## Purpose

This document supplements the SmartDock FRS with a strict end-to-end
process for account creation, RFID issuance, borrowing, configurable
school-hour/time limits, extension approval, return, overdue handling,
and administration.

The existing FRS already establishes registered borrowers, RFID
authentication, timed borrow/return sessions, due/overdue processing,
Web Admin monitoring, Android borrower monitoring, and one assigned item
per monitored box. It does **not** currently specify the full RFID
issuance workflow, a fixed 3-hour borrowing rule, school-hours
restrictions, or an extension-request/approval workflow. Those are
proposed requirements below and should be formally approved by the
team/instructor.

# CRITICAL AI / IDE SEPARATION

> **WEB ADMIN AND ANDROID BORROWER APP ARE SEPARATE APPLICATIONS,
> SEPARATE PROJECTS/IDES, AND MAY BE DEVELOPED BY SEPARATE AI
> SESSIONS.**
>
> An AI working on Web Admin must not create or assume access to Android
> source code. An AI working on Android must not create or assume access
> to Web Admin source code.
>
> Both applications communicate only through the approved
> Firebase/backend data model, authentication, backend rules/functions,
> notifications, and shared data contracts.
>
> The ESP32/SmartDock Tower is also separate. Physical RFID and sensor
> truth originates from the IoT/backend workflow, not from either client
> UI.

# 1. Component Responsibilities

## Android Borrower App

Responsible for registration/login, account approval status, RFID
status, equipment availability, personal loans, due time/countdown,
extension requests/status, notifications, and personal history.

Android must **not** issue RFID cards, approve accounts/extensions, edit
borrowing policy, directly change sensor truth, or directly mark
equipment returned.

## Web Admin

Responsible for registration review, borrower approval/rejection, RFID
issuance/activation/deactivation/replacement, equipment/box management,
school hours, maximum borrowing time, extension policy, extension
decisions, active/overdue monitoring, reconciliation, overrides, and
audit history.

## Firebase / Backend

Authoritative for permissions, account/RFID mapping, borrowing
eligibility, operating-hours validation, duration/due-time calculation,
extension validation, active-loan rules, concurrency, transaction
history, overdue processing, and notifications.

## ESP32 / Tower

Responsible for RFID reading, short physical sessions, sensor
removal/placement events, LEDs/buzzer, unauthorized-removal detection,
and physical-event submission.

# 2. Borrower Account Lifecycle

Recommended states:

`REGISTERED_PENDING_VERIFICATION -> APPROVED_NO_RFID -> RFID_ASSIGNED -> ACTIVE`

Other states: `REJECTED`, `SUSPENDED`, `RFID_LOST`, `RFID_DISABLED`,
`OVERDUE_RESTRICTED`, `INACTIVE`.

Having an Android account does **not** automatically mean the user may
borrow equipment.

# 3. Account Creation

## Android Process

1.  User selects Register.
2.  User enters institutionally required data such as name, student ID,
    approved email, password, and only other fields genuinely needed.
3.  Android performs basic validation.
4.  Backend validates required fields, uniqueness, duplicate student
    ID/account, and data format.
5.  Account becomes `REGISTERED_PENDING_VERIFICATION`.
6.  User may log in but cannot physically borrow.
7.  Android displays: **Account created. Borrower access is awaiting
    administrator verification.**

## Web Admin Process

1.  Pending registration appears in **Pending Borrower Registrations**.
2.  Admin reviews the account.
3.  Admin approves or rejects.
4.  Approved account becomes `APPROVED_NO_RFID`.
5.  Rejection may store a reason.
6.  Approval/rejection is audited.

# 4. RFID Card Issuance

The FRS requires registered borrower RFID cards but does not define
issuance. Recommended process:

1.  Borrower account is approved.
2.  Authorized staff provides a physical RFID card.
3.  Web Admin opens borrower profile and selects **Assign RFID**.
4.  System enters controlled enrollment mode.
5.  Physical card is tapped on the MFRC522/enrollment reader.
6.  UID is sent through the trusted IoT/backend path.
7.  Backend validates UID.
8.  Web Admin shows the pending card association.
9.  Admin confirms borrower-card assignment.
10. Backend stores mapping.
11. RFID becomes active.
12. Borrower becomes `ACTIVE`.
13. Android shows **RFID Active**.

## RFID Validation

Before assignment verify: - UID valid; - UID not assigned to another
active borrower; - borrower approved; - admin authorized; - existing
active card handled according to replacement policy.

Audit borrower, card reference, admin, assignment time, and status.

## Android RFID Status

Show only user-friendly state: - RFID not yet issued - RFID active -
RFID disabled - RFID reported lost - replacement required

Do not unnecessarily expose raw UID.

# 5. Lost, Damaged, and Replacement RFID

Lost card: 1. Borrower reports loss through Android if implemented,
otherwise staff. 2. Old card becomes `LOST/DISABLED`. 3. It can no
longer start borrowing. 4. Admin issues replacement. 5. New UID is
linked to the same borrower. 6. Historical loans remain attached to
borrower ID.

Damaged cards follow the same replacement process.

If a borrower has an active loan when RFID is replaced, the loan remains
theirs and the new active card can authenticate their return.

# 6. School/Operating Hours

New borrowing must only be allowed during centrally configured operating
hours.

**Never hard-code hours independently in Android, Web, and ESP32.**

Web Admin settings should support: - borrowing days; - opening time; -
borrowing cutoff; - return cutoff; - holidays; - special closure
dates; - one-day special hours; - borrowing enabled/disabled; - returns
enabled/disabled.

Recommended distinction: - **New borrowing:** restricted to configured
borrowing hours. - **Returns:** preferably allowed whenever the
SmartDock is physically accessible, even when new borrowing is closed.

This prevents policy from forcing borrowers to become overdue because
the system refuses a return.

# 7. Maximum Borrowing Duration

Proposed default:

> **3 hours maximum normal borrowing time.**

It must be administrator-editable and stored centrally,
e.g. `defaultMaxBorrowMinutes = 180`.

For the prototype, prefer one global duration. Future versions may
support per-equipment/category rules if required.

# 8. Due-Time Calculation

Store authoritative timestamps:

`borrowedAt` `originalDueAt` `currentDueAt`

Normal calculation:

`normalDue = borrowedAt + configuredMaxDuration`

Final due time should not exceed configured same-day return cutoff:

`dueAt = min(normalDue, returnCutoff)`

Example: - borrow 1:00 PM, 3-hour max -\> 4:00 PM; - borrow 3:30 PM,
return cutoff 5:00 PM -\> 5:00 PM, not 6:30 PM.

Admin should also be able to configure `minimumBorrowMinutes`. If too
little time remains before cutoff, backend may reject a new loan with
**Borrowing unavailable because the facility is closing soon.**

# 9. Borrowing Eligibility

Before authorizing a new loan, backend verifies: 1. account active; 2.
RFID active and linked; 3. current time within borrowing hours; 4.
equipment available; 5. tower/box healthy enough; 6. no conflicting
active transaction; 7. borrower not policy-blocked; 8. overdue
restriction satisfied; 9. maximum concurrent loans not exceeded; 10.
enough permitted time remains before cutoff.

Only backend-authorized eligibility should open a borrow session.

# 10. Maximum Concurrent Loans

This is not defined in the current FRS and should be decided.

Recommended prototype default:

> **1 active item per borrower.**

Make `maxActiveLoansPerBorrower` centrally configurable if implemented.

# 11. Android Pre-Borrow Experience

Android should show: - item name; - availability; - box/location; -
current borrowing hours; - normal maximum duration; - current account
eligibility; - closing/cutoff information where useful; - tower/offline
warning when physical availability cannot be confirmed.

Viewing `Available` does not reserve the item.

# 12. Physical Borrow Process

1.  Borrower may check Android.
2.  Borrower goes to SmartDock.
3.  Borrower taps assigned RFID.
4.  Tower/backend validates account and policy.
5.  Short borrow session opens.
6.  Tower gives authorization feedback.
7.  Borrower removes **one** item.
8.  First valid removal binds box/equipment to borrower.
9.  Session immediately closes.
10. Transaction is persisted/synchronized.
11. Backend calculates due time.
12. Android receives active-loan information.
13. Web Admin Active Borrowings updates.

If invalid RFID, outside borrowing hours, restricted borrower, faulty
box, unavailable item, or other eligibility failure: **no borrow
session**.

# 13. Android After Borrow

Display: - borrowed equipment; - borrow time; - due time; -
box/location; - time remaining; - extension eligibility; - transaction
status.

Example: **Borrowed:** Microphone 01\
**Borrowed at:** 1:15 PM\
**Due:** 4:15 PM\
**Time remaining:** 2h 59m

The countdown is informational. Backend `dueAt` is authoritative, not
phone clock.

# 14. Reminder Policy

Recommended configurable reminders: - 30 minutes before due; - 10
minutes before due; - at due time; - overdue.

Values should be backend/admin configurable where practical. Avoid
excessive notifications.

# 15. Extension Request Process

A borrower must **not automatically extend their own loan**.

Flow:

`Android request -> backend validation -> Web Admin review -> approve/reject -> backend updates -> Android receives decision`

## Android

For an eligible active loan show **Request Extension**.

Borrower supplies: - requested duration from permitted options; -
concise reason.

Possible options: - +30 minutes - +1 hour - other allowed choices

Android must never directly edit `dueAt`.

After submission: `EXTENSION_PENDING`

Display: \> Extension request submitted. Your current due time remains
unchanged until approved.

**A pending request does not pause or extend the current due time.**

# 16. Backend Extension Validation

Check: - active loan exists; - request belongs to borrower; - item not
already returned; - request is within allowed request window; - no
duplicate pending request; - requested duration allowed; - proposed due
does not exceed return cutoff unless authorized override policy allows
it; - extension-count limit not exceeded; - borrower is eligible.

# 17. Web Admin Extension Queue

Recommended page: **Borrowing -\> Extension Requests**

Display: - borrower; - equipment; - borrowedAt; - current due time; -
time remaining/overdue state; - requested duration; - borrower reason; -
previous extensions; - proposed new due time; - Approve; - Reject.

## Approval

Backend validates and calculates final new due time.

Store: - request ID; - original due; - approved duration; - new due; -
approving admin; - approval time.

Android receives **Extension approved. New due time: ...**

## Rejection

Store admin, time, and rejection reason.

Android receives **Extension declined. Original due time remains ...**

# 18. Pending Extension at Due Time

If no approval exists when current due time passes:

`ACTIVE -> OVERDUE`

Pending extension does not silently prevent overdue status.

# 19. Extension Policy Settings

Admin-configurable: - extensions enabled; - permitted extension
durations; - maximum extension duration; - maximum approved extensions
per loan; - latest request time; - whether overdue requests are
allowed; - whether closing-time override is allowed; - who may approve.

Recommended prototype policy: - request before due time; - one pending
request at a time; - one approved extension per loan; - final due cannot
exceed return cutoff; - overdue loan requires staff intervention instead
of normal self-request.

# 20. Admin Manual Due-Time Adjustment

Exceptional admin action: **Adjust Due Time**.

Require: - new due time; - reason; - admin identity; - timestamp.

Preserve original due time in audit/history. Backend still validates
permissions/policy.

# 21. Normal Return Process

1.  Borrower approaches SmartDock with item.
2.  Borrower taps RFID.
3.  Backend/tower recognizes active loan.
4.  Return session opens.
5.  Expected box is identified.
6.  Borrower places item in correct box.
7.  Expected sensor becomes present.
8.  ESP32 validates placement during return session.
9.  Return transaction is recorded.
10. Active loan closes.
11. Equipment becomes available unless maintenance/inspection hold
    applies.
12. Android receives return confirmation.
13. Web Admin updates inventory/history.

# 22. Return Outside Borrowing Hours

Recommended:

> Returns remain allowed whenever SmartDock is physically accessible
> even when new borrowing is closed.

Borrowing and returning should therefore be separate policy switches.

# 23. Wrong Box Return

If the wrong box changes: - do not close loan; - red/error feedback; -
identify expected box; - log unexpected placement; - Web Admin sees
mismatch; - Android shows return incomplete.

Current hardware detects box presence, not true physical object
identity.

# 24. Return Without RFID

Do not automatically attribute an object placement to a borrower.

Create `UNVERIFIED_PLACEMENT`.

Web Admin may reconcile. Android active loan remains until validated
return/reconciliation.

# 25. Late Return

Late returns must still be accepted.

`OVERDUE -> RETURNED`

Preserve original due, actual return time, overdue duration,
notifications, and escalation history.

# 26. Overdue Process

At due time without validated return:

`ACTIVE -> OVERDUE`

Backend: - marks overdue; - notifies borrower; - updates Web Admin; -
applies configured account restriction.

The FRS already refers to a grace period. Make `overdueGraceMinutes`
admin-configurable.

Recommended restriction: - overdue borrower cannot start a new loan; -
overdue borrower can still return; - exceptional clearing is audited.

# 27. School Closing With Active Loans

System should avoid generating due times beyond return cutoff.

Before closing: - send reminders; - warn borrower; - deny new loans when
minimum usable time is unavailable.

Do not silently move due time to next day unless the institution
formally adopts that rule.

# 28. Holidays and Special Closures

Admin may configure: - holidays; - emergency closure; - shortened day; -
special event hours.

If a schedule change would make an existing active loan impossible to
return on time, Web Admin should warn the administrator and require an
explicit resolution rather than silently changing the due time.

# 29. Policy Changes During Active Loans

Example: max duration changes from 3 hours to 2 hours.

Recommended: - existing loan keeps stored `currentDueAt`; - new policy
applies to future loans; - changing an active loan requires audited
due-time adjustment.

# 30. Equipment-Specific Rules

Future-ready options: - non-borrowable equipment; - maintenance; -
staff-only equipment; - category-specific duration; - specific
eligibility.

Avoid unnecessary prototype complexity unless required.

# 31. No Reservation Assumption

The current FRS does not define reservations.

Therefore: - `Available` does not mean reserved; - viewing an item does
not hold it; - extension request does not reserve anything.

Reservations require a separate specification if later added.

# 32. Borrowing When Web Admin Is Offline

Routine borrowing must **not require an administrator browser to be
open**.

Backend/tower enforce routine policy.

Admin is required for: - registration approval; - RFID issuance; -
extension decisions; - reconciliation; - exceptional overrides.

# 33. Extension With No Admin Response

No response is not approval.

Original due time remains active.

# 34. Temporarily Disable Borrowing

Web Admin should support: `Borrowing Enabled: ON/OFF`

Useful for maintenance, inventory checks, emergencies, and events.

Returns should remain separately configurable.

Android shows **New borrowing temporarily unavailable.**

# 35. Approved Account Without RFID

Android: **Account approved. RFID card issuance required before
borrowing.**

Web: **Approved - RFID not assigned.**

Tower cannot authenticate until card is assigned.

# 36. RFID Active Without Android Logged In

Physical borrowing may still work because RFID identifies the active
borrower at the tower.

Android is a monitoring companion, not the physical authentication
token.

# 37. Phone Account and RFID Mismatch

The phone currently logged in must never override RFID ownership.

Backend RFID mapping determines the tower borrower identity.

# 38. Duplicate RFID Assignment

If admin attempts to assign a UID already used by another active
borrower: reject and require controlled resolution.

# 39. Suspension With Active Loan

If borrower is suspended: - block new borrowing; - allow return; -
retain active loan/due obligation; - preserve history.

# 40. Account Deactivation With Active Loan

Do not hard-delete borrower. Preserve enough account/history state for
return and reconciliation.

# 41. Extension Reasons

Borrower should provide a concise operational reason, e.g. class
activity still ongoing, presentation extended, instructor requested
continued use.

Do not request unnecessary sensitive information.

Rejection reason should normally be provided. Approval note may be
optional.

# 42. History

Android: borrower sees own history.

Web Admin: authorized system-wide history.

Recommended history: - item; - borrow time; - original due; - extension
requests/decisions; - final due; - return time; - overdue state.

Borrower cannot edit history.

# 43. Audit Log

Web Admin audit should include: - account approval/rejection; - RFID
assignment/disable/replacement; - borrowing-policy changes; - extension
decisions; - manual due adjustments; - manual returns; - equipment
status overrides; - restriction changes.

# 44. Recommended Web Admin Screens

> **WEB PROJECT ONLY**

1.  Dashboard
2.  Pending Borrower Registrations
3.  Borrower Management
4.  Borrower Detail
5.  RFID Assignment/Replacement
6.  Equipment Management
7.  Box Assignment
8.  Active Borrowings
9.  Extension Requests
10. Overdue Borrowings
11. Transaction History
12. Alerts/Reconciliation
13. Borrowing Policy Settings
14. Operating Hours/Holiday Settings
15. Audit Log

# 45. Recommended Android Screens

> **ANDROID PROJECT ONLY**

1.  Register
2.  Login
3.  Account Approval Status
4.  RFID Status
5.  Home/Dashboard
6.  Equipment Availability
7.  Equipment Detail
8.  My Borrowed Items
9.  Active Borrow Detail
10. Extension Request
11. Extension Request Status
12. Due/Overdue Information
13. Borrowing History
14. Notifications
15. Profile/Account

Physical borrowing/return remains a tower action. Android guides and
reflects it.

# 46. Recommended Backend Records

## Borrower

-   borrowerId
-   accountStatus
-   rfidStatus/reference
-   borrowingEligibility
-   activeLoanCount
-   restrictionState

## RFID Card

-   cardId/reference
-   UID representation
-   assignedBorrowerId
-   status
-   assignedAt
-   disabled/lost/replacedAt

## Equipment

-   equipmentId
-   name
-   boxId
-   availability/businessStatus
-   maintenanceState

## Borrow Transaction

-   transactionId
-   borrowerId
-   equipmentId
-   boxId
-   borrowedAt
-   originalDueAt
-   currentDueAt
-   returnedAt
-   status
-   extensionCount

## Extension Request

-   requestId
-   transactionId
-   borrowerId
-   requestedDuration
-   reason
-   requestedAt
-   status
-   reviewedBy
-   reviewedAt
-   decisionReason
-   resultingDueAt

## Borrowing Policy

-   timezone
-   allowedDays
-   openingTimes
-   borrowingCutoff
-   returnCutoff
-   defaultMaxDuration
-   minimumBorrowMinutes
-   maxActiveLoans
-   gracePeriod
-   extensionRules
-   reminderRules

# 47. Timezone

Use the school's authoritative timezone for all policy calculations.

For the intended Cebu deployment: `Asia/Manila`

Browser/phone timezone must not redefine school hours.

# 48. Race Conditions to Protect Against

Backend must handle: - two borrowers attempting same item; - two admins
reviewing same extension; - return while extension is being approved; -
policy change during borrow validation; - card disabled while session
begins; - item changes state while Android displays availability.

Use backend atomic/transactional validation where appropriate.

# 49. Extension Approved After Return

If item already returned, do not modify completed transaction.

Request becomes invalidated/cancelled because the loan is complete.

# 50. Approval Exactly at Due Time

Use authoritative backend/server time and atomic processing. Android and
Web must not independently decide event ordering.

# 51. Borrow Near Closing

Example: 4:50 PM, return cutoff 5:00 PM, max duration 3 hours.

Use configurable `minimumBorrowMinutes`.

If remaining permitted time is too short, reject new borrowing.

# 52. Complete Happy Path

## A. Registration

**Android:** register -\> login -\> pending status.\
**Web:** review -\> approve.\
**Android:** approved, RFID required.

## B. RFID Issuance

**Web + Reader/Tower:** select borrower -\> enrollment mode -\> tap
physical card -\> validate UID -\> confirm assignment.\
**Backend:** activate mapping.\
**Android:** RFID Active.

## C. Borrow

**Android:** user may check availability/policy.\
**Tower/Backend:** tap RFID -\> eligibility check -\> short session -\>
remove one item -\> sensor event -\> close session -\> create
transaction -\> calculate due.\
**Android:** active loan/countdown.\
**Web:** active borrowing appears.

## D. Extension

**Android:** request extension + reason -\> pending; original due
remains.\
**Web:** review -\> approve/reject.\
**Backend:** validate/calculate new due if approved.\
**Android:** display result.

## E. Return

**Tower:** tap RFID -\> identify active loan/expected box -\> place item
-\> sensor validates.\
**Backend:** close loan -\> update availability.\
**Android:** return confirmation/history.\
**Web:** move from active list to history.

# 53. Policy Decisions Team Must Formally Approve

1.  Exact school/borrowing hours.
2.  Whether returns are allowed after new borrowing closes.
3.  Default max duration --- proposed 3 hours.
4.  Minimum remaining time required to borrow.
5.  Maximum active items per borrower.
6.  Grace period.
7.  Whether overdue blocks new borrowing.
8.  Maximum extension duration.
9.  Maximum extension count.
10. Extension request timing window.
11. Whether overdue extension requests are allowed.
12. Who may approve extensions.
13. Whether admins may override return cutoff.
14. Holiday/special closure behavior.
15. Whether returns require inspection before availability.
16. Exact RFID enrollment hardware/process.
17. Whether Android supports self-reporting lost RFID.
18. Whether institutional identity verification is required for
    approval.

These are proposed policy decisions, not facts already defined by the
FRS.

# 54. AI Implementation Instructions

## AI OWNS WEB ADMIN

Implement only registration review, borrower/RFID management, policy
settings, active/overdue monitoring, extension review, reconciliation,
overrides, audit UI, and Web/backend integration. **Do not create
Android screens/code.**

## AI OWNS ANDROID

Implement only borrower registration/login, approval/RFID status,
availability, personal active loan, due/countdown, extension
request/status, personal history, notifications, and Android/backend
integration. **Do not create admin approval or Web code.**

## AI OWNS BACKEND

Implement authoritative policy, permissions, account states, RFID
mapping, borrowing validation, due calculations, extension workflow,
consistency, concurrency, notifications, and audit records.

## AI OWNS ESP32

Implement RFID reading, physical sessions, sensor events, local
feedback, session timeout, backend event submission, and safe
offline/reconnection behavior.

# 55. Final Rules

1.  Account registration is not borrowing authorization.
2.  RFID must be explicitly issued and linked to an approved borrower.
3.  RFID identity belongs to the borrower account, not the Android
    phone.
4.  New borrowing is controlled by centrally configured school hours.
5.  Proposed normal maximum is 3 hours, but admin can edit it.
6.  Due time respects configured return cutoff.
7.  Android cannot extend its own due time.
8.  Extension requires backend validation and admin approval.
9.  Pending extension does not change current due time.
10. Returns should remain possible after borrowing closes when the tower
    is accessible and policy allows it.
11. Overdue borrowers must still be able to return.
12. Policy changes do not silently rewrite existing active loans.
13. Sensitive admin overrides/policy changes are audited.
14. Web Admin and Android are separate applications/codebases.
15. Shared business rules belong in backend contracts, not duplicated
    client assumptions.
16. Physical RFID/sensor truth originates from the tower/backend.
17. Current presence sensors do not prove physical item identity.
18. Ambiguous evidence must be preserved and safely reconciled rather
    than guessed.
