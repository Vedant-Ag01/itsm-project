# I-ITSM: Requirements Specification

Scope: team system requirements, with D-owned UI responsibilities identified. Requirements describe acceptance targets; they do not assert that pending backend modules are already integrated. Source: supplied project brief, Major Users, MVP, Security, Exception Handling and Deliverables sections.

## Roles and authorization

| Role | Permitted responsibility | Data boundary |
| --- | --- | --- |
| Visitor | Register as an employee and sign in | No incident access |
| Employee | Create/view own incidents, provide information, verify resolution, give feedback | Own reported incidents only |
| Technician | View assigned incidents, acknowledge, investigate, add notes, request information and resolve | Incidents assigned to that technician |
| Administrator | Manage users/services/policies/assignment, monitor incidents, close verified incidents and access reports/audit | Organization-wide authorized data |

The supplied backend also permits administrators to perform some technician and verification actions. This administrative override must remain explicit and auditable. The frontend's employee and technician routes do not expose administrative actions.

## Functional requirements

| ID | Requirement and acceptance criterion | Ownership |
| --- | --- | --- |
| FR-01 | Register an employee with validated identity fields and password; reject duplicate email; never accept a client-selected elevated role. | D UI; A API |
| FR-02 | Authenticate, restore a valid session after refresh and invalidate it on sign-out; rejected credentials must not grant access. | Shared UI/A |
| FR-03 | Route authenticated users to their role workspace and reject unauthorized server operations, including guessed incident IDs. | Shared UI/A |
| FR-04 | Report an incident with title, description, service, impact, urgency and positive whole-number affected-user count; show the created ID. | D UI/A |
| FR-05 | List and open the reporter's incidents, with accurate status, priority, service, assignment and available timestamps. | D |
| FR-06 | Allow authorized updates under documented lifecycle rules and preserve form input on rejected updates. | D UI/A API; contract pending |
| FR-07 | Accept additional information as notes and display author and timestamp. Support requests must be visible and answerable using the agreed request-info contract. | D/B UI; A API |
| FR-08 | Display resolution details; allow the reporter to verify only a resolved incident and submit feedback after resolution. | D UI/A |
| FR-09 | Assign using at least two interchangeable strategies and expose unavailability or workload errors. | B/A |
| FR-10 | Technician can acknowledge assigned work, start investigation, add notes and resolve with a nonempty resolution note. | B/shared technician UI |
| FR-11 | Calculate response/resolution deadlines and expose SLA status, warnings and violations. | B/A |
| FR-12 | Notify relevant users through at least two mechanisms, simulated or real, without representing failed delivery as successful. | B/C/A |
| FR-13 | Record important incident actions with actor, timestamp, action, ID and status transition. | C/A |
| FR-14 | Support at least five meaningful search/filter criteria and useful incident, workload and service reports. | C/A |
| FR-15 | Manage users, technicians, services, SLA policies and assignment configuration under administrator authorization. | A |
| FR-16 | Implement at least two intelligent/automated features with explainable results and safe handling of missing data. | B/C/A |
| FR-17 | Persist core users and incidents, including notes, resolution and feedback; integrate persistence for services, policies and audit as required by the brief. | A/C |
| FR-18 | Provide loading, empty, error and retry states; prevent repeated requests while a submission is pending. | D/shared frontend |
| FR-19 | Switch among appearance presets without losing data or form input; retain the preference locally. | D |

## Non-functional requirements

| ID | Quality target | Verification |
| --- | --- | --- |
| NFR-01 | Authorization on the backend for every protected operation; no reliance on navigation visibility. | Cross-role and cross-reporter API tests |
| NFR-02 | Passwords never stored as plaintext or included in logs/client persistence. Use HTTPS outside the localhost demo. | Code/configuration review |
| NFR-03 | Explicit field labels, keyboard operation, visible focus, semantic controls and readable contrast. | Keyboard walkthrough and contrast review in each theme |
| NFR-04 | Layout usable on desktop and narrow mobile viewports; tables may scroll within their container. | Manual browser checks at 1366px and 390px widths |
| NFR-05 | Failed requests preserve user input and do not display a false success; offer a recovery action. | Offline/HTTP error injection |
| NFR-06 | Keep domain rules behind interfaces and out of UI components; isolate API mappings. | Review Strategy, State, Repository and API adapter boundaries |
| NFR-07 | Persist successful updates and define which data are ephemeral. | Refresh, server-restart and database checks |
| NFR-08 | Keep secrets in local environment/configuration; commit no production credentials. | Repository review before PR |
| NFR-09 | Make performance claims only after measuring; introduce paging/indexes before loading large incident histories in every browser. | Record workload size and measured latency in future load testing |
| NFR-10 | Reproducible setup through lockfile, Maven configuration, documented commands and deterministic tests. | Clean environment setup and test runs |

The supplied salted SHA-256 password hashing is a prototype limitation: it lacks the work factor of a dedicated password KDF. The report does not describe it as production-grade. In-memory session tokens also disappear on server restart and are not JWTs.

## Business rules

1. The authenticated identity determines the reporter. Public employee registration must not permit a supplied ADMIN or TECHNICIAN role.
2. Incident access is scoped to the reporter, assigned technician or authorized administrator.
3. Required text must contain non-whitespace characters. Affected users must be a positive integer; invalid data must also be rejected server-side.
4. Priority is computed by the domain strategy. The browser must not authoritatively choose the result.
5. Service criticality belongs to service configuration in the planned persisted-service model. The current report form follows the supplied legacy API until the replacement contract is confirmed.
6. Assignment considers the selected strategy and technician eligibility; no suitable technician is a handled error, not a fabricated assignment.
7. Only valid state transitions may occur. NEW -> CLOSED must be rejected. Resolve requires an appropriate investigation state and a resolution note.
8. Employee verification is allowed for a RESOLVED incident belonging to that employee. Closure is an administrator operation.
9. Feedback rating is an integer from 1 through 5 and is accepted only after resolution. The UI preserves an existing feedback value.
10. Additional notes retain author/time. The current UI disables new notes on CLOSED incidents; backend policy remains authoritative.
11. Request-info is a separate interaction, not an invented lifecycle status named "Awaiting reply". Its field shape and completion rules require the new API contract.
12. Duplicate suggestions do not silently discard the newly reported incident or imply permission to read another employee's incident.
13. Dashboard counts and trends are derived from authorized records. Missing resolution dates must not become invented chart points.
14. Audit entries and notification deliveries must not be inferred from a visual timeline. The current frontend explicitly distinguishes incident-derived updates from a real inbox.
15. Completed incidents must not remain in active-workload calculations. RESOLVED, VERIFIED and CLOSED form the completed set used by the UI.
16. Registration currently uses frontend limits of name/department 100, email 254 and password 8-128 characters; these are proposed UI rules awaiting backend confirmation, not requirements invented from the PDF.

## Use-case specifications

### UC-01: Register employee
Actor: visitor. Precondition: registration API deployed and contract confirmed. Enter name, email, optional department, password and confirmation; submit; server creates an employee; sign in separately. Invalid fields remain editable, duplicate email offers sign-in, and network/server errors preserve input. Success is displayed only after a successful server response.

### UC-02: Report and track incident
Actor: employee. Precondition: authenticated employee and available service configuration. Enter incident information; submit once; see created ID and possible duplicates; open details; observe current status and assignment. On validation failure, correct the relevant data. On unavailable server, retain input and retry after connectivity returns.

### UC-03: Provide additional information
Actor: reporter. Open an accessible incident; read technician notes or a supported request-info item; add an answer; see the saved note and timestamp. On failure keep the answer. Do not claim a request is marked answered until the new server contract confirms it.

### UC-04: Verify and give feedback
Actor: reporter. Precondition: own incident RESOLVED. Read resolution, test the service, confirm that the fix works, and provide rating/comment. The incident becomes VERIFIED; closure remains with the administrator. Failed verification must leave the displayed incident unchanged until a successful refresh/action response.

### UC-05: Investigate and resolve
Actor: assigned technician. Acknowledge assigned incident, start work, add notes, request information where supported, and submit a resolution. Invalid transitions or access to another technician's assignment are rejected by the server. Reporter can then verify.

## D traceability

| Deliverable | Evidence |
| --- | --- |
| Registration | frontend/src/pages/Register.jsx; validation/registration.js; api/registration.js |
| Employee reporting | LiveReportIncident.jsx and ReportIncident.jsx |
| Tracking and follow-up | MyIncidents.jsx; IncidentDetails.jsx; EmployeeOverview.jsx |
| Appearance/navigation | App.jsx; WorkspaceLayout.jsx; ThemeToggle.jsx; design-polish.css |
| Registration verification | tests/registration.test.mjs and check-registration.mjs |
| Report/diagram/demo | docs/person-d/01 through 05, use-case-diagram.svg and combined PDF |

See INTEGRATION.md for the exact boundary between completed frontend work and blocked live acceptance.
