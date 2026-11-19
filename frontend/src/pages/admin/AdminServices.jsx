import { useState } from 'react'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'
import { createService, getServices, updateService } from '../../api/admin'
import { gridStyle, LoadError, Notice, useAction, useLoad } from './adminUi'

const LEVELS = ['HIGH', 'MEDIUM', 'LOW']
const EMPTY = { name: '', description: '', criticality: 'MEDIUM' }

export default function AdminServices() {
  const { data, error, reload } = useLoad(getServices)
  const { notice, busy, run } = useAction()
  const [form, setForm] = useState(EMPTY)

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    const created = await run(() => createService(form), `Service "${form.name}" added.`)
    if (created) {
      setForm(EMPTY)
      reload()
    }
  }

  async function change(service, patch, text) {
    const done = await run(() => updateService(service.id, patch), text)
    if (done) reload()
  }

  const columns = [
    { key: 'name', label: 'Service' },
    { key: 'description', label: 'Description' },
    {
      key: 'criticality',
      label: 'Business criticality',
      render: (s) => (
        <select
          value={s.criticality}
          disabled={busy}
          aria-label={`Criticality of ${s.name}`}
          onChange={(e) =>
            change(s, { criticality: e.target.value }, `${s.name} is now ${e.target.value}.`)
          }
        >
          {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
        </select>
      ),
    },
    { key: 'active', label: 'Status', render: (s) => (s.active ? 'Active' : 'Switched off') },
    {
      key: 'action',
      label: 'Action',
      render: (s) => (
        <button
          className="secondary-button"
          disabled={busy}
          onClick={() => change(s, { active: !s.active }, `${s.name} ${s.active ? 'switched off' : 'switched on'}.`)}
        >
          {s.active ? 'Switch off' : 'Switch on'}
        </button>
      ),
    },
  ]

  return (
    <div className="workspace-stack">
      <Card title="Add a service">
        <Notice notice={notice} />
        <form onSubmit={submit}>
          <div style={gridStyle}>
            <label className="report-field">Name
              <input required value={form.name} onChange={set('name')} />
            </label>
            <label className="report-field">Description
              <input value={form.description} onChange={set('description')} />
            </label>
            <label className="report-field">Business criticality
              <select value={form.criticality} onChange={set('criticality')}>
                {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
              </select>
            </label>
          </div>
          <button className="primary-button" disabled={busy}>Add service</button>
        </form>
        <p className="report-help">
          Criticality decides how urgent new incidents on this service are (it feeds the priority).
        </p>
      </Card>

      <Card title="Service catalogue">
        <LoadError error={error} />
        {data && <DataTable columns={columns} rows={data} caption="Service catalogue" />}
      </Card>
    </div>
  )
}
