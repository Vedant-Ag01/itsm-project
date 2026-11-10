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