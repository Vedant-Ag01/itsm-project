# I-ITSM: Intelligent IT Service Management and Incident Resolution System

CS F213 / MAC F212 Object-Oriented Programming, Semester I, 2026-2027, BITS Pilani, Hyderabad Campus. Group 31.

I-ITSM manages the life of an IT incident: an employee reports a problem, the system works out its
priority and SLA, assigns a technician, notifies the people involved, tracks the investigation, and
keeps a full audit trail until the incident is closed.

## Team

| Member | Main work |
|---|---|
| Vedant Agarwal (2025A7PS0025H) | Backend integration, REST API, persistence, administration |
| Arnav Mittal (2025A7PS0186H) | Priority and assignment strategies, SLA monitoring, escalation, Telegram channel |
| Kaushik Karma (2025A7PS0157H) | Events, audit, notifications, duplicate detection, search, reports |
| Naman Yerra (2025A7PS0075H) | React frontend, employee and technician screens, documentation |

## What it does

- **Three roles** with separate screens: employee, technician, administrator.
- **Incident lifecycle** (State pattern): New, Classified, Assigned, Acknowledged, In progress, Resolved, Verified, Closed. Invalid jumps such as New to Closed are rejected.
- **Priority** (Strategy): weighted score from impact, urgency, service criticality and affected users, mapped to P1 to P4.
- **Technician assignment** (Strategy): skill-based or load-based, switchable by the admin. A workload limit applies.
- **SLA**: response and resolution deadlines per priority, a background check every 30 seconds, warnings and breach records.
- **Notifications** (Observer): in-app, simulated email, and real Telegram messages.
- **Audit trail**: every action is stored with who, when, and the old and new state.
- **Search and reports**: filter by ID, text, date, status, priority, category, service, technician and SLA status; overview, workload, service and category reports.
- **Intelligent features**: duplicate detection, automatic escalation, category suggestion, SLA risk detection, incident correlation, suggested resolutions.

## Tech stack

Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, React 19 with Vite, Maven. Telegram Bot API for the optional external notifications.

## How to run it

You need: JDK 21, Node.js 22 (or 20.19 or newer), and PostgreSQL running on port 5432.

### 1. Database

Create an empty database called `itsm`:

```
psql -U postgres -c "CREATE DATABASE itsm;"
```

The connection settings are in `src/main/resources/application.properties` (user `postgres`,
password `itsm123`). Change the password there if yours is different. The tables are created
automatically on first start.

### 2. Backend

From the project folder:

```
./mvnw spring-boot:run          (Windows PowerShell: .\mvnw.cmd spring-boot:run)
```

The API runs on http://localhost:8080. On the first start it creates four demo accounts.

### 3. Frontend

In a second terminal:

```
cd frontend
npm install
```

Create a file `frontend/.env.local` with these two lines (copy `.env.example`):

```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCKS=false
```

Then:

```
npm run dev
```

Open the address it prints (normally http://localhost:5173). Without the `.env.local` file the
frontend runs on built-in mock data and does not talk to the backend.

### Demo accounts

All use the password `password123`.

| Email | Role |
|---|---|
| admin@bits.edu | Administrator |
| asha@bits.edu | Employee |
| ravi@bits.edu | Technician (network, hardware, security, storage, authentication) |
| meera@bits.edu | Technician (software, database, cloud, application, authentication) |

Anyone can also register a new employee account from the sign-in page.

### Optional: demo data

With the backend running, on a fresh empty database:

```
powershell -File docs/seed-demo.ps1
```

This creates about a dozen sample incidents in different states.

### Optional: Telegram notifications

Telegram is switched on only if both environment variables are set before the backend starts:

```
$env:TELEGRAM_BOT_TOKEN="<your bot token>"
$env:TELEGRAM_CHAT_ID="<your chat id>"
```

The backend prints "Telegram notifications are ON" or "OFF" at startup. Setup steps are in
`docs/telegram-notifications.md`.

### Running the tests

Stop the backend first, then:

```
./mvnw test
```

The last run gave 494 tests, 0 failures, BUILD SUCCESS. Do not use `clean` while the backend is running.

## Project layout

```
src/main/java/edu/bits/itsm/
  api/            REST controllers and request/response records
  auth/           login, roles, password hashing, user management
  domain/         Incident, the state classes, users, enums, domain exceptions
  priority/       PriorityStrategy and WeightedPriorityStrategy
  assignment/     AssignmentStrategy, SkillBasedStrategy, LoadBasedStrategy
  sla/            SLA policy, monitor, risk detection
  escalation/     automatic escalation
  event/          IncidentEventPublisher and event types
  audit/          audit observer and repository
  notification/   channels (in-app, email, Telegram) and the observer
  duplicate/      similarity-based duplicate detection
  classification/ category suggestion from text
  correlation/    grouping of related incidents
  suggestion/     suggested resolutions
  search/         incident search and filters
  reporting/      reports
  repository/     IncidentRepository interface and in-memory version
  config/         wiring of the beans (AppConfig), web and CORS settings
  persistence/    JPA entities, tables and mappers
  service/        IncidentService, factory, service catalogue
src/test/         unit and API tests
frontend/src/     React app: pages (admin, employee, technician), components, api
docs/             design notes, API reference, Telegram guide, seed script
```

## Design patterns used

| Pattern | Where |
|---|---|
| Strategy | `PriorityStrategy`, `AssignmentStrategy` |
| State | `IncidentState` and the eight state classes |
| Observer | `IncidentEventPublisher`, `AuditLogObserver`, `NotificationObserver` |
| Factory | `IncidentFactory`, `NotificationFactory` |
| Repository | `IncidentRepository`, `JpaIncidentRepository`, `AuditRepository` |
| Adapter | `TelegramChannel` behind the notification channel contract |
| Decorator | `DeduplicatingChannel` wrapped around `TelegramChannel` |

## More documentation

- `docs/DESIGN.md`: design notes
- `docs/api-reference.md` and `docs/api_examples.md`: the REST API
- `docs/telegram-notifications.md`: Telegram setup
- `docs/person-d/`: problem analysis, requirements, technology selection, demo script

## Known limitations

- Incident categories are fixed in the code (nine values). Adding one is a small code change, but there is no admin screen for it.
- Escalation state is held in memory, so a backend restart resets the current escalation level (the audit entries stay).
- Login tokens are held in memory, so everyone must sign in again after a restart.
- Passwords use salted SHA-256, adequate for this project; a real system would use bcrypt or Argon2.
- Incident lists are not paginated.
- The database password is in `application.properties` for the demo setup.

## AI assistance

AI tools were used for parts of the report, some code, and test help. The team ran, reviewed and
tested the result.
