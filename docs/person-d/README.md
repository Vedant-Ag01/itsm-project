# Person D delivery

Start with INTEGRATION.md for completed work, actual test results and the backend dependencies.

Report-ready sections:
1. 01-PROBLEM-ANALYSIS.md
2. 02-REQUIREMENTS.md
3. use-case-diagram.svg (rendered UML) and use-cases.puml (editable source)
4. 03-TECHNOLOGY-SELECTION.md (all nine required answers)
5. 04-DEMO-SCRIPT.md (18-minute plan and three complete workflows)
6. 05-MODULE-CONTRIBUTION.md (report paragraph, introduction and viva notes)

D-Report.pdf combines these sections and the integration/validation record. It is D's contribution to the single group PDF required by the brief. The team must supply architecture, class diagram, ER/schema/sample data, executed system tests and other members' paragraphs. The demo script is not a recorded video.

Run from frontend:

    npm ci
    npm run lint
    npm run build
    node --test tests/registration.test.mjs
    node check-registration-disabled.mjs
    node check-registration.mjs
    node check-api.mjs

Registration is disabled by default because the uploaded backend has no registration route. See INTEGRATION.md before enabling it. No new npm dependency is needed for the shipped tests.
