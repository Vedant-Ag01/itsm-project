import { Link } from 'react-router'

export default function EmployeeOverview() {
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
          <p className="subtitle">Report issues. Track progress.</p>
        </div>

        <Link className="primary-button" to="/employee/report">
          + Report incident
        </Link>
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