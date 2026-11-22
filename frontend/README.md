# I-ITSM frontend

React 19 + Vite. See the README in the project root for how to run the whole system.

```
npm install
npm run dev      # development server
npm run build    # production build
npm run lint     # eslint
```

Copy `.env.example` to `.env.local` so the app talks to the backend:

```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCKS=false
```

Without it the app runs on mock data.
