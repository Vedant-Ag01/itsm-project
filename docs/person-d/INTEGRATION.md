# D Integration and Validation Record

Date: 19 November 2026. Baseline: uploaded itsm-D-current.zip. Changes confined to frontend and docs. No Java package, database configuration or teammate backend module was edited.

## Completed in this delivery

- Registration route and login link, matching the existing appearance system.
- Labeled name/email/department/password/confirmation inputs, field validation and focus on the first invalid field.
- No password or confirmation stored in browser persistence; password confirmation/role/ID are omitted from the request.
- In-flight submission lock, preserved values after errors, duplicate-email message and success-to-login navigation.
- Registration adapter isolated from session login. No automatic sign-in or assumption about a returned token.
- Public registration defaults OFF until the backend contract is confirmed; demo mode cannot create real accounts.
- Existing employee and technician flows preserved.
- Problem analysis, requirements, use-case diagram, all nine technology answers, team demo script and D module paragraph.
- Registration tests and current API adapter regression checks.

## Why live D acceptance remains blocked

The supplied AuthController implements POST /auth/login, GET /auth/me and POST /auth/logout only. There is no docs/api-reference.md. The new registration, persisted employee service list, incident update and request-info endpoints announced by Vedant are absent. A GitHub read did not retrieve the missing file either. The ZIP also retains temporary AppConfig strategy/observer wiring.

The registration UI and adapter are complete against the proposed boundary below, but it would be inaccurate to call live account creation complete. Employee update/request-info integration must use the actual forthcoming contract. Existing additional-information notes continue to work against the supplied notes API.

## Proposed registration contract to confirm with Vedant

POST /api/auth/register, unauthenticated.

Request: {"name":"Naman Yerra","email":"naman@bits.edu","password":"example-password","department":"CSE"}

The browser sends no role, reporter ID or confirmation password. The server must create an EMPLOYEE. Proposed frontend limits: nonblank name <=100, valid email <=254, password 8-128 (preserve spaces, reject whitespace-only), optional department <=100. Confirm these limits and whether department is optional/required before enabling.

Success: a successful HTTP response (201 preferred); body may be empty or JSON and is not used for authentication. The user signs in separately. Errors: 400 validation, 409 duplicate email, 5xx failure, with message or error text. A malformed success response is treated as an error by the shared JSON client.

After confirmation, change api/registration.js and validation/registration.js if needed, then set VITE_USE_MOCKS=false and VITE_REGISTRATION_ENABLED=true in the existing local environment and restart Vite. Do not enable merely to remove the unavailable notice.

## Other integration questions

| Contract needed | Frontend change once specified |
| --- | --- |
| Employee-readable service catalog URL/shape | Replace free-text service/criticality with configured service selection; send the required service ID |
| Update incident method/path, fields and allowed states | Add edit controls with preserved input and server-authoritative permissions |
| Request-info fields, request and answer paths | Show open request, submit answer and display confirmed answered state |
| SLA fields and timezone convention | Render real deadlines/status and consistent timestamps |
| Notifications/audit APIs | C/shared UI replaces incident-derived updates and timestamp milestones |

## Validation actually executed

| Check | Actual result | Scope |
| --- | --- | --- |
| npm run lint | PASS | Frontend source static checks |
| npm run build | PASS | 122 modules; production assets generated |
| node --test tests/registration.test.mjs | PASS, 13 tests | Input boundaries and payload whitelist |
| node check-registration-disabled.mjs | PASS | Disabled endpoint makes no request |
| node check-registration.mjs | PASS | Intercepted HTTP request, success and error behavior |
| node check-api.mjs | PASS | Existing incident/notes/feedback/technician adapters with intercepted responses |
| Browser execution | BLOCKED | Chromium startup denied by execution environment; not a UI pass |
| Live registration/database test | NOT RUN | Registration backend absent from supplied archive |
| Java suite | NOT RUN in this delivery | No backend changes; previously reported compilation failures remain teammate work |

The output package does not claim that the whole group project or live D integration is finished. Do not copy unexecuted cases into an "actual result = pass" column.

## Final team gates

D can submit the frontend and document contribution for review now, with the blocked contracts listed in the PR. Before declaring the module fully complete: confirm/wire new contracts, run live registration and employee acceptance, merge the PR, assemble A's architecture/class/ER sections and B's tests into the single group PDF, and record all members demonstrating the integrated workflows.

## Focused D acceptance cases

This supplements B's required 15-case test document; it does not replace B's executed system test evidence.

| ID | Area | Input | Expected | Actual result |
| --- | --- | --- | --- | --- |
| D01 | Normal registration | Valid name/email/password | No validation errors | PASS unit |
| D02 | Invalid input | Blank/spaces name | Name error | PASS unit |
| D03 | Boundary | Name 100/101 chars | Accept 100, reject 101 | PASS unit |
| D04 | Invalid input | Malformed email | Email error | PASS unit |
| D05 | Boundary | Password 7 chars | Reject | PASS unit |
| D06 | Boundary | Password 8/128 chars | Accept | PASS unit |
| D07 | Boundary | Password 129 chars | Reject | PASS unit |
| D08 | Invalid input | All-space password | Reject | PASS unit |
| D09 | Invalid input | Different confirmation | Reject | PASS unit |
| D10 | Optional field | Empty department | Accept | PASS unit |
| D11 | Boundary | Department 101 chars | Reject | PASS unit |
| D12 | Authorization boundary | Inject role ADMIN in input object | Omit role/ID from payload | PASS adapter/unit; server authorization not tested |
| D13 | Password handling | Leading/trailing password spaces | Preserve exactly | PASS unit |
| D14 | Success | HTTP 201, empty body | Resolve registration call | PASS intercepted API |
| D15 | Business rule | HTTP 409 | Propagate duplicate error | PASS intercepted API; browser message not executed |
| D16 | Exceptions | HTTP 400/401/403/500 | Expose server failure without success | PASS intercepted API |
| D17 | Network | Fetch rejects | Readable connection error | PASS intercepted API |
| D18 | Existing workflow | Notes/feedback/technician DTOs | Existing mappings and paths unchanged | PASS intercepted API |
| D19 | Browser accessibility | Submit invalid form | Focus first invalid field and show errors | NOT RUN - browser blocked |
| D20 | Duplicate submissions | Double click submit | One pending request | NOT RUN - browser blocked |
| D21 | Live persistence | Register, sign in, refresh | New employee remains accessible | BLOCKED - endpoint absent |
| D22 | Role/ownership | Employee requests another user's incident | Server rejects | NOT RUN in this delivery |
| D23 | Live lifecycle | Resolve then verify then close | Server accepts only valid actors/states | NOT RUN in this delivery |
| D24 | Layout/themes | Narrow screen and all five themes | Readable controls, no form loss | NOT RUN in this delivery |

