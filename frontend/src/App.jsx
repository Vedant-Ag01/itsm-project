import { useEffect, useState } from 'react'
import './App.css'

function getInitialTheme() {
  try {
    const saved = localStorage.getItem('itsm-theme')

    if (saved === 'light' || saved === 'dark') {
      return saved
    }
  } catch {
    // The app still works when browser storage is unavailable.
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light'
}

const pages = [
  'Overview',
  'My incidents',
  'Report incident',
  'Notifications',
]

function ThemeToggle({ theme, onToggle }) {
  const nextTheme = theme === 'dark' ? 'light' : 'dark'

  return (
    <button
      className="theme-toggle"
      type="button"
      onClick={onToggle}
      aria-label={`Switch to ${nextTheme} mode`}
      title={`Switch to ${nextTheme} mode`}
    >
      <span aria-hidden="true">
        {theme === 'dark' ? '☀' : '☾'}
      </span>
      {nextTheme === 'light' ? 'Light mode' : 'Dark mode'}
    </button>
  )
}

function Sidebar({ activePage, onNavigate }) {
  return (
    <aside className="sidebar">
      <div>
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          I-ITSM
        </div>

        <p className="workspace-label">Employee workspace</p>

        <nav className="navigation" aria-label="Main navigation">
          {pages.map((page) => (
            <button
              key={page}
              type="button"
              className={`nav-item ${activePage === page ? 'active' : ''}`}
              aria-current={activePage === page ? 'page' : undefined}
              onClick={() => onNavigate(page)}
            >
              {page}
            </button>
          ))}
        </nav>
      </div>

      <div className="profile">
        <span className="avatar" aria-hidden="true">N</span>
        <div>
          <strong>Naman</strong>
          <span className="profile-role">Employee · Preview</span>
        </div>
      </div>
    </aside>
  )
}

function Overview({ onReport }) {
  const metrics = [
    'Open incidents',
    'In progress',
    'Awaiting verification',
    'Resolved this month',
  ]

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>Your workspace</h1>
          <p className="subtitle">
            Report issues. Track progress.
          </p>
        </div>

        <button
          className="primary-button"
          type="button"
          onClick={onReport}
        >
          + Report incident
        </button>
      </div>

      <div className="stats-grid">
        {metrics.map((label) => (
          <section className="stat-card" key={label}>
            <h2>{label}</h2>
            <div className="stat-value" aria-label="Not connected">
              —
            </div>
            <p>Data not connected yet</p>
          </section>
        ))}
      </div>

      <div className="dashboard-grid">
        <section className="panel">
          <div className="panel-heading">
            <h2>My incidents</h2>
            <span className="preview-badge">Layout preview</span>
          </div>

          <div className="empty-state">
            <span className="empty-symbol" aria-hidden="true">＋</span>
            <h3>Your incidents will appear here</h3>
            <p>
              We’ll connect this panel to mock incident data in a later step.
            </p>
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <h2>Incident activity</h2>
          </div>

          <div className="empty-state">
            <h3>No activity loaded</h3>
            <p>
              Incident updates and their timestamps will appear here.
            </p>
          </div>
        </section>
      </div>

      <section className="panel attention-panel">
        <h2>Needs your attention</h2>
        <p className="subtitle">
          Requests for information and resolutions awaiting verification
          will appear here once connected.
        </p>
      </section>
    </>
  )
}

function PlaceholderPage({ title }) {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Employee workspace</p>
          <h1>{title}</h1>
        </div>
      </div>

      <section className="panel empty-state">
        <h2>This page is next</h2>
        <p>
          The navigation works. We’ll build this screen after the shared
          layout is ready.
        </p>
      </section>
    </>
  )
}

export default function App() {
  const [theme, setTheme] = useState(getInitialTheme)
  const [activePage, setActivePage] = useState('Overview')

  useEffect(() => {
    document.documentElement.dataset.theme = theme

    try {
      localStorage.setItem('itsm-theme', theme)
    } catch {
      // Theme switching still works without persistence.
    }
  }, [theme])

  return (
    <div className="app-shell">
      <Sidebar
        activePage={activePage}
        onNavigate={setActivePage}
      />

      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            <span>Workspace</span>
            <span aria-hidden="true">/</span>
            <strong>{activePage}</strong>
          </div>

          <ThemeToggle
            theme={theme}
            onToggle={() =>
              setTheme((current) => current === 'dark' ? 'light' : 'dark')
            }
          />
        </header>

        <main className="main-content">
          {activePage === 'Overview' ? (
            <Overview
              onReport={() => setActivePage('Report incident')}
            />
          ) : (
            <PlaceholderPage title={activePage} />
          )}
        </main>
      </div>
    </div>
  )
}