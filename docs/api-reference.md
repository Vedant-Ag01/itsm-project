# I-ITSM API reference (v2)

Base URL: `http://localhost:8080/api`
Every endpoint except login and register needs the header `Authorization: Bearer <token>`.
Tokens are kept in memory, so after a backend restart everyone must log in again.

Errors always look like `{ "status": 409, "error": "Conflict", "message": "...", "timestamp": "..." }`.
401 = not logged in (or account switched off), 403 = role not allowed or not yours, 404 = unknown id or URL,
405 = wrong HTTP method, 400 = bad input (empty/too long fields, unknown service, broken JSON, wrong enum value),
409 = illegal lifecycle move, nobody available to assign, technician workload exceeded, missing SLA configuration,
503 = database temporarily unavailable.

An incident object also carries `awaitingInfo` (true while a technician's question is unanswered).
The incident's serviceName must be one from `GET /services`; its criticality is taken from the service catalogue
(the criticality sent in the request is ignored).

Demo accounts (password `password123` for all):
admin@bits.edu (ADMIN), asha@bits.edu (EMPLOYEE), ravi@bits.edu and meera@bits.edu (TECHNICIAN).

Enum values: impact / urgency / criticality = LOW, MEDIUM, HIGH.
category = HARDWARE, SOFTWARE, NETWORK, DATABASE, CLOUD, SECURITY, AUTHENTICATION, APPLICATION, STORAGE.
status = NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED.
priority = P1..P4. SLA status = ON_TRACK, AT_RISK, BREACHED.

## Auth
| Method | Path | Who | Notes |
|---|---|---|---|
| POST | /auth/login | anyone | body `{email, password}` returns the token and user. Email is not case sensitive. A switched-off account gets 401. |
| POST | /auth/register | anyone | body `{name, email, department, password}` (password at least 8 characters). Always creates an EMPLOYEE and logs them in: returns the same shape as login (201). Duplicate email gives 400. |
| GET | /auth/me | any | the logged-in user |
| POST | /auth/logout | any | |

## Incidents
| Method | Path | Who | Notes |
|---|---|---|---|
| POST | /incidents | EMPLOYEE, ADMIN | body `{title, description, serviceName, criticality, impact, urgency, affectedUsers, category}`. Returns `{incident, possibleDuplicateIds}`. The reporter is the logged-in user. |
| GET | /incidents | any | only incidents the user may see. Optional filters: `status, priority, category, technicianId, service, slaStatus, q, from, to` (dates like 2026-11-10). Newest first. |
| GET | /incidents/{id} | who may see it | |
| PUT | /incidents/{id} | reporting EMPLOYEE, ADMIN | body any of `{title, description, impact, urgency, affectedUsers}` (omitted = unchanged). Priority is recalculated; audit gets INCIDENT_UPDATED and, if it changed, PRIORITY_CHANGED. Not allowed once RESOLVED/VERIFIED/CLOSED (409). Limits: title 200 chars, description 2000, affectedUsers 1 to 100000. |
| POST | /incidents/{id}/request-info | assigned TECHNICIAN, ADMIN | body `{message}`. Asks the reporter a question; the incident status does not change and `awaitingInfo` becomes true. |
| POST | /incidents/{id}/provide-info | reporting EMPLOYEE, ADMIN | body `{message}`. Answers the question; `awaitingInfo` becomes false. 400 if nothing was asked. |
| POST | /incidents/{id}/auto-assign | ADMIN | uses the current assignment strategy |
| POST | /incidents/{id}/assign | ADMIN | body `{technicianId}` |
| POST | /incidents/{id}/acknowledge | assigned TECHNICIAN, ADMIN | |
| POST | /incidents/{id}/start-work | assigned TECHNICIAN, ADMIN | |
| POST | /incidents/{id}/resolve | assigned TECHNICIAN, ADMIN | body `{note}` |
| POST | /incidents/{id}/verify | reporting EMPLOYEE, ADMIN | |
| POST | /incidents/{id}/close | ADMIN | body `{note}` |
| GET / POST | /incidents/{id}/notes | who may see it | POST body `{text}`. Each note has `{authorName, authorRole, text, createdAt, type}` where type is NOTE, INFO_REQUEST or INFO_RESPONSE. |
| POST | /incidents/{id}/feedback | reporting EMPLOYEE, ADMIN | body `{rating 1-5, comment}` |
| GET | /incidents/{id}/audit | who may see it | history, oldest first |
| GET | /incidents/{id}/sla | who may see it | `{incidentId, priority, responseDeadline, resolutionDeadline, status}` |

## Services and SLA targets
| Method | Path | Who | Notes |
|---|---|---|---|
| GET | /services | any | active services for the "report an incident" dropdown: `[{id, name, description, criticality, active}]` |
| GET | /sla-policies | any | `[{priority, responseMinutes, resolutionMinutes}]` for P1 to P4 |

## Technician
| Method | Path | Notes |
|---|---|---|
| GET | /technician/workload | TECHNICIAN only: `{technicianId, name, available, skills, activeIncidents, maxActiveIncidents, remainingCapacity, byStatus, incidents[]}` |

## Notifications (the logged-in user's own)
| Method | Path | Notes |
|---|---|---|
| GET | /notifications | `{unread, notifications:[{id, recipient, subject, message, createdAt, read}]}` newest first |
| POST | /notifications/{id}/read | returns `{unread}` |
| POST | /notifications/read-all | returns `{unread}` |

## Admin (ADMIN only)
| Method | Path | Notes |
|---|---|---|
| GET | /admin/users | all accounts: `[{id, name, email, department, role, active}]` |
| POST | /admin/users | body `{name, email, department, password, role}`; role EMPLOYEE or ADMIN only (technicians are configured in the backend because assignment depends on their skills) |
| PUT | /admin/users/{id} | body any of `{name, department, active}`. `active:false` switches the account off at once; an admin cannot switch off their own account. |
| GET | /admin/services | all services including switched-off ones |
| POST | /admin/services | body `{name, description, criticality}` (201). Duplicate name gives 400. |
| PUT | /admin/services/{id} | body any of `{description, criticality, active}` |
| PUT | /admin/sla-policies/{priority} | priority P1 to P4; body `{responseMinutes, resolutionMinutes}`. Response must not be longer than resolution. Takes effect immediately. |
| GET / PUT | /admin/workload-limit | GET `{maxActiveIncidents}`; PUT body `{maxActiveIncidents}` (at least 1). Default 8. A technician at the limit is skipped by auto-assign, and a manual assign is refused with 409 "Technician workload exceeded". |
| GET | /admin/technicians | technicians with skills, availability, active incident count |
| PUT | /admin/technicians/{id}/availability | body `{available: true/false}` |
| GET / PUT | /admin/assignment-strategy | GET `{current, available[]}`; PUT body `{name}` e.g. `SkillBasedStrategy` or `LoadBasedStrategy` |
| GET | /admin/audit | every audit record |
| GET | /admin/reports/overview | `{totalIncidents, openIncidents, criticalP1Incidents, resolvedIncidents, slaViolations}` |
| GET | /admin/reports/workload | `[{technicianId (the name), activeIncidentCount}]` |
| GET | /admin/reports/categories | `[{category, count}]` |
| GET | /admin/reports/resolution-time | `{averageResolutionMinutes}` |
| GET | /admin/reports/services | `[{serviceName, criticality, totalIncidents, openIncidents, resolvedIncidents, p1Incidents, slaBreaches, averageResolutionMinutes, mostCommonCategory}]` busiest service first |
| POST | /admin/monitor/run | runs the SLA and escalation check now; returns `{slaWarnings, slaBreaches, escalations}` |

An audit record: `{incidentId, eventType, actor, previousStatus, newStatus, description, timestamp, formattedLog}`.
The SLA and escalation check also runs by itself every 30 seconds and writes SLA_WARNING, SLA_BREACHED
and ESCALATED events into the audit trail and the notifications.