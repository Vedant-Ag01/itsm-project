import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import Card from '../../components/Card'
import { getIncidents } from '../../api/incidents'

const activeStates = ['ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS']
export default function Workload() {
  const [state, setState] = useState({ items: [], phase: 'loading', error: '' })
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    getIncidents({}, { signal: controller.signal }).then(({ items }) => {
      if (!controller.signal.aborted) setState({ items, phase: 'ready', error: '' })
    }).catch((error) => {
      if (!controller.signal.aborted) setState({ items: [], phase: 'error', error: error.message })
    })
    return () => controller.abort()
  }, [attempt])
  const active = state.items.filter((item) => activeStates.includes(item.status))
  const refresh = () => { setState({ items: [], phase: 'loading', error: '' }); setAttempt((n) => n + 1) }
  return <>
    <div className="page-heading"><div><h1>Your workload</h1><p className="subtitle">Current assignments and priorities.</p></div><button className="secondary-button" onClick={refresh}>Refresh</button></div>
    {state.phase === 'loading' ? <Card><p role="status">Loading workload…</p></Card> : state.phase === 'error' ? <Card><p role="alert">{state.error}</p><button className="secondary-button" onClick={refresh}>Try again</button></Card> : <>
      <div className="stats-grid">
        {[['Active incidents', active.length], ['To acknowledge', active.filter((i) => i.status === 'ASSIGNED').length], ['In progress', active.filter((i) => i.status === 'IN_PROGRESS').length], ['Critical active', active.filter((i) => i.priority === 'P1').length]].map(([label, count]) => <section className="stat-card" key={label}><h2>{label}</h2><div className="stat-value">{count}</div></section>)}
      </div>
      <div className="dashboard-grid">
        <Card title="Active incidents by priority">
          <p className="subtitle">{active.length} active incidents assigned to you. Resolved, verified and closed incidents are excluded.</p>
          {['P1', 'P2', 'P3', 'P4'].map((priority) => {
            const count = active.filter((i) => i.priority === priority).length
            return <div className="workload-row" key={priority}><span>{priority}</span><progress aria-label={priority + ' active incidents'} value={count} max={Math.max(active.length, 1)} /><strong>{count}</strong></div>
          })}
        </Card>
        <Card title="Next to acknowledge">
          {active.filter((i) => i.status === 'ASSIGNED').length === 0 ? <p>No incidents are waiting for acknowledgment.</p> : <ul className="attention-list">{active.filter((i) => i.status === 'ASSIGNED').sort((a, b) => (a.priority ?? 'P4').localeCompare(b.priority ?? 'P4')).map((i) => <li key={i.id}><div><span className="incident-id">{i.id} · {i.priority}</span><h3>{i.title}</h3><Link className="incident-link" to={'/technician/incidents/' + encodeURIComponent(i.id)}>Open incident</Link></div></li>)}</ul>}
        </Card>
      </div>
    </>}
  </>
}
