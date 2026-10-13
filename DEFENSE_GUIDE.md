# TeleConsult - Defense Guide

Everything is stored in memory using Java data structures only (no database, as required).

## Data structures used and why

| Structure | Where | Why this one |
|---|---|---|
| `ArrayList` | `DataStore`: users, appointments, consultation requests, medical records | Records are added in order and shown in tables. Needs ordered, indexable storage. |
| `HashMap<String, User>` | `DataStore.usersByEmail` | Login and "does this e-mail exist" become **O(1)** instead of scanning every user (**O(n)**). The index repairs itself if an e-mail is edited or a user is deleted. |
| `LinkedHashMap<LocalDate, ...>` | `Schedule` (open days, free times) | Direct lookup of a date, and dates stay in the order they were added. |
| `HashSet<String>` (per date) | `Schedule.bookedTimes` | A slot is either booked or not. Set gives **O(1)** "is it booked?" and makes a double booking impossible (no duplicates). |
| `HashSet<String>` | Doctor dashboard (distinct patients) | Counts unique patients without the O(n) `list.contains` check. |
| `TreeSet<String>` | Doctor profile (available dates/times) | Keeps dates and times automatically sorted for display. |
| `PriorityQueue` (heap) with a `Comparator` | `AppointmentService.getPendingQueue` | A doctor's waiting list: **Senior and PWD requests first**, and inside each group the request that arrived first (lowest id). The dashboard shows the queue head and "+N more waiting". A heap gives the next patient in O(1) (`peek`) and inserts in O(log n). |
| `Queue` (`ArrayDeque`, FIFO) | `MedicalRecordService.getVitalsQueue` | Patients waiting for a nurse to take vitals, oldest first. The nurse dashboard shows the next patient. |
| `HashMap<String,Integer>` / `HashMap<String,Long>` | `AuthenticationService` | Failed-login count and lock-until time per e-mail (5 failures = 60 s lock). |
| `HashMap<User, ArrayDeque<Notification>>` used as a **stack** (LIFO) | `DataStore.notifications`, `NotificationService` | Each user has their own stack; `push` puts the newest notification on top, so the inbox shows newest first. The map finds a user's stack in O(1). Each stack is capped at 100 (oldest removed from the bottom). |
| `PriorityQueue` (heap) with a `Comparator` | `AppointmentService.getNurseReviewQueue` | The nurses' shared to-do list: new requests nobody has reviewed yet, priority patients first, then oldest first. |
| `Comparator` sorting | Queues, `Schedule` times | Orders requests by arrival id and time slots by clock time. |

### Likely questions
- **Why HashMap for e-mail and not the list?** Lookup is O(1) on average vs O(n) scanning; login happens often.
- **Why a Set for booked slots?** Membership test is O(1) and duplicates cannot exist.
- **Why a Queue for requests?** Fairness: first come, first served (FIFO).
- **Why does data disappear on close?** Requirement: data structures only, no database. Demo data is re-created at start.
- **Weakness / future work?** Records link people by full name (made safe by requiring unique names); with a database they would use IDs.

## Roles and flow (one-minute summary)
1. **Patient** books a slot with a doctor - status `PENDING`.
2. **Nurse** reviews the request and the patient's self-reported vitals, then forwards it (`FOR_DOCTOR_REVIEW`) or returns it.
3. **Doctor** approves (`DOCTOR_APPROVED`) or rejects, with the patient's history with that doctor shown on screen.
3. **Secretary** confirms (`CONFIRMED`) or rejects. A rejection frees the slot again.
   The **patient** may cancel (`CANCELLED`) any time before the date, which also frees the slot.
   After the visit the **doctor** marks it `COMPLETED`.
4. **Consultation**: patient requests, doctor approves/starts/completes with notes - a **medical record** is created.
5. **Nurse** reviews new requests first (FIFO queue, oldest first) and checks or enters vitals; later she can still add vitals to records waiting in the vitals queue.
6. **Admin** verifies new doctor/nurse/secretary accounts and oversees everything. Deleting a doctor cancels that doctor's upcoming appointments and tells the patients. The **secretary** works the "needs confirmation" list first and can cancel a confirmed booking.

### Notifications (one-minute explanation)
- Events in the service layer (book, approve, confirm, decline, consult, record, vitals, register) call `NotificationService`, so the UI never has to remember to notify anyone.
- The red badge on the sidebar button shows the unread count; the inbox page lists notifications newest first with a red dot on unread ones.

## Demo script (about 5 minutes)
1. Log in as a **patient**, book an appointment with a doctor (pick a weekday).
2. Log in as the **doctor**: dashboard shows the request at the head of the queue; approve it.
3. Log in as the **secretary**: confirm it (or reject to show the slot is freed).
4. Back as the patient: send a consultation request; as the doctor start and complete it with notes.
5. Log in as the **nurse**: dashboard shows the patient waiting for vitals; record them.
6. Log in as **admin** (Ctrl+Shift+A on the login page): show counts and account verification.
7. After each step, log in as the next person and point at the **red notification badge**.
8. As the patient, cancel a booking and show the slot is bookable again; as the doctor, mark a confirmed visit as completed.
9. Show a guard rule: try to book a past date, or register a duplicate name.

Demo logins are in `README_FIXED.md`.

## Extra: account security screens
- *Why does forgot password ask for a date of birth / license number?* There is no e-mail server or database in this
  project, so the system cannot send a reset link. It confirms the person with details given at registration, limits
  wrong guesses, and never says whether an e-mail exists. A real system would e-mail a one-time link instead.
- *Where do I change my password?* Profile > Account Information > Change Password.
- *What does Esc do?* Asks "Do you want to log out?" (Yes / No). It protects against closing a session by accident.

## Extra: Senior / PWD / Regular patient types
- *How is the patient type decided?* Senior is **calculated** from the date of birth (age 60 or above), so it cannot be picked wrongly. PWD cannot be known from age, so the patient **declares** it and gives a PWD ID number that the nurse can check on the appointment row. Everyone else is Regular.
- *What does priority do?* Seniors and PWDs are reviewed before regular patients, first by the nurse and then by the doctor. Inside each group the order stays first come first served, so nobody regular is skipped forever by other regulars.
- *Why a PriorityQueue and not sorting a list?* It always hands out the most urgent patient next (`peek` is O(1), `add` is O(log n)) and the ordering rule is one `Comparator`, `PRIORITY_THEN_ARRIVAL`.
- *Why is the type saved on the appointment?* A booking keeps the type it had when it was made. If the profile changes later (for example the PWD box is unticked) old requests are not rewritten.
- *Weakness / future work?* PWD is self-declared; a real clinic would verify the ID card in person. Real-life discounts are out of scope (no payments).
