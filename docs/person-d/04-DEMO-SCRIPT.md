# I-ITSM: Team Demo Script

Target length: 18 minutes within the PDF's 15-20 minute requirement. Every member introduces themselves and explains their module. The three workflows below follow the PDF's examples; the brief requires at least three complete workflows, not these exact incident IDs or wording.

## Before recording

1. Merge the reviewed team changes into a tested integration branch. Resolve the known Java compilation failures first. Run Java tests and frontend checks; record actual results rather than reading this script as evidence.
2. Start PostgreSQL, the Spring backend and Vite in live mode. Verify employee, technician and administrator credentials locally without showing passwords or tokens in the recording.
3. Confirm registration, service catalog, request-info, SLA, notification, audit, duplicate/correlation and report endpoints in docs/api-reference.md. Enable registration only after the adapter matches it. The supplied archive cannot yet demonstrate these integrations.
4. Seed a clearly named Demo payment service and appropriate technician skills/availability. Use synthetic descriptions with no personal data. Prepare another employee account for authorization checks.
5. Agree who controls each role window. Record the real incident IDs as they are created. Do not reuse guessed IDs such as INC1002.
6. Rehearse the full flow once, including database persistence and each module handoff. Turn off unrelated notifications and hide developer credentials.
7. Keep architecture/class/ER diagrams and B's completed test-case document ready. D's report sections are contributions to the required one group PDF, not a substitute for the other deliverables.

## Recording timeline

| Time | Speaker/action | What the evaluator should see |
| --- | --- | --- |
| 00:00-01:30 | All members introduce name and responsibility | Every member appears/speaks; concise problem statement |
| 01:30-03:00 | A explains architecture; D shows role workspaces | Browser/API/domain/database separation and role navigation |
| 03:00-06:30 | Workflow 1, D then A/B/C | Real creation, computed priority, assignment and notification |
| 06:30-11:30 | Workflow 2, B then D then A | Investigation, SLA, notes, resolution, verification and closure |
| 11:30-15:00 | Workflow 3, C then B/A | Duplicate/correlation output and escalation or risk evidence |
| 15:00-17:00 | Tests, security and OOP explanation by owners | One invalid transition/access rejection and pattern code |
| 17:00-18:00 | D summarizes UX; A states limitations/conclusion | Honest boundary of implemented features |

## Workflow 1: Report -> prioritize -> assign -> notify

D says: "I am using an employee account. I can report and track my own incidents, while assignment and closure belong to other roles."

If the registration API is integrated, create a new employee using the registration screen, show a validation error, correct it, submit and sign in. Otherwise do not portray the gated screen as a working registration backend; resolve the blocker before the final demonstration.

Report title: "Demo payment service unavailable". Description: "Since 10:00, checkout fails for the finance team; transactions cannot be completed." Select the configured service, high impact, high urgency and an appropriate affected-user count. Submit once. Show the server-returned ID and priority. A/B explains which active priority strategy produced the value and the service's criticality; do not preannounce P1 unless the response confirms it.

A assigns the incident through the integrated admin UI or authorized API. B explains skill/load eligibility and names the actual assigned technician. C shows the generated notification through a real in-app response or clearly labeled simulated channel output. The employee opens the incident to see its status and assignment. End state: created/classified and assigned, with notification evidence.

## Workflow 2: Investigate -> monitor SLA -> resolve -> verify -> close

B signs in as the assigned technician, opens the same ID, acknowledges it and starts work. Show that the available action changes with the incident state. Add an investigation note: "Checked payment service logs; connection pool exhausted." Refresh to demonstrate the note persists.

Show response/resolution deadlines and SLA/risk information from the integrated backend, explaining the clock and applicable policy. Do not label an absent value as "on track". If request-info is integrated, ask "Does the error affect all payment methods?"; D returns as the reporter, reads the request and answers with additional information. Show the backend-defined answered state if available.

B resolves with "Restarted the affected connection pool and verified successful test transactions." C shows the resolution notification. D reads the resolution, confirms that the issue is fixed, clicks verification and submits a rating/comment. A closes the VERIFIED incident with a closure note. Display the audit trail from the audit module and refresh the employee details. End state: CLOSED with saved resolution, feedback and history.

D says: "Verification confirms the fix from the reporter's perspective. It does not bypass the administrator's closure step. The browser displays the server's accepted state."

## Workflow 3: Related incidents -> correlation -> escalation/risk

Using authorized demo accounts, create three distinct open reports for the same configured service within 30 minutes, with related descriptions such as payment checkout failure, payment timeout and payment confirmation unavailable. Keep them open; already completed incidents do not satisfy the planned correlation rule.

C shows the duplicate suggestions and explains the matching evidence. Demonstrate that a new report is still created and another user's private details are not exposed. Show the correlation group containing the real IDs, service name and first-reported time. C explains that a possible outage is a heuristic grouping, not proof of root cause.

B/A demonstrates a risk warning or escalation using a documented demo fixture or controlled test clock. Never secretly change system time or claim that a simulated timer is real elapsed time. If demonstrating escalation, show the actual escalation event/recipient, not just a colored badge. End by showing the resulting report/service analysis and audit evidence.

## OOP and negative-case demonstration

- B opens AssignmentStrategy and two implementations; show how wiring selects one without editing the employee form.
- A opens an IncidentState implementation; demonstrate that NEW -> CLOSED is rejected.
- C opens IncidentObserver and explains notification/audit subscribers, showing that these are wired in the running build.
- D attempts to open a different employee's incident or an administrative route. Distinguish frontend redirection from the server's 403 rejection.
- Show B's test-case table with actual execution outcomes, including failures fixed and remaining limitations. D's adapter tests use intercepted responses and must be labeled as such.

## D's ready-to-speak introduction

"Hi, I am Naman Yerra. My responsibility is the shared frontend, the employee workspace and our assigned documentation. I built the role-aware navigation, appearance controls, incident reporting and tracking screens, and the follow-up flow for notes, resolution verification and feedback. I also contributed the technician interface. My registration screen validates input, prevents repeated submission and connects through a separate API adapter once the backend contract is confirmed."

After registration is integrated and tested, replace the last sentence with: "The registration screen validates input and creates an employee through our backend API." Do not use that sentence before the real test passes.

## D's 60-second walkthrough

"An employee reports the service problem here. Required fields are labeled, and failed requests retain what was entered. The list and dashboard show only the incidents returned for this user. Opening an incident shows the current status, assigned technician, notes and available timestamps. Once support resolves it, the reporter can verify the fix and leave feedback. Shared components keep these interactions consistent across pages and themes. API adapters translate backend data into the shapes used by the UI. The backend remains responsible for ownership and lifecycle permissions; hiding a button is only a usability measure."

## Submission checks

The PDF specifies one group report and one video, with a final upload deadline of 22 November 2026. The teammate's D/PR deadline is 19 November, 6 PM. The PDF's sample video filename uses a 2025-2026 suffix despite its 2026-2027 heading; confirm the required naming convention with the team/instructor instead of silently changing it. Ensure all members introduce themselves and the upload opens successfully before submission.
