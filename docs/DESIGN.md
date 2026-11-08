# I-ITSM Design Guide

Status: agreed visual direction; implementation details may be refined.
Owner: Naman — shared frontend, employee screens and documentation.

## 1. Visual direction

A professional incident-management workspace with clear typography,
readable tables, restrained colour and consistent spacing.

Light theme: warm off-white background, white cards and burnt-orange accents.
Dark theme: warm charcoal background, slightly lighter panels and copper-orange accents.

Both themes use the same layout, components and information hierarchy.

Avoid neon colours, glowing borders, decorative gradients, game-style
typography and large promotional sections inside the application.

## 2. Colour tokens

These are implementation starting values based on the approved reference.
Check actual text, controls and badges for contrast in both themes.

| Token | Light | Dark |
| --- | --- | --- |
| background | #F5F2ED | #181716 |
| surface | #FFFFFF | #23211F |
| surface-muted | #EEE9E2 | #2D2926 |
| text-primary | #24211E | #F3F0EB |
| text-secondary | #625B54 | #BDB5AD |
| border | #DDD6CE | #49413A |
| action | #B94715 | #A9502C |
| action-hover | #983910 | #BA5932 |
| action-text | #FFFFFF | #FFFFFF |
| accent-text | #A63C10 | #E8A180 |
| selected-background | #F9E4D8 | #382820 |
| focus-ring | #A63C10 | #E8A180 |

Use shared CSS custom properties rather than hardcoding colours in pages.
Use action colours for filled buttons and accent-text for links on surfaces.

Status and priority colours are semantic:

- Critical / errors: red.
- High / warning: orange or amber.
- Medium: muted amber.
- Low / success: muted green.
- Assigned / in progress: muted blue.
- Neutral or inactive states: grey.

Every badge includes a text label. Colour alone must not convey meaning.

## 3. Typography

Primary font: Inter.
Fallback: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif.

Use proportional sans-serif typography for headings, navigation, forms and tables.
Monospace is optional for short incident IDs only.

- Page heading: 32px, weight 500, line-height 1.2.
- Section heading: 20px, weight 600, line-height 1.3.
- Body and controls: 14–16px, weight 400.
- Labels and buttons: 14px, weight 500.
- Supporting metadata: 12–13px, with readable contrast.
- Summary numbers: 32px, weight 400–500, tabular numerals.
- Body line-height: approximately 1.5.

Avoid oversized bold headings and widely spaced uppercase body text.

## 4. Layout

Desktop:

- Left sidebar approximately 240px wide.
- Top bar with breadcrumbs, theme toggle, notifications and user menu.
- Main content padding: 24–32px.
- Four summary cards across the employee dashboard.
- Incident table in the wider main column.
- Incident activity in the narrower right column.
- Secondary information below.

Use an 8px spacing rhythm, with 4px for small internal adjustments.
Typical gaps: 8, 16, 24 and 32px.
Cards: 12px corner radius, subtle border, approximately 24px padding.
Inputs and buttons: 8px corner radius.

Tablet: summary cards form two columns; main panels stack when needed.
Mobile: collapsible navigation, one-column panels, labelled controls.
Wide tables may scroll horizontally within their own container.

## 5. Shared components

- AppShell and role-specific navigation
- ThemeToggle
- Button
- Card and StatCard
- FormField, Input, Select and Textarea
- StatusBadge, PriorityBadge and SlaBadge
- DataTable
- EmptyState, LoadingState and ErrorState
- IncidentTimeline

Coordinate shared component changes with Naman.
Role-specific pages should reuse these components.

## 6. Themes

Place a sun/moon toggle in the top bar.
Default to the saved preference; otherwise use the system preference.
Remember the selection in localStorage under "itsm-theme".

Switch colours without changing page layout, data or form contents.
Give the toggle an accessible label and visible keyboard focus.
Both themes must style charts, menus, forms, badges and error states.

## 7. Interaction and accessibility

- Clear hover, focus, active and disabled states.
- Short transitions of approximately 150–200ms.
- Respect reduced-motion preferences.
- Visible labels for form fields.
- Errors displayed beside the relevant field where possible.
- Preserve entered values after a failed submission.
- Disable duplicate submissions while a request is pending.
- Use proper buttons, links and table headers.
- Aim for at least 4.5:1 contrast for normal text.
- Charts include meaningful labels and a text summary.
- Include loading, empty and failure states for data-driven screens.

## 8. Roles and API alignment

Employee navigation:
Overview, My incidents, Report incident, Notifications.

Technician navigation:
Assigned incidents, Workload, Notifications.

Admin navigation:
Overview, Incidents, Users, Technicians, Services, SLA policies,
Assignment strategy, Reports, Audit logs, SLA violations.

Backend authorization remains authoritative.
Hiding a frontend control is not an authorization mechanism.

Use docs/api_examples.md as the API contract.

Official incident states:
NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS,
RESOLVED, VERIFIED, CLOSED.

Display friendly labels while preserving the exact API values.
Do not introduce "Awaiting reply" as a lifecycle state.
Only show a reply-needed indicator when supported by backend data.

Employees can verify their resolved incidents.
Closing an incident is an admin action.

Reports endpoints are documented as admin-only.
Employee dashboard summaries must use the employee's own incident data.

## 9. Mock data and integration

Mock data must match the documented request and response shapes.
Keep mocks separate from components and access them through the API helper.

Do not treat visual mockup numbers, dates or messages as requirements.
Never show fabricated metrics as live data.

Confirm with Vedant:

- How employees retrieve available services.
- Whether incident category is inferred or submitted.
- How requests for additional information are represented.
- Which data supports employee resolution trends.
- How timestamps without timezone offsets should be interpreted.

## 10. References

Visual inspiration: https://styles.refero.design/

- Ventriloc: warm neutral surfaces and analytics presentation.
- Sauce Labs: hierarchy and structured layout.
- Axiom: restrained dark surfaces.
- Ui / shadcn-style reference: consistent controls and cards.

The approved team mockup sets the final direction.
Reference-brand instructions do not override this guide.