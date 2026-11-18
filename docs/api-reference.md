# I-ITSM API reference (v1-freeze)

Base URL: `http://localhost:8080/api`
Every endpoint except login needs the header `Authorization: Bearer <token>`.
Tokens are kept in memory, so after a backend restart everyone must log in again.

Errors always look like `{ "status": 409, "error": "Conflict", "message": "...", "timestamp": "..." }`.
401 = not logged in, 403 = role not allowed, 404 = unknown id, 409 = illegal lifecycle move
or nobody available to assign, 400 = bad input.

Demo accounts (password `password123` for all):
admin@bits.edu (ADMIN), asha@bits.edu (EMPLOYEE), ravi@bits.edu and meera@bits.edu (TECHNICIAN).

Enum values: impact / urgency / criticality = LOW, MEDIUM, HIGH.
category = HARDWARE, SOFTWARE, NETWORK, DATABASE, CLOUD, SECURITY, AUTHENTICATION, APPLICATION, STORAGE.
status = NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED.
priority = P1..P4. SLA status = ON_TRACK, AT_RISK, BREACHED.

## Auth
| Method | Path | Who | Notes |
|---|---|---|---|
| POST | /auth/login | anyone | body `{email, password}` returns the token and user |
| GET | /auth/me | any | the logged-in user |
| POST | /auth/logout | any | |

## Incidents
| Method | Path | Who | Notes |
|---|---|---|---|
| POST | /incidents | EMPLOYEE, ADMIN | body `{title, description, serviceName, criticality, impact, urgency, affectedUsers, category}`. Returns `{incident, possibleDuplicateIds}`. The reporter is the logged-in user. |
| GET | /incidents | any | only incidents the user may see. Optional filters: `status, priority, category, technicianId, service, slaStatus, q, from, to` (dates like 2026-11-10). Newest first. |
| GET | /incidents/{id} | who may see it | |
| POST | /incidents/{id}/auto-assign | ADMIN | uses the current assignment strategy |
| POST | /incidents/{id}/assign | ADMIN | body `{technicianId}` |
| POST | /incidents/{id}/acknowledge | assigned TECHNICIAN, ADMIN | |
| POST | /incidents/{id}/start-work | assigned TECHNICIAN, ADMIN | |
| POST | /incidents/{id}/resolve | assigned TECHNICIAN, ADMIN | body `{note}` |
| POST | /incidents/{id}/verify | reporting EMPLOYEE, ADMIN | |
| POST | /incidents/{id}/close | ADMIN | body `{note}` |
| GET / POST | /incidents/{id}/notes | who may see it | POST body `{text}` |
| POST | /incidents/{id}/feedback | reporting EMPLOYEE, ADMIN | body `{rating 1-5, comment}` |
| GET | /incidents/{id}/audit | who may see it | history, oldest first |
| GET | /incidents/{id}/sla | who may see it | `{incidentId, priority, responseDeadline, resolutionDeadline, status}` |

## Notifications (the logged-in user's own)
| Method | Path | Notes |
|---|---|---|
| GET | /notifications | `{unread, notifications:[{id, recipient, subject, message, createdAt, read}]}` newest first |
| POST | /notifications/{id}/read | returns `{unread}` |
| POST | /notifications/read-all | returns `{unread}` |

## Admin (ADMIN only)
| Method | Path | Notes |
|---|---|---|
| GET | /admin/users | all accounts |
| GET | /admin/technicians | technicians with skills, availability, active incident count |
| PUT | /admin/technicians/{id}/availability | body `{available: true/false}` |
| GET / PUT | /admin/assignment-strategy | GET `{current, available[]}`; PUT body `{name}` e.g. `SkillBasedStrategy` or `LoadBasedStrategy` |
| GET | /admin/audit | every audit record |
| GET | /admin/reports/overview | `{totalIncidents, openIncidents, criticalP1Incidents, resolvedIncidents, slaViolations}` |
| GET | /admin/reports/workload | `[{technicianId (the name), activeIncidentCount}]` |
| GET | /admin/reports/categories | `[{category, count}]` |
| GET | /admin/reports/resolution-time | `{averageResolutionMinutes}` |
| POST | /admin/monitor/run | runs the SLA and escalation check now; returns `{slaWarnings, slaBreaches, escalations}` |

An audit record: `{incidentId, eventType, actor, previousStatus, newStatus, description, timestamp, formattedLog}`.
The SLA and escalation check also runs by itself every 30 seconds and writes SLA_WARNING, SLA_BREACHED
and ESCALATED events into the audit trail and the notifications.