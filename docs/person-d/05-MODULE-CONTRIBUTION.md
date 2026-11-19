# Person D: Module Contribution

## Paragraph for the group report

Naman Yerra developed the shared React frontend and employee workspace for I-ITSM, including role-aware navigation, reusable cards and status badges, responsive layouts and persistent appearance preferences. The employee workflow covers reporting incidents, viewing and filtering submitted incidents, reading investigation notes, adding information, viewing resolutions, verifying fixes and submitting feedback. He also contributed the technician interface for assigned work and lifecycle actions. His registration frontend includes validation, accessible field errors, duplicate-submission protection and an isolated API adapter; live account creation awaits the backend registration contract and integration acceptance. He prepared the problem analysis, requirements specification, use-case diagram, nine-question technology selection report and three-workflow demo script. Validation includes frontend lint/build checks, registration unit tests and intercepted-response API adapter checks; these are distinguished from live end-to-end acceptance.

## Short architecture explanation for questions

The browser handles presentation and form interaction. An API helper sends JSON and bearer tokens and translates HTTP failures into readable errors. Role routing improves navigation, while Spring checks ownership and permissions. Java domain services own priority, assignment and incident-state rules. Repositories handle persistence. This separation allows UI changes without rewriting lifecycle logic and backend strategy changes without duplicating rules in React.

## Likely viva questions

**Why use React when this is an OOP project?** The brief permits React plus a REST API. Java contains the domain model and design-pattern implementations; React provides a reusable browser interface.

**Does the frontend enforce security?** It hides irrelevant actions and redirects role routes, but authorization must be enforced by the server on every request, including manually crafted ones.

**How do you prevent duplicate submissions?** A synchronous ref lock prevents a second request before React updates the disabled state. A pending state also disables the submit button. Server-side idempotency is a separate backend concern.

**What happens when the backend is unavailable?** The API helper reports a network error; forms retain their values and allow retry. No success is displayed until a successful response arrives.

**Are the dashboard graphs fabricated?** No. Employee summaries use the authorized loaded incident records and available timestamps. Demo mode is labeled; missing fields are not invented.

**Are the updates a complete audit trail?** Not in the supplied integration. Current live UI shows recorded timestamps and incident-derived updates. C/A must expose and wire the real audit/inbox APIs.

**Why is registration gated?** The uploaded AuthController has login/me/logout only. A visible form must not claim to create accounts without an agreed backend operation. The adapter and contract checklist make the remaining integration explicit.

**What remains before the team submission?** Backend contract integration and live regression, the other owners' report sections and tests, team PR merge, and recording the three complete workflows.
