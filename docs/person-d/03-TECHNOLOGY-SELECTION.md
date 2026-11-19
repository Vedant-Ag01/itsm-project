# I-ITSM: Technology Selection Report

This section answers all nine questions in the supplied project PDF. Versions below are from the uploaded manifests, not assertions about the latest available releases. It describes the implementation supplied for review and marks scaling/security proposals as future work.

## Stack inventory

| Layer | Technology in supplied project | Purpose |
| --- | --- | --- |
| Domain and backend language | Java 21 | Domain objects, interfaces, records, collections and lifecycle rules |
| Frontend language | JavaScript with JSX, HTML and CSS | Browser behavior, component markup and presentation |
| Frontend | React 19.3.1; React DOM 19.3.1 | Component-based role workspaces and controlled forms |
| Routing | react-router 8.5.1 | Login, registration and nested role-specific pages |
| Build and static checks | Vite 8.3.6; ESLint 10.14.0 | Development server, production assets and lint checks |
| Backend framework | Spring Boot 4.0.8, Spring Web MVC | HTTP controllers, request mapping and dependency wiring |
| Persistence | Spring Data JPA, PostgreSQL JDBC driver, PostgreSQL | Mapping and persistent user/incident storage |
| Build/test | Maven configuration, JUnit test classes, Node test runner | Java tests and frontend validation/adapter checks |
| Transport | REST-style JSON over HTTP on localhost | Browser/backend boundary |
| Collaboration | Git and GitHub feature branches/PRs | Reviewable changes and team integration |
| UI assets | IBM Plex Sans embedded in existing stylesheet; SVG/CSS | Local typography, icons and responsive themes |

There is no required paid external API for D's module. Telegram integration is a teammate extension; it is not implemented or claimed by D. Node/npm are frontend development tools and do not replace the Java backend.

## 1. Why did you choose this programming language?

Java suits a system whose central problem is modeling users, incidents, policies and valid lifecycle behavior. Types such as IncidentStatus, Priority, Impact and Urgency make domain values explicit. Interfaces let assignment and priority algorithms change without changing every caller. Records provide compact data-transfer values, while exceptions distinguish invalid actions from successful transitions.

Java also provides collections, date/time types and a mature testing ecosystem useful for incident histories, active workloads and SLA deadlines. The team can demonstrate encapsulation, inheritance where appropriate, polymorphism and composition in a concrete service domain rather than adding classes solely to satisfy a checklist.

JavaScript is used in the browser because interactive forms, routing and rendering run there. This division does not weaken the OOP focus: lifecycle and policy rules remain in Java. JavaScript's weaker compile-time guarantees are addressed partly by validation, explicit adapters, linting and tests; TypeScript would be a reasonable future refinement.

## 2. Why did you choose this frontend technology?

React lets the team reuse the same cards, badges, tables, navigation, form patterns and theme controls across role-specific pages. Controlled inputs make it practical to preserve user-entered values after failed requests. Component boundaries separate the workspace shell from employee reporting, incident details and technician workload views.

React Router maps URLs to those views so an incident can be opened directly and browser navigation remains meaningful. Vite supplies a fast development loop and bundles a static production frontend. CSS custom properties provide consistent colors across the five appearance presets without duplicating every component.

The tradeoff is client-side state and routing complexity. The frontend must handle loading, stale requests, session restoration and server failures explicitly. A production host also needs an SPA fallback for deep links. React does not itself enforce authorization or validate business transitions.

## 3. Why did you choose this backend technology?

Spring Boot provides the HTTP and integration layer around the domain model. Spring Web MVC controllers receive JSON, obtain the current user, check access and call domain services. Spring Data JPA connects repository operations to PostgreSQL. Dependency injection centralizes which strategies, observers and repositories are active.

This reduces infrastructure work within a student project and leaves time for incident behavior and testing. New intelligent-feature classes should remain plain Java, as the team's work division requires; framework annotations belong in the wiring/API/persistence boundary when needed. Domain algorithms can then be tested without starting an HTTP server.

The limitations are framework startup cost, configuration complexity and the possibility of accidentally coupling business rules to controllers. The supplied AppConfig still uses temporary priority/assignment strategies and no-op duplicate/observer wiring, so the existence of other classes does not prove those classes run in the application.

## 4. Why did you select this database?

PostgreSQL fits the structured relationships among users, incidents, assigned technicians, services and policy information. A relational database supports constraints, transactions and queries over status, time and ownership. Durable incident/resolution data make refresh and later reporting meaningful.

JPA maps persistence entities to domain objects through a mapper, helping keep database concerns separate from the incident lifecycle. The supplied project persists users and incidents; additional service, policy and audit persistence must be confirmed in the integrated release.

The cost is a separately running database, schema management and connection configuration. SQLite would simplify a single-machine prototype, but PostgreSQL better matches concurrent multi-user operation. Persistence technology alone does not guarantee correctness: indexes, migrations, transactional boundaries and concurrency behavior still need deliberate design.

## 5. Why did you choose this architecture?

The design is a layered web application with a modular Java backend: browser components -> API adapters -> REST controllers/authentication -> domain/application services -> repositories -> PostgreSQL. Event observers connect incident changes to notification and audit modules when wired. This is one deployable backend, not a microservice system.

Layering keeps UI presentation separate from authoritative business rules. A frontend can change layout without rewriting IncidentState, while a new assignment strategy can implement the same interface without changing employee forms. The API adapter absorbs DTO differences, such as serviceName versus the UI's service object.

A modular monolith is practical for a small team and local demo. It avoids distributed tracing, service discovery and cross-service consistency costs. Boundaries still make future extraction possible if real load or organizational ownership justifies it.

## 6. What alternatives did you consider?

These are architectural alternatives evaluated for this report, not claims that the team built benchmark prototypes of each.

| Alternative | Benefit | Why the chosen stack fits this project |
| --- | --- | --- |
| Java Swing desktop UI | One primary language and direct OOP GUI examples | Browser access across roles avoids distributing a desktop client |
| Server-rendered Spring templates | Simpler frontend build and session integration | React suits the shared interactive dashboards already implemented |
| Angular or Vue | Strong UI ecosystems and reusable components | Switching now adds migration work without a demonstrated requirement |
| Node/Express backend | Shared browser/server language | Java expresses the course's domain interfaces and lifecycle model directly |
| Python/FastAPI backend | Compact APIs and easy ML experimentation | The system uses explainable rules and benefits from the existing Java model |
| SQLite | Minimal local setup | PostgreSQL better represents concurrent organizational data use |
| MongoDB | Flexible document shape | Relational identities, assignments and reporting favor the selected model |
| Microservices | Independent scaling/deployment | Not justified by a four-person local course-project scope |

## 7. What are the advantages and limitations of your technology stack?

Advantages include reusable UI components, type-safe domain enums/interfaces, testable plain-Java strategies, persistent structured data, an explicit HTTP boundary and familiar Git-based review. The existing theme assets are local, so the interface does not need a font CDN at runtime.

Limitations in the supplied implementation are material. Authentication uses opaque UUID bearer tokens held in an in-memory map; these are not JWTs and disappear on restart. The current password hash is salted SHA-256 and needs replacement with a dedicated adaptive password-hashing scheme before production. The browser stores its session token in sessionStorage, which remains accessible to JavaScript; XSS prevention and deployment security matter.

The uploaded backend does not expose the announced registration, update-incident or request-info contract. Its IncidentResponse lacks SLA deadlines/status, so those values cannot be truthfully displayed yet. Notifications are currently incident-derived frontend updates with local read state, and timestamp milestones are not a full audit log. Some new backend modules are present but not wired into AppConfig.

The frontend currently loads the authorized incident list and filters/aggregates it locally. This is acceptable for a small demo dataset but not a scalable reporting architecture. Embedded font data makes the CSS bundle large; separate cached WOFF2 assets would reduce repeat transfer costs. None of these limitations is solved merely by choosing a framework.

## 8. How does your technology support object-oriented design?

| OOP idea/pattern | Concrete project evidence | Reason |
| --- | --- | --- |
| Encapsulation | Incident methods manage lifecycle behavior | Protect state changes behind domain operations |
| Abstraction/polymorphism | AssignmentStrategy and PriorityStrategy | Select different implementations through one contract |
| State | NewState, AssignedState, InProgressState, ResolvedState and related classes | Model permitted actions by current lifecycle state |
| Repository | IncidentRepository with in-memory and JPA implementations | Separate storage from service callers |
| Factory | IncidentFactory | Centralize incident construction |
| Observer | IncidentObserver and event/audit/notification classes | Add reactions to events without UI coupling |
| Composition | IncidentService collaborates with repository, factory and strategies | Assemble behavior from focused objects |
| Inheritance | Employee, Technician and Administrator domain roles | Express role-specific forms of a user where meaningful |

React composition is useful for reuse, but the report's OOP evidence comes primarily from the Java domain. The team should open the actual implementations during the video and explain a call path. A named interface alone is not proof of a complete pattern; wiring and behavior must also be demonstrated.

## 9. How would your system evolve if the number of users increased significantly?

First measure request volume, incident count, response latency, database query cost and error rates. Add server-side pagination, filtering and aggregate endpoints so browsers do not fetch entire histories. Index frequently queried ownership, assignment, status and creation-time columns based on query plans rather than guessing.

Next strengthen reliability: explicit database migrations, transactional lifecycle changes, optimistic concurrency/version checks, connection-pool tuning, backups and restore tests. Make authentication scalable and secure through a shared session store or an appropriately designed identity system, expiry/revocation, rate limiting and stronger password hashing. Use TLS and environment-based secrets outside the local demo.

Move slow notification delivery to an asynchronous queue with bounded retries and delivery records. Use a persistent event/outbox design if audit and notifications must reliably reflect committed incident changes. Schedule SLA checks with coordination so multiple backend instances do not duplicate escalations.

Only then scale backend replicas behind a load balancer, serve static frontend assets efficiently and consider caching suitable reads. Extract a service only when independent workload or ownership makes the distributed-system cost worthwhile. Load tests and observability should verify each change. These are proposed evolution steps, not features claimed in the current submission.

## Source basis

Project brief: Technology Selection Report questions 1-9; OOP, Security and Persistence sections. Implementation: pom.xml, frontend/package.json, AuthService.java, PasswordHasher.java, AppConfig.java, IncidentResponse.java, frontend API adapters and shared layout components in the supplied archive. No performance benchmark or production deployment is asserted.
