## Authentication (read this first)

Every request except login needs this header:

    Authorization: Bearer <token>

**POST /api/auth/login**

Request:

    { "email": "asha@bits.edu", "password": "password123" }

Response:

    {
      "token": "09ddfa96-cf39-4abc-90b4-1e240e7733d5",
      "userId": "E1",
      "name": "Asha",
      "email": "asha@bits.edu",
      "role": "EMPLOYEE",
      "department": "Finance"
    }

**GET /api/auth/me** returns the same shape for the logged-in user.
**POST /api/auth/logout** returns 204 and invalidates the token.

Demo accounts (all use password `password123`):
admin@bits.edu (ADMIN), asha@bits.edu (EMPLOYEE),
ravi@bits.edu and meera@bits.edu (TECHNICIAN).

Tokens are lost when the server restarts, so log in again.

## Incident list: search and filters

GET /api/incidents accepts these optional query parameters. Give several and an
incident must match all of them. Results are newest first and only include
incidents the logged-in user may see.

    status        e.g. RESOLVED          (NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED,
                                          IN_PROGRESS, RESOLVED, VERIFIED, CLOSED)
    priority      e.g. P1                (P1, P2, P3, P4)
    category      e.g. NETWORK
    technicianId  e.g. T1
    q             free text, matches id, title or description
    from, to      dates like 2026-11-10 (inclusive)

Example: GET /api/incidents?status=RESOLVED&priority=P1&q=vpn
A wrong value gives 400 with the list of allowed values.

## Incident shape (all list and action responses)

    {
      "id": "INC1003",
      "title": "Printer offline",
      "description": "Floor 3 printer not responding",
      "status": "RESOLVED",
      "priority": "P3",
      "category": "HARDWARE",
      "serviceName": "Printing",
      "impact": "LOW",
      "urgency": "MEDIUM",
      "reporterName": "Asha",
      "assignedTechnician": "Meera",
      "affectedUsers": 5,
      "createdAt": "2026-11-18T16:34:53.275973",
      "acknowledgedAt": "2026-11-18T16:34:54.376305",
      "resolvedAt": "2026-11-18T16:34:54.555591",
      "closedAt": null,
      "resolutionNote": "Replaced the power adapter",
      "closureNote": null,
      "feedbackRating": 5,
      "feedbackComment": "Great"
    }

Fields that have not happened yet are null.

## Notes

**GET /api/incidents/{id}/notes** returns the notes, oldest first.

    [ { "authorName": "Asha", "authorRole": "EMPLOYEE",
        "text": "It started after the power cut.",
        "createdAt": "2026-11-18T16:34:53.795716" } ]

**POST /api/incidents/{id}/notes** with `{ "text": "..." }` adds a note and
returns the full notes list (201). Anyone who can see the incident may add one.
A closed incident returns 409.

## Feedback

**POST /api/incidents/{id}/feedback** with `{ "rating": 5, "comment": "Great" }`.
Only the employee who reported the incident (or an admin). Rating must be 1 to 5
(otherwise 400). Only allowed once the incident is RESOLVED, VERIFIED or CLOSED
(otherwise 409). Returns the incident.

## Admin endpoints (ADMIN role only, others get 403)

**GET /api/admin/users**

    [ { "id": "T1", "name": "Ravi", "email": "ravi@bits.edu",
        "department": "IT", "role": "TECHNICIAN" } ]

**GET /api/admin/technicians**

    [ { "id": "T1", "name": "Ravi", "email": "ravi@bits.edu",
        "available": true, "skills": ["NETWORK"], "activeIncidents": 1 } ]

**PUT /api/admin/technicians/{id}/availability** with `{ "available": false }`
returns the technician. (Resets when the backend restarts.)

**POST /api/incidents/{id}/assign** with `{ "technicianId": "T2" }` assigns a
specific technician. 409 if that technician is unavailable, 400 if the id is
unknown, 409 if the incident is not in a state where assigning is allowed.

## Action endpoints and who may call them

    POST /api/incidents/{id}/auto-assign   ADMIN
    POST /api/incidents/{id}/acknowledge   the assigned TECHNICIAN, or ADMIN
    POST /api/incidents/{id}/start-work    the assigned TECHNICIAN, or ADMIN
    POST /api/incidents/{id}/resolve       the assigned TECHNICIAN, or ADMIN   body: { "note": "..." }
    POST /api/incidents/{id}/verify        the reporting EMPLOYEE, or ADMIN
    POST /api/incidents/{id}/close         ADMIN                               body: { "note": "..." }

Coming soon (not built yet): audit trail per incident, notifications, report
endpoints, assignment strategy switch, SLA deadlines and status on the incident.

### Errors

Every error has this shape:

    { "status": 409, "error": "Conflict", "message": "Cannot close an incident that is CLASSIFIED", "timestamp": "..." }

- 401: no token, a bad token, or wrong email/password
- 403: logged in, but this role (or this incident) is not allowed
- 404: unknown incident id
- 409: the lifecycle move is not allowed right now
- 400: invalid input, such as an empty title

### What changed in the incident endpoints

- **POST /api/incidents** no longer takes `reporterName`. The reporter is the logged-in user.
  Fields: `title`, `description`, `serviceName`, `criticality`, `impact`, `urgency`,
  `affectedUsers`, `category` (optional). Response: `{ "incident": {...}, "possibleDuplicateIds": [] }`.
- **auto-assign, acknowledge, start-work, verify** take no body.
- **resolve and close** take only `{ "note": "..." }`. There is no `actor` any more.

### Who can do what

| Action | Who |
|---|---|
| POST /api/incidents | EMPLOYEE, ADMIN |
| GET /api/incidents | all roles. Employees see their own, technicians see their assigned ones, admins see all |
| GET /api/incidents/{id} | same visibility rule |
| POST .../auto-assign | ADMIN |
| POST .../acknowledge, start-work, resolve | the assigned TECHNICIAN, or ADMIN |
| POST .../verify | the EMPLOYEE who reported it, or ADMIN |
| POST .../close | ADMIN |