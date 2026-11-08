# I-ITSM API examples

These are the JSON shapes the backend will use. Frontend screens can be built
against these with fake data before the real API exists.

Conventions
- Base URL: `http://localhost:8080/api`
- After login, every request sends the header `Authorization: Bearer <token>`
- Dates and times are text like `2026-11-10T10:05:00`
- Errors always look like `{ "status": 409, "error": "message for the user" }`
- Status codes: 400 bad input, 401 not logged in or bad login, 403 not allowed
  for this role, 404 not found, 409 not allowed right now (wrong lifecycle step)

Allowed values
- role: EMPLOYEE, TECHNICIAN, ADMIN
- status: NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED
- priority: P1, P2, P3, P4 (labels: Critical, High, Medium, Low)
- impact, urgency, criticality: LOW, MEDIUM, HIGH
- category: HARDWARE, SOFTWARE, NETWORK, DATABASE, CLOUD, SECURITY, AUTHENTICATION, APPLICATION, STORAGE
- slaStatus: ON_TRACK, AT_RISK, BREACHED

---

## 1. Login

`POST /api/auth/login`  (no token needed)

Request
```json
{ "email": "asha@bits.edu", "password": "secret123" }
```

Response 200
```json
{
  "token": "eyJhbGciOi...",
  "user": { "id": "E1", "name": "Asha", "email": "asha@bits.edu", "role": "EMPLOYEE" }
}
```

Wrong password gives 401
```json
{ "status": 401, "error": "Invalid email or password" }
```

---

## 2. Report an incident (Employee)

`POST /api/incidents`

Request
```json
{
  "title": "Payment application is down",
  "description": "Customers cannot complete payments since 10 am.",
  "serviceId": 3,
  "impact": "HIGH",
  "urgency": "HIGH",
  "affectedUsers": 120
}
```

Response 201 (the new incident, plus any possible duplicates that were found)
```json
{
  "incident": { "id": "INC1024", "status": "CLASSIFIED", "title": "Payment application is down" },
  "possibleDuplicates": [
    { "id": "INC1019", "title": "Payment transaction failing", "score": 0.62 }
  ]
}
```

The full incident shape is in section 4.

---

## 3. List and search incidents

`GET /api/incidents`

All query parameters are optional and can be combined:
`id`, `status`, `priority`, `category`, `serviceId`, `technicianId`,
`slaStatus`, `from` (date), `to` (date)

Example: `GET /api/incidents?status=IN_PROGRESS&priority=P1&from=2026-11-01`

Response 200
```json
{
  "total": 2,
  "items": [
    {
      "id": "INC1024",
      "title": "Payment application is down",
      "service": "Payment application",
      "category": "APPLICATION",
      "priority": "P1",
      "status": "IN_PROGRESS",
      "assignedTechnician": "Ravi",
      "slaStatus": "AT_RISK",
      "createdAt": "2026-11-10T10:05:00"
    },
    {
      "id": "INC1030",
      "title": "VPN not connecting",
      "service": "VPN",
      "category": "NETWORK",
      "priority": "P1",
      "status": "IN_PROGRESS",
      "assignedTechnician": "Meena",
      "slaStatus": "ON_TRACK",
      "createdAt": "2026-11-10T11:20:00"
    }
  ]
}
```

Employees only see their own incidents. Technicians see their assigned
ones. Admins see all.

---

## 4. One incident in full

`GET /api/incidents/INC1024`

Response 200
```json
{
  "id": "INC1024",
  "title": "Payment application is down",
  "description": "Customers cannot complete payments since 10 am.",
  "category": "APPLICATION",
  "service": { "id": 3, "name": "Payment application", "criticality": "HIGH" },
  "impact": "HIGH",
  "urgency": "HIGH",
  "affectedUsers": 120,
  "priority": "P1",
  "priorityLabel": "Critical",
  "status": "IN_PROGRESS",
  "reporter": { "id": "E1", "name": "Asha" },
  "assignedTechnician": { "id": "T1", "name": "Ravi" },
  "createdAt": "2026-11-10T10:05:00",
  "acknowledgedAt": "2026-11-10T10:12:00",
  "resolvedAt": null,
  "closedAt": null,
  "responseDeadline": "2026-11-10T10:10:00",
  "resolutionDeadline": "2026-11-10T10:35:00",
  "slaStatus": "AT_RISK",
  "resolutionNote": null,
  "closureNote": null,
  "notes": [
    { "author": "Ravi", "text": "Checked the logs, database is full", "createdAt": "2026-11-10T10:20:00" }
  ],
  "feedback": null
}
```

---

## 5. Lifecycle actions

Each action returns the updated incident (same shape as section 4).

| Action | Request | Who | Body |
| --- | --- | --- | --- |
| Assign | `POST /api/incidents/{id}/assign` | Admin | optional `{ "technicianId": "T1" }`; leave empty to let the system choose |
| Acknowledge | `POST /api/incidents/{id}/acknowledge` | Technician | none |
| Start work | `POST /api/incidents/{id}/start` | Technician | none |
| Resolve | `POST /api/incidents/{id}/resolve` | Technician | `{ "resolutionNote": "Cleared the database logs" }` |
| Verify | `POST /api/incidents/{id}/verify` | Employee (reporter) | none |
| Close | `POST /api/incidents/{id}/close` | Admin | `{ "closureNote": "Employee confirmed the fix" }` |

A wrong step gives 409, for example closing a NEW incident:
```json
{ "status": 409, "error": "Cannot close an incident that is NEW" }
```

---

## 6. Notes and feedback

Add a note: `POST /api/incidents/{id}/notes`
```json
{ "text": "Restarted the payment service, monitoring now" }
```

Give feedback after resolution: `POST /api/incidents/{id}/feedback`
```json
{ "rating": 5, "comment": "Fixed quickly, thanks" }
```

Both return the updated incident.

---

## 7. Audit trail

`GET /api/incidents/INC1024/audit`

Response 200 (oldest first)
```json
[
  {
    "timestamp": "2026-11-10T10:05:00",
    "user": "Asha (E1)",
    "action": "CREATED",
    "incidentId": "INC1024",
    "previousStatus": null,
    "newStatus": "NEW",
    "description": "Incident created by Asha"
  },
  {
    "timestamp": "2026-11-10T10:06:00",
    "user": "System",
    "action": "PRIORITY_CALCULATED",
    "incidentId": "INC1024",
    "previousStatus": "NEW",
    "newStatus": "CLASSIFIED",
    "description": "Priority calculated as P1"
  }
]
```

---

## 8. Notifications (for the logged-in user)

`GET /api/notifications`

Response 200
```json
[
  {
    "id": 17,
    "channel": "IN_APP",
    "subject": "Incident assigned",
    "message": "INC1024 has been assigned to you",
    "createdAt": "2026-11-10T10:07:00",
    "read": false
  }
]
```

Mark as read: `POST /api/notifications/17/read` returns 204 with no body.

---

## 9. Reports (Admin)

`GET /api/reports/overview`
```json
{ "total": 250, "open": 42, "critical": 8, "resolved": 198, "slaViolations": 10 }
```

`GET /api/reports/technician-workload`
```json
[
  { "technician": "Ravi", "activeIncidents": 5 },
  { "technician": "Meena", "activeIncidents": 2 }
]
```

`GET /api/reports/service-analysis`
```json
[
  { "service": "Network", "incidents": 48 },
  { "service": "Database", "incidents": 35 }
]
```

---

## 10. Admin

Users (Admin only)

`GET /api/admin/users` returns
```json
[ { "id": "E1", "name": "Asha", "email": "asha@bits.edu", "role": "EMPLOYEE", "active": true } ]
```

`POST /api/admin/users` request
```json
{ "name": "Kiran", "email": "kiran@bits.edu", "password": "temp1234", "role": "TECHNICIAN",
  "skills": ["NETWORK", "SECURITY"] }
```

Technicians: `GET /api/admin/technicians`
```json
[ { "id": "T1", "name": "Ravi", "available": true, "skills": ["APPLICATION", "DATABASE"], "activeIncidents": 3 } ]
```

Services: `GET /api/admin/services` returns
```json
[ { "id": 3, "name": "Payment application", "criticality": "HIGH" } ]
```
`POST /api/admin/services` request: `{ "name": "VPN", "criticality": "MEDIUM" }`

SLA policies: `GET /api/admin/sla-policies` returns
```json
[
  { "priority": "P1", "responseMinutes": 5, "resolutionMinutes": 30 },
  { "priority": "P2", "responseMinutes": 15, "resolutionMinutes": 120 }
]
```
`PUT /api/admin/sla-policies` takes the same list.

Assignment strategy: `GET /api/admin/settings/assignment-strategy`
```json
{ "current": "SKILL_BASED", "available": ["SKILL_BASED", "LOAD_BASED"] }
```
`PUT /api/admin/settings/assignment-strategy` request: `{ "current": "LOAD_BASED" }`

SLA violations: `GET /api/admin/sla-violations` returns a list in the same shape as section 3's `items`.