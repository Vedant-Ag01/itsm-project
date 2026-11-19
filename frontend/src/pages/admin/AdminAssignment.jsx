import { useEffect, useState } from 'react'
import Card from '../../components/Card'
import {
  getAssignmentStrategy,
  getWorkloadLimit,
  setAssignmentStrategy,
  setWorkloadLimit,
} from '../../api/admin'
import { LoadError, Notice, useAction, useLoad } from './adminUi'

const EXPLAIN = {
  SkillBased: 'Picks the technician whose skills fit the incident category, then the least busy of those.',
  LoadBased: 'Picks the technician with the fewest active incidents, whatever the category.',
}

export default function AdminAssignment() {
  const strategy = useLoad(getAssignmentStrategy)
  const limit = useLoad(getWorkloadLimit)
  const a = useAction()
  const b = useAction()
  const [chosen, setChosen] = useState('')
  const [max, setMax] = useState('')

  useEffect(() => {
    if (strategy.data) setChosen(strategy.data.current)
  }, [strategy.data])
  useEffect(() => {
    if (limit.data) setMax(String(limit.data.maxActiveIncidents))
  }, [limit.data])

  async function saveStrategy() {
    const done = await a.run(() => setAssignmentStrategy(chosen), `Automatic assignment now uses ${chosen}.`)
    if (done) strategy.reload()
  }

  async function saveLimit() {
    const done = await b.run(() => setWorkloadLimit(Number(max)), `Limit set to ${max} active incidents.`)
    if (done) limit.reload()
  }

  return (
    <div className="workspace-stack">
      <Card title="Assignment strategy">
        <Notice notice={a.notice} />
        <LoadError error={strategy.error} />
        {strategy.data && (
          <>
            <label className="report-field">Rule used by automatic assignment
              <select value={chosen} onChange={(e) => setChosen(e.target.value)}>
                {strategy.data.available.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </label>
            <p className="report-help">{EXPLAIN[chosen] || ''}</p>
            <button className="primary-button" disabled={a.busy || chosen === strategy.data.current}
              onClick={saveStrategy}>
              Apply
            </button>
          </>
        )}
      </Card>

      <Card title="Technician workload limit">
        <Notice notice={b.notice} />
        <LoadError error={limit.error} />
        {limit.data && (
          <>
            <label className="report-field">Maximum active incidents per technician
              <input type="number" min="1" value={max} onChange={(e) => setMax(e.target.value)} />
            </label>
            <p className="report-help">
              A technician at the limit is skipped by automatic assignment and cannot be assigned manually.
            </p>
            <button className="primary-button" disabled={b.busy} onClick={saveLimit}>Save limit</button>
          </>
        )}
      </Card>
    </div>
  )
}
