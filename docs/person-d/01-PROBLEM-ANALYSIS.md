# I-ITSM: Problem Analysis

Prepared by Naman Yerra (Person D). Reviewed against the supplied CS F213/MAC F212 project brief, Semester I, 2026-2027. This section describes the system problem and intended solution; implementation evidence and outstanding integration are recorded separately in INTEGRATION.md.

## Problem and context

An organization depends on shared networks, authentication, applications, databases and other IT services. A service interruption affects both the person reporting it and people who may never submit a report. A payment application outage, for example, can stop transactions across a department while arriving at support as several unrelated messages. The organization needs a consistent way to restore service, allocate limited technical capacity and retain evidence of what happened.

Informal email, chat and spreadsheet handling makes it difficult to distinguish severity from the order in which requests arrived. An urgent message is not necessarily the highest-impact incident. Different technicians may investigate the same outage independently, while important incidents remain unassigned. Without explicit lifecycle rules, a report may be closed before the reporter confirms that the fix works.

## Stakeholders

| Stakeholder | Need | Consequence of failure |
| --- | --- | --- |
| Employee/reporter | Report quickly, track own incidents, provide information, verify resolution | Lost work and repeated follow-up messages |
| Technician | Assigned queue, context, investigation notes, deadlines and workload | Duplicated effort and missed response targets |
| Administrator/IT manager | Manage services, technicians, policies and operational visibility | Unbalanced work, weak accountability and poor prioritization |
| Organization/service owner | Restore important services and identify recurring failures | Business disruption and declining trust |
| Maintainer/evaluator | Understand design, reproduce behavior and extend rules | Fragile changes and unverifiable claims |

## Existing challenges

1. Inconsistent reports omit the affected service, impact or urgency, increasing clarification time.
2. Manual triage can prioritize loud or recent requests over business-critical work.
3. Assignment without considering skills, availability and active load can overload technicians.
4. Separate communication channels lose investigation and resolution history.
5. Missing deadlines and warning mechanisms make SLA violations visible too late.
6. Similar reports hide a shared outage and consume support capacity independently.
7. Weak role separation can expose other employees' incidents or allow unauthorized lifecycle actions.
8. Reports without persistent structured data cannot reliably support trend analysis.

## Proposed solution

I-ITSM is a browser-based workspace backed by a Java service and PostgreSQL. An authenticated employee reports an incident; the domain layer classifies and prioritizes it, applies an SLA and supports technician assignment. The assigned technician acknowledges the incident, investigates, records notes and provides a resolution. The reporter verifies the fix and supplies feedback; an administrator closes the incident. Notifications and audit observers provide communication and accountability around domain events.

The interface uses a shared navigation shell, consistent status labels, readable forms and compact dashboards. Employees see their own incidents, technicians see assigned work, and administrators have management views. Role checks must also be enforced by the server: a hidden button is not an authorization control.

Intelligent assistance is rule-based and explainable. Duplicate detection, category suggestion, risk prediction and correlation are intended to support human judgment, not automatically delete reports or guarantee a diagnosis. A similarity score represents a heuristic match, not a probability of the same root cause.

## Scope and boundaries

The required lifecycle is NEW -> CLASSIFIED -> ASSIGNED -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED -> VERIFIED -> CLOSED. The service layer is responsible for transition validity. The frontend presents available actions and reports server errors without inventing success.

Person D owns the shared frontend, registration/employee screens and the assigned report sections. Earlier technician frontend assistance is preserved. Backend registration, persisted service selection, incident-update rules and request-info integration belong to the backend owner. The supplied source does not yet expose these new contracts. Search/report/notification modules and the team's 15-case test document are owned by their assigned teammates.

This is a local course-project deployment, not a production service commitment. External notification delivery may be simulated where the rubric allows it. Real channels require credentials and network connectivity. No machine learning training, Kubernetes deployment or mobile application is necessary to satisfy the chosen architecture.

## Assumptions

- Each user has one server-assigned role. Public registration creates employees only; technician and administrator accounts are provisioned by an administrator.
- A reporter is derived from the authenticated session, not accepted as an arbitrary browser field.
- Services and policy configuration are managed centrally once the promised persistence integration is available.
- An incident has one affected service and at most one current assigned technician in the present domain model.
- A resolved incident is operationally completed for workload summaries but remains eligible for reporter verification and administrator closure.
- Test/demo data are explicitly identified. Dashboard counts use authorized incident data; absent SLA information is shown as unavailable.
- The current API sends local timestamps without offsets. A same-timezone local demo is assumed; a deployment across timezones requires a documented UTC/offset contract.
- Example workflows in the brief are examples, not fixed incident IDs. Demo IDs must be recorded from actual responses.

## Success criteria and evaluation

The team should demonstrate three complete workflows, persist incident changes across refresh/restart where applicable, reject unauthorized actions and explain at least two design patterns with actual code. D's acceptance criteria are clear registration validation, usable employee reporting and follow-up, accurate status/resolution display, recoverable failures, role-aware routing and documentation consistent with the integrated build.

Improvement should be evaluated through observable behavior: fewer incomplete submissions, clear assignment/status visibility, traceable notes, valid lifecycle transitions and readable failure messages. No measured productivity improvement or response-time benchmark is claimed without an experiment.
