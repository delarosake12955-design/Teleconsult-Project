# TeleConsult — final updated project

## Run in IntelliJ IDEA
1. Extract this ZIP.
2. Open the folder that contains `pom.xml` as a Maven project.
3. Set the project SDK to JDK 25 and reload Maven dependencies.
4. Run `ph.edu.teleconsultation.system.portal.MainApp` or run `mvn clean javafx:run` in the project terminal.

## Demo accounts
- Admin: `admin@teleconsult.com` / `admin123`
- Doctor: `doctor@teleconsult.com` / `doctor123` (second doctor: `maria@teleconsult.com` / `doctor123`)
- Nurse: `nurse@teleconsult.com` / `nurse123`
- Secretary: `secretary@teleconsult.com` / `secretary123`
- Patient: `patient@teleconsult.com` / `patient123`
- Senior patient: `senior@teleconsult.com` / `senior123` (age is calculated, always a senior)
- PWD patient: `pwd@teleconsult.com` / `pwd12345` (PWD ID `PWD-2026-0001`)

## Final updates
- Preserved the existing JavaFX pages, visual theme, dashboard/sidebar layout, and page styling.
- Appointment requests now follow the SRS review sequence: patient request → nurse review → doctor approval → secretary schedule confirmation.
- Doctors can review only appointments assigned to them; secretaries confirm only doctor-approved requests; admin and nurse appointment pages are monitoring-only.
- Rejecting an appointment returns that exact time slot to the doctor's availability calendar.
- Added a service-level duplicate-slot guard so the same doctor/date/time cannot be requested twice while an earlier request is active.
- Improved null-safe appointment lookups and kept appointment data in memory, as specified by the SRS.
- Navigation remains role-aware and inner pages are intended to load inside the existing dashboard content area.

## Notes
The application uses in-memory data, so newly registered users and records reset when the app closes. The non-UI model/data/service layer was compiled and the appointment workflow was smoke-tested. The JavaFX window could not be launched in this build environment because Maven and JavaFX runtime dependencies are not installed here; please run once in IntelliJ after Maven finishes downloading dependencies.

## Review fixes (latest)
- **Unique full names**: appointments, consultations and medical records are linked by full name, so registration and profile edits now refuse a name another account already uses (prevents patients seeing each other's records).
- **Doctor dashboard**: "today's appointments" now counts doctor-approved and confirmed requests (it used a status that never occurs and always showed 0).
- **Admin, patient, nurse and secretary dashboards** now show live data (counts, next appointment, latest record, pending items) instead of placeholder text.
- **Schedule/booking**: booked slots can no longer reappear when a doctor toggles a day off/on or re-adds the time; rejecting a request restores only its own slot and never reopens a day the doctor blocked; blocking every date no longer resets the calendar; the calendar now stays filled 120 days ahead; times are kept in order; past dates and already-passed hours today cannot be booked or edited; the doctor sees the day's open slots.
- **Workflow**: a secretary can only reject requests the doctor has already approved; secretary accounts must be verified by an administrator.
- **Consultation requests** must be for a day the chosen doctor is available (unavailable days are greyed out in the date picker).
- **Vital signs**: nurses get a "Record Vital Signs" button on the medical-records page (validated ranges).
- **Privacy**: a doctor's patient list only shows patients who have booked or consulted with that doctor.
- **Security**: passwords are stored as salted PBKDF2 hashes (never plain text) and sign-in locks an email for 60 seconds after 5 failed attempts.
- **Window size**: moving between pages (login, register, home) no longer resizes the window or drops the F11 fullscreen shortcut; the window opens at 1200x750 or the screen size if that is smaller (minimum 1000x650).
- **Date picker**: new blue calendar theme (`date-picker-theme.css`) on the registration and consultation-request pages; birth dates in the future are greyed out and rejected, and the field also accepts typing (MM/DD/YYYY).
- **Data structures**: users are indexed by e-mail in a `HashMap` (O(1) login/lookup); doctors have a FIFO `Queue` of pending requests and nurses a FIFO `Queue` of records waiting for vitals. Unused classes were removed. See `DEFENSE_GUIDE.md` for the full list, likely questions and a demo script.
- **Notifications**: every role has a "Notifications" button in its sidebar with a **red badge** showing the unread count (hidden when there are none). The inbox page (`notifications.fxml` + `notifications.css`) shows unread items with a red dot; click a card to mark it read, "Dismiss" to remove it, or use "Mark all as read" / "Clear all". Notifications are created automatically: new appointment request (doctor), doctor approval (patient + secretaries), confirmation (patient + doctor), decline (patient, and the doctor if a secretary declined an approved request), new/approved/declined consultation request, consultation started, new medical record (patient), vitals needed / recorded (nurses, patient, doctor), and new accounts awaiting verification (admins and secretaries).
- **Admin shortcut fix**: Ctrl+Shift+A on the login page works again (it is now a scene filter that is removed when you leave the page, so it works even while a text box has focus).

## Pop-ups, loading screens and registration dialogs (latest)
New files (the look reuses the existing palette; nothing existing was restyled):
- `popup-theme.css` – styles for everything below.
- `notification-toast.fxml` + `ToastController` – the notification pop-up.
- `login-loading.fxml` + `LoadingController` – the sign-in loading screen.
- `registration-popup.fxml` + `RegistrationPopupController` – the registration result card.
- `OverlayHost`, `NotificationToast`, `LoadingOverlay`, `RegistrationPopup` – small helpers that show them.

What you will see:
- **Notification pop-up**: whenever the signed-in user receives a notification (`NotificationService.notifyUser`),
  a card slides in at the top-right (blue = info, green = success, amber = warning, red = alert), the red sidebar
  badge updates, a thin bar counts down 6 s, hovering pauses it, `x` closes it, and clicking it opens the
  Notifications inbox. After sign-in, one "You have N unread notifications" pop-up appears.
- **Sign-in loading screen**: after a correct password the glass loading screen shows a badge for the role
  (Patient / Doctor / Nurse / Secretary / Admin portal) and role-specific steps, then the dashboard fades in.
  Wrong passwords still show the normal red message on the login form (no loading screen).
- **Registration dialog**: Patient -> green check, "You're Registered!". Doctor / Nurse / Secretary -> amber
  "Waiting for Verification" card with the 3 steps (received -> admin verifies -> you can sign in).

Small edits to existing files: `MainApp` (installs the pop-ups, page changes go through `OverlayHost.setPage`),
`Navigator` (same + clears pop-ups on logout), `LoginController`/`AdminLoginController` (loading screen),
the four `*RegistrationController`s (one line each to open the dialog), `NotificationService` (`addListener`).

Not verified: JavaFX is not installed in the build sandbox, so these screens were not run. The new classes were
compiled against JavaFX stand-ins, the three FXML files were checked against their controllers (fx:ids, handlers,
CSS classes), and the notification hook was unit-tested. Please run once and tell me about any visual tweaks.

## Verification requests and blue profiles

- The **Selected Request** panel keeps its original NAME / EMAIL / ROLE cards. A second row of cards in the same style now shows **LICENSE NUMBER**, **SPECIALTY / DEPARTMENT** and **ACCOUNT STATUS** so the reviewer can see the license before approving. A missing license shows in red as "Not provided"; secretaries show "Not required". The approval message repeats the license number. (`verification-license.css`)
- All profile pages (patient, doctor, nurse, secretary, admin, generic and the doctor "view profile") use the blue theme from `profile-blue.css`: blue gradient header, white avatar, readable white name/role/e-mail, blue-focus fields and a blue Save button.

## Passwords and verification layout

- **Registration** (patient, doctor, nurse, secretary): new **Confirm Password** field and a **Show password** checkbox that reveals both password fields. Registration is refused with "Passwords do not match" if they differ. (`PasswordToggle.java`, `password-toggle.css`)
- **Admin login**: "Show password" now works. The page was missing its visible text field, so the checkbox had nothing to show.
- **Verification requests**: the six cards in the Selected Request panel now have equal widths, so the License / Specialty / Status row lines up exactly under Name / Email / Role.

## Homepage and register page refresh (latest)
- **Homepage hero**: the odd "WELCOME TO  01" label is now a "WELCOME TO TELECONSULT" badge. New headline
  "Healthcare that comes to you." with a description of the real flow (request, care-team review, confirmed schedule).
  The "01 / 04" numbers in the stat cards were replaced with real figures (5 user roles, 3 review steps, 24/7 access).
  The glass card on the right now shows the four role icons. Nav buttons (Services / About / Contact) scroll to the
  real section instead of fixed positions, so they stay correct if the layout changes.
- **Register page**: the four wide buttons are now a 2x2 grid of square role cards. Each card has a coloured
  rounded-square icon tile (patient = blue heart-person, doctor = teal cross-person, nurse = pink cap-person,
  secretary = amber clipboard), a short description, and a pill saying "Active instantly" (patient) or
  "Admin approval required" (staff). Hover lifts the card and colours its border.
- **New shared file `role-icons.css`**: the four icons are vector shapes drawn in CSS (`-fx-shape`), so no image files
  are needed. Use `<StackPane styleClass="role-tile role-tile-doctor"><Region styleClass="role-icon role-icon-lg role-icon-doctor"/></StackPane>`
  anywhere (sizes: `role-icon-sm`, default, `role-icon-lg`; tile: `role-tile-sm`, default). Colours match the sign-in loading screen.
- Controller ids/handlers are unchanged (`patientButton`, `handlePatient`, ...), only `HomeController` gained section scrolling.
- **FXML tip (important):** several style classes on one node must be separated by commas in FXML
  (`styleClass="role-card,role-card-patient"`). A space makes JavaFX read it as ONE class name and none of the CSS applies.


## Role usefulness update (v3)
What changed, per role:
- **Patient**: can now **cancel their own appointment** (Appointments page, "Cancel Appointment" button). Allowed while it is PENDING, DOCTOR_APPROVED or CONFIRMED and the date has not passed. The slot returns to the doctor's calendar and the doctor (and secretaries, if it was already approved) get a notification. Before this, a patient had no way to back out of a booking. Cancelled and completed bookings no longer show as the "next appointment" on the dashboard.
- **Doctor**: a CONFIRMED appointment can be closed with **Mark Completed** (the Approve button changes its label when you select a confirmed row). Allowed on or after the appointment date, only for your own patients; the patient is notified. Appointments now have a full life cycle: PENDING -> DOCTOR_APPROVED -> CONFIRMED -> COMPLETED (or REJECTED / CANCELLED).
- **Nurse**: the sidebar had two buttons ("Medical Records" and "Vital Signs") that opened the same page. They are now one button, **Records & Vitals**. The dashboard "Manage Vital Signs" card still opens it.
- **Consultations (all roles)**: the rules now live in `ConsultationService`, not only in the screen: blank fields are refused, a patient cannot have two open requests with the same doctor on the same day (the form shows a clear message), and a request can only be approved or declined while it is PENDING.
- Secretary and Admin pages were left as they are; see the review notes in the chat for suggestions.

Verified: all 5 modified/new service rules were run in a small console test (17 checks, all passed); every FXML file parses. JavaFX screens were not launched here, so please run once and try: patient cancel, doctor Mark Completed, nurse sidebar.


## Nurse, Secretary and Admin update (v4)
- **Nurse**: new sidebar button **Today's Visits**. It lists confirmed appointments, earliest first, with a **Pre-visit Vitals** column and a **Record Vitals** button (validated form, same ranges as the records page). The doctor is notified when vitals are ready and also sees them in a Pre-visit Vitals column on the Appointments page. When the doctor completes a consultation, vitals already taken for that patient/doctor/day are copied onto the medical record, so the nurse does not take them twice. The nurse dashboard now leads with "Next visit: ... N visit(s) today need vitals" (a FIFO queue by appointment time), then falls back to records waiting for vitals. Vitals can be recorded for any CONFIRMED appointment (for an easy demo you do not need to wait for the day itself); the dashboard queue only counts today's.
- **Secretary**: the Appointments page lists **requests waiting for confirmation first**, then by date and time ("09:00 AM" before "01:00 PM"). The Reject button became **Reject / Cancel**: on a confirmed, upcoming booking it cancels it (slot freed, patient and doctor notified), for example when the doctor becomes unavailable.
- **Admin**: deleting or deactivating a **doctor** now cancels that doctor's upcoming pending/approved/confirmed appointments and notifies each patient, so nobody is left waiting for a doctor who can no longer sign in. Deleting any user also removes their notification inbox.
- Appointment tables for doctor, nurse, secretary and admin are sorted by date then clock time.
- New files: `VitalsDialog.java`. Changed: `Appointment` (vitals), `VitalSigns.summary()`, `AppointmentService`, `UserService`, `AppointmentController`, `ConsultationController`, `NurseDashboardController`, `nurse-dashboard.fxml`, `nurse-appointments.fxml`, `doctor-appointments.fxml`.
- Verified with a console test (service layer, 17 + 17 checks) and an FXML-handler cross-check. JavaFX screens were not launched here: please try the nurse "Today's Visits" page, the secretary "Reject / Cancel" button and deleting a doctor as admin.


## Final polish (v5)
- **Doctor decision support**: on the doctor's Appointments page, selecting a request shows the patient's history with that doctor under the table: completed visits, cancellations, last diagnosis and latest vitals ("First visit with you" if none). Other doctors' records are never shown. (`AppointmentService.historySummary`)
- **Cleanup**: removed 10 generic pages and stylesheets that no role could ever reach (`appointments`, `consultation`, `dashboard`, `doctors`, `medical-records`, `patients`, `profile`, `requests`, `schedule`, `verification-requests` - `.fxml` and `.css`). Navigation always uses the role-prefixed pages. A reference check confirms nothing points at the removed files.
- Checks run: 3 console test suites (service layer, all passing), FXML parse check, FXML handler / controller cross-check, missing-file check.


## UI polish (v7)
- New shared stylesheet `ui-polish.css`, attached to every page by `OverlayHost.setPage`, so all five roles get the same: dimmed disabled buttons, press feedback, hover on secondary/danger buttons, slim rounded scroll bars and styled tooltips.
- Appointment tables show a readable, colour-coded status (green confirmed/completed, blue doctor approved, amber pending, red rejected/cancelled) instead of raw text like DOCTOR_APPROVED.
- Register page role cards reworded to describe what each role really does.


## SRS nurse review + patient-reported vitals (v8)  - supersedes the nurse notes above
Why: the SRS (F-3.5, workflow step 7) puts a **nurse review** between the patient and the doctor, and in a teleconsultation the patient is not next to a nurse, so the patient reports their own vitals.
- **Patient**: the booking form has an optional "Your vitals (from a home device)" block: temperature, pulse, blood pressure (e.g. 120/80), breaths per minute. Leave all four blank or fill all four; values are range-checked.
- **Nurse** ("Review Requests"): new requests are listed first. She can **Update Vitals** (check or enter them herself, e.g. after phoning a patient without a device), **Forward to Doctor** (only when vitals are present) or **Return to Patient** with a reason (slot freed, patient notified). Nurses are notified when a request arrives.
- **Doctor**: only sees requests the nurse forwarded (status `FOR_DOCTOR_REVIEW`, shown as "For doctor review"); the Patient Vitals column shows "(self-reported)" or "nurse-checked". Approving or rejecting a request still waiting for the nurse is refused with a clear message.
- **Statuses** now: PENDING -> FOR_DOCTOR_REVIEW -> DOCTOR_APPROVED ("Accepted") -> CONFIRMED -> COMPLETED, or REJECTED / CANCELLED.
- Vitals still carry over to the medical record when the doctor completes a consultation.
- Checks run: 36 flow checks covering the whole chain, plus the earlier suites; FXML parse and handler cross-check. The controllers could not be compiled here (no JavaFX); their only errors are the missing JavaFX classes. Please run the app and test: patient books with vitals -> nurse forwards -> doctor approves -> secretary confirms; also nurse Return, and nurse Update Vitals.
- Still open: account verification by the Secretary (SRS says Admin only), and the SRS statuses "Suspended" / "Schedule Conflict".

## New register page (v9)
- `register.fxml` / `register.css` were rebuilt in the style of the "Choose Your Role" mockup: soft corner glows, a glassy gradient card per role (Doctor, Patient, Nurse, Secretary) with that role's character, a coloured Register pill, a heart logo, a tagline and an ECG-line footer.
- The characters and heart logo are cut out of the mockup picture with feathered edges (`register-doctor.png`, `register-patient.png`, `register-nurse.png`, `register-secretary.png`, `register-logo.png`) so they blend into the cards. All text and buttons are real JavaFX controls, so they can be edited.
- Every button keeps its original `fx:id` and handler (`handleDoctor`, `handlePatient`, `handleNurse`, `handleSecretary`, `handleLogin`, `handleHome`), so navigation did not change.
- Cards sit in a `FlowPane`: four in a row on a wide window, wrapping to two rows on a narrower one.

## Vital signs display
- The Vital Signs box on every role's Medical Records page is now four read-only tiles (Temperature, Heart rate, Blood pressure, Respiratory rate) instead of a small scrolling text area, so nothing is cut off or highlighted. When no record is selected, or a visit has no vitals, a short message is shown instead.

## Vital signs, round 2
- Tile colors fixed (each tile now uses a single style class, so the colors always apply). Each tile also shows a small status chip ("Within / Above / Below typical range", typical adult ranges only, with a reminder that the doctor interprets results).
- "Record Vital Signs" moved up into the Vital Signs header (nurses only). Patients, doctors, admin and secretary can only view.
- Vitals now remember who recorded them and when ("Recorded by <nurse> · <date>"), shown under the tiles.
- The vital-signs dialog uses the app's look (rounded fields, blue Save button, example values as hints). The selected table row is now blue instead of green.

## Registration forms match the "Choose Your Role" page (v3 of the VitalSigns build)
- The four forms (`patient-`, `doctor-`, `nurse-`, `secretary-register.fxml`) now share one look with the role chooser: the heart logo with "Tele**Consult**" in the header, the same soft glow background, a glass form card tinted in the role's colour with that role's character at the top (the same artwork as the role card), role-coloured Register button and badge, and the ECG-line footer.
- All of it comes from one new stylesheet, `register-theme.css`, loaded last in each form, plus small header/icon/footer edits in the four FXML files. Fields, `fx:id`s and handlers are unchanged, so registration works exactly as before.

## One look for the whole app (VitalSigns v4)
Every screen now shares the "Choose Your Role" design.
- **Logo**: the heart logo with "Tele**Consult**" (navy + blue) and the spaced "M E D I C A L  S Y S T E M" replaces the old "+" tile on the homepage, login, admin login and all five dashboard sidebars.
- **`app-glass.css`** (new, applied by `OverlayHost.setPage` through the `tc-app` class, so it reaches every page): glow background, the original navy sidebar (kept) with rounder menu pills, glass cards, pill buttons (primary / secondary / danger), rounded inputs and tinted table headers. The sidebar colours (dashboards and homepage) come from the original theme files untouched.
- **Login** now has the glass card with a banner of all four role characters; **admin login** has the glass card with the heart logo. Both get the ECG footer.
- Registration forms still use `register-theme.css` (role-coloured). Nothing in the logic changed: all `fx:id`s and handlers are identical in all 50 FXML files.
- Not yet visually checked in the running app (JavaFX could not be launched where this was built). If a colour looks off, the rules to tweak are all in `app-glass.css` and `register-theme.css`.

### Navy sidebar kept (VitalSigns v5)
The sidebar stays the original dark navy on the dashboards and the homepage. Only the new heart logo (now with a transparent background so it sits cleanly on navy), the white "Tele" + light-blue "Consult" wordmark and the rounder menu pills were added. Everything else from v4 is unchanged.

## Notification sounds
- Every pop-up notification (and the "Welcome back" summary after sign-in) now plays a short chime. The sounds are generated in code (`NotificationSound`), so there are no audio files or new libraries. Each type has its own pattern: information (soft two-note ding), success (rising three notes), warning (double beep), alert (three-note call).
- A "Sound: On/Off" button on the Notifications page mutes or unmutes them (it also plays a short preview when turned on). The setting resets when the app closes.
- If the computer has no sound device, nothing is played and nothing breaks.
- `module-info.java` gained one line, `requires java.desktop;`, because Java's sound API (`javax.sound.sampled`) lives in that module.

## Easier vitals for patients (booking page)
- The home-vitals form on the patient booking page now has a visible label and unit above every box (Temperature °C, Pulse bpm, Blood pressure top / bottom mmHg, Breathing per min). Before, the hints disappeared as soon as you typed, so you could not tell which box was which.
- Blood pressure is two boxes ("120" / "80"). Typing "/" or a space jumps to the second box, and pasting "120/80" fills both.
- Boxes accept digits only (and one decimal point for temperature), so "must be numbers" errors can no longer happen.
- Live feedback: a box turns green when the value is realistic and red when it is not (red only after you leave the box, so typing "3" on the way to "36.5" is never flagged). One status line says what to do next: "Nothing entered...", "2 of 4 filled in...", the exact problem, or "All set".
- A temperature typed in Fahrenheit (for example 98.6) is detected and converted to °C automatically, with a note.
- A "Clear" button empties the form, and it is clear that the section is optional: a nurse will take the vitals if the patient leaves them empty.
- The limits are the same as `VitalSigns.check`, so anything shown green is accepted when booking. The checking logic is in `VitalsInput` (no JavaFX inside, easy to test).

## Build fix

- `AppointmentController` used `Platform.runLater(...)` in the vitals input code without importing `javafx.application.Platform`, which stopped the build ("cannot find symbol variable Platform"). The import was added. All Java files were checked for the same kind of missing import: none left.

## Enter to sign in, forgot / change password, Esc to log out (latest)
- **Enter signs in.** On the login and admin login pages, pressing Enter in the email or password box signs in
  (in the email box it first jumps to an empty password box). It is ignored while the loading screen is running, so
  it can never double-submit.
- **Forgot password** (new page `forgot-password.fxml`, link "Forgot password?" on the login page). There is no
  e-mail or database, so identity is checked with details from registration: role + email + full name + one more
  detail (patient: date of birth, doctor / nurse: license number, secretary: department). Wrong guesses are limited
  (5 tries, then a 1-minute wait, same as sign-in). Unknown email and wrong details give the same message. A reset also
  clears a sign-in lockout and leaves a "Password reset" warning in the account's Notifications. Admin accounts are
  not reset here.
- **Change password** (new card, button "Change Password" in Profile > Account Information, all 5 roles). Needs the
  current password, a strong new one (same rule as registration) and a confirmation. A "Password changed" pop-up
  appears afterwards.
- **Esc = "Log out?"**. On any dashboard, Esc opens a Yes / No card (focus starts on "No"; Esc again = No). If another
  pop-up card is open (e.g. Change Password) Esc closes that instead. Esc no longer leaves full screen; F11 still does.
- New files: `forgot-password.fxml`, `logout-confirm.fxml`, `change-password.fxml`, `message-popup.fxml`;
  `ForgotPasswordController`, `ChangePasswordController(+Dialog)`, `LogoutConfirmController`, `LogoutConfirm`,
  `MessagePopupController(+MessagePopup)`; services `PasswordService`, `PasswordPolicy`. Styles were added to
  `popup-theme.css` and `login.css` (nothing existing was changed).

Checked: the password logic has 42 automated checks (policy, change, reset for every role, lockouts); the new
classes compile against JavaFX stand-ins; every new FXML was checked against its controller. Not run on screen
(no JavaFX in the build sandbox), so please look at the four new screens once.


## Senior / PWD / Regular patient types (priority review)
- **Three types.** *Senior* is calculated from the date of birth (60 or above, `PatientType.SENIOR_AGE`), *PWD* is declared by the patient together with a PWD ID number, everyone else is *Regular*. A senior who is also a PWD is *Senior + PWD*. The rule lives in `Patient.getPatientType()`, the values in the new enum `PatientType`.
- **Where the patient sets it.** Registration has a "Patient Type" block (PWD checkbox + ID box that appears when ticked). Profile > Patient Type shows the calculated type and lets the patient tick/untick PWD and edit the ID. A PWD needs an ID number (4 to 30 letters, numbers, dashes).
- **Booking page.** The patient sees "Your patient type: Senior (age 68) - priority review" before booking. The type is **saved on the appointment** when it is requested, so later profile edits never rewrite old bookings.
- **Priority review.** Senior and PWD requests are reviewed first, then first come first served. The nurse's review queue and the doctor's waiting list are now `PriorityQueue`s (`AppointmentService.PRIORITY_THEN_ARRIVAL`). The nurse, doctor and secretary appointment tables list priority requests first inside the group that needs their action. The nurse dashboard and doctor dashboard show a `[Senior]` / `[PWD]` tag on the next request.
- **Visible to staff.** A new **Type** column on every appointments table (PWD rows also show the ID so the nurse can check it). The nurse gets a warning-style notice "Priority request to review", the doctor "Priority request ready for your review". The patient list shows the type of priority patients.
- **Demo accounts.** `senior@teleconsult.com` and `pwd@teleconsult.com` (see the list at the top).
- **Tests.** `src/test/java/ph/edu/teleconsultation/system/portal/PriorityChecks.java` is a plain Java program (no JUnit, no extra library): run its `main` and it prints 44 PASS lines (type rules, age edge cases, queue order, table order, notifications, full workflow with a priority patient). The 44 checks pass.
- New / changed files: new `PatientType.java`, `PriorityChecks.java`; changed `Patient`, `Appointment`, `AppointmentService`, `AppointmentController`, `PatientRegistrationController`, `ProfileController`, `PatientController`, `NurseDashboardController`, `DoctorDashboardController`, `MainApp`, `patient-register.fxml`, `patient-profile.fxml`, the five `*-appointments.fxml`, `ui-polish.css`.
- Not run on screen (no JavaFX where this was built). The logic layer was compiled and tested; the changed controllers were compile-checked (no errors other than the missing JavaFX classes) and every FXML was parsed. Please look at: registration PWD box, Profile > Patient Type, the booking page label, and the Type column.

## Sound timing, yellow bell and profile customization

- **Sound on time**: every chime is built and its audio line opened once at start-up (`NotificationSound.warmUp()`), so playing is just "rewind and start". The pop-up layout is also pre-loaded. The chime now starts in the same instant the pop-up begins to slide in (it used to start before the pop-up had loaded).
- **Yellow bell**: the sliding notification pop-up shows a yellow bell that bounces in, swings from its top and rings with sound waves, then rings again every few seconds while the pop-up is visible. A small badge on its corner keeps the type (i / check / !). (`notification-toast.fxml`, `popup-theme.css`, `ToastController`)
- **Customize profile** (patient, doctor, nurse, secretary): a new "Customize Profile" card on each profile page lets you choose a **profile photo** (PNG/JPG/GIF up to 5 MB, centre-cropped to a circle), one of 8 **colour themes** for the header and avatar, and a short **About me** line (140 characters). The header previews changes live; **Save Customization** keeps them and **Reset to Default** restores the original look. Patients also see a doctor's photo, colour and About me on the doctor's profile page. Stored in memory like the rest of the data. (`ProfileTheme`, `ProfileStyle`, `profile-customize.css`)


## Notification sound fix
- The chime sometimes did not play at all. Cause: each chime used its own idle audio Clip, and many Windows sound devices
  put an idle line to sleep, swallowing the short chime while they wake up; one failed start-up also turned sound off until
  the app was restarted.
- `NotificationSound` now keeps one audio line open and streaming silence, and mixes the chime in when a pop-up appears, so
  it starts within about 40 ms. If the device is missing or busy it retries every 3 seconds instead of giving up. Volume
  raised from 0.30 to 0.40. The public methods (`warmUp`, `play`, `isEnabled`, `setEnabled`) are unchanged.

## Doctor names always start with "Dr."
- A doctor can now register with just the plain name; the system stores it as "Dr. Name" (typing "Dr." or "Dr" first is
  fine, it is never doubled). Because the title is part of the stored full name, it shows the same everywhere: doctor
  list, booking, appointments, notifications, medical records, verification requests and the profile.
- The same rule applies when a doctor edits their name in Profile. The duplicate-name check uses the final "Dr. Name".
- Changed: `Doctor.withTitle()` (new, used by the Doctor constructor), `DoctorRegistrationController`,
  `ProfileController`, and the hint on `doctor-register.fxml`. The demo doctors already had "Dr.".

## License number and PWD ID: examples and rules
- **License number (doctor, nurse)** was already required, but anything was accepted (for example "234242424" or "abc"). The text
  boxes now show an example ("e.g. MD-2026-001" / "e.g. RN-2026-001") and registration checks the shape with the new
  `LicenseRules`: 4 to 20 characters, letters, numbers and dashes, no spaces, at least one digit. The "MD-" prefix is NOT
  required, so a plain 7-digit license number still works.
- **PWD ID** is required whenever the PWD box is ticked (registration and Profile). The text boxes now show "e.g.
  PWD-2026-0001" and the error message repeats the example. Formats differ by city, so only the basic shape is checked.

## Vitals check-in (clinic visit before the appointment)
Problem: a patient with no thermometer or BP monitor could request an appointment without vitals, and the only
fallback was "a nurse will call you". Now the nurse can ask the patient to come to the clinic first.

How it works
- Patient: leaves the vitals empty (still optional). The form and the success message now explain that a nurse may
  ask them to come in. The table has a new **Vitals Check-in** column.
- Nurse: on a new request with no vitals, select it and press **Vitals Visit**, then pick a day and time
  (08:00 AM to 04:30 PM, every 30 minutes) and where. Only times that are in the future and at least 60 minutes
  before the appointment are offered, so there is still time to forward the request.
- The patient gets a notification with the day, time and place. The doctor's time slot stays reserved.
- When the patient arrives the nurse uses **Update Vitals**; the patient is thanked, the column shows "done", and the
  nurse can **Forward to Doctor** as before.
- If the patient does not come, the column shows "not done yet". The nurse can press **Vitals Visit** again to move
  it (the patient is told it moved), phone the patient and use **Update Vitals**, or **Return to Patient**.
- Patients with a home device are unchanged: they type their vitals and the nurse can forward right away.

Files: `Appointment` (3 new fields), `AppointmentService` (rules in the "vitals check-in" section),
`VitalsVisitDialog` + `VitalsVisitController` + `vitals-visit.fxml` (new in-app card; styles are at the end of `popup-theme.css`), `AppointmentController`, `nurse-appointments.fxml`, `patient-appointments.fxml`,
test `VitalsVisitChecks.java` (39 checks; run its main method like `PriorityChecks`).

## Vitals counter wording

- The patient booking form said "2 of 4 filled in" while showing **5** boxes (blood pressure has a top and a bottom box and counted as one reading). It now counts the boxes you can see ("3 of 5 filled in") and names what is still missing ("Still missing: blood pressure (bottom number), breathing"). The rule for when the set counts as complete is unchanged. (`VitalsInput.filledBoxes` / `VitalsInput.missing`)
