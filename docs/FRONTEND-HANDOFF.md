# Employee and technician frontend handoff

Scope: React frontend and this document only. No Java, database configuration or admin screen changes.

## Included
- Live employee notes (separate GET/POST /incidents/{id}/notes endpoint) and feedback.
- Preserve impact, urgency, feedback and resolution timestamps from IncidentResponse.
- Technician assigned list with search/status/priority filters and refresh.
- Technician detail view: ASSIGNED -> acknowledge, ACKNOWLEDGED -> start-work,
  IN_PROGRESS -> resolve with required note; prevent duplicate submissions.
- Shared notes, incident details, resolution display and recorded timestamp milestones.
- Technician workload counts and priority chart from the role-scoped incident list.
- Incident-derived updates for employees and technicians with browser-local read state.
- Existing warm light/dark themes and responsive styles retained.

## Install into the existing repository
Stop the old frontend server. Extract this package, then copy its frontend and docs
folders into the ORIGINAL itsm-project repository, merging folders.
Only modified files are included; do not delete other files or replace the whole repository.
Do not use the older itsm-project-updated backend.

From frontend:
    npm ci
    npm run lint
    npm run build
    node check-api.mjs
    VITE_USE_MOCKS=false VITE_API_BASE_URL=http://localhost:8080/api npm run dev -- --port 5173 --strictPort

Run the current merged Spring Boot backend and your existing PostgreSQL database separately.
Keep your working database environment variables and credentials.
The technician workflow requires live mode; the existing mock login supports only the employee.

## Local acceptance test
Demo users, if seeded in your database:
asha@bits.edu (employee), ravi@bits.edu and meera@bits.edu (technicians).
Seed password: password123. Existing database credentials may differ.

1. Employee reports an incident.
2. Admin assigns it via existing backend tooling (admin UI is outside this delivery).
3. Sign in as the technician actually assigned to the incident.
4. Search/filter the list; open it; acknowledge; start work.
5. Add a note; refresh and confirm persistence.
6. Resolve with a nonempty resolution note.
7. Employee opens the incident, reads the note, verifies the fix, submits feedback.
8. Refresh and confirm feedback, dashboard resolution count and graph.
9. Check workload and incident-derived updates; switch themes.
10. Test an inaccessible ID and stopped backend: show errors, retry, preserve form text.

## API gaps for backend owners
- No notifications or audit controller in the supplied backend. Updates are explicitly
  incident-derived, and milestones are not presented as an audit trail.
- IncidentResponse does not expose SLA status/deadlines. UI shows unavailable rather
  than fabricating SLA values.
- Reporting retains the current backend contract with serviceName/category/criticality.
- Timestamps have no offset; the current frontend interprets them in the browser timezone.

## Validation
Passed: npm run lint, npm run build, node check-api.mjs.
The adapter check uses intercepted responses matching the Java DTO/controller contracts.
It is not an end-to-end database/browser test. Run the acceptance flow locally.
