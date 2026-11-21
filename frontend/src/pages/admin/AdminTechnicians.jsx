import { useState } from 'react'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'
import { createTechnician, getTechnicians, setTechnicianAvailability } from '../../api/admin'
import { gridStyle, LoadError, Notice, useAction, useLoad } from './adminUi'

const SKILLS = [
  'HARDWARE',
  'SOFTWARE',
  'NETWORK',
  'DATABASE',
  'CLOUD',
  'SECURITY',
  'AUTHENTICATION',
  'APPLICATION',
  'STORAGE',
]

const EMPTY = { name: '', email: '', department: 'IT', password: '', skills: [] }

export default function AdminTechnicians() {
  const { data, error, reload } = useLoad(getTechnicians)
  const { notice, busy, run } = useAction()
  const [form, setForm] = useState(EMPTY)

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  function toggleSkill(skill) {
    setForm((f) => ({
      ...f,
      skills: f.skills.includes(skill)
        ? f.skills.filter((s) => s !== skill)
        : [...f.skills, skill],
    }))
  }

  async function submit(e) {
    e.preventDefault()
    const created = await run(() => createTechnician(form), `Technician ${form.name} added.`)
    if (created) {
      setForm(EMPTY)
      reload()
    }
  }

  async function toggle(t) {
    const done = await run(
      () => setTechnicianAvailability(t.id, !t.available),
      `${t.name} is now ${t.available ? 'unavailable' : 'available'}.`,
    )
    if (done) reload()
  }

  const columns = [
    { key: 'name', label: 'Technician' },
    { key: 'email', label: 'Email' },
    { key: 'skills', label: 'Skills', render: (t) => t.skills.join(', ') || '—' },
    { key: 'activeIncidents', label: 'Active incidents' },
    { key: 'available', label: 'Availability', render: (t) => (t.available ? 'Available' : 'Unavailable') },
    {
      key: 'action',
      label: 'Action',
      render: (t) => (
        <button className="secondary-button" disabled={busy} onClick={() => toggle(t)}>
          {t.available ? 'Mark unavailable' : 'Mark available'}
        </button>
      ),
    },
  ]

  return (
    <div className="workspace-stack">
      <Card title="Add technician">
        <Notice notice={notice} />
        <form onSubmit={submit}>
          <div style={gridStyle}>
            <label className="report-field">Name
              <input required value={form.name} onChange={set('name')} />
            </label>
            <label className="report-field">Email
              <input required type="email" value={form.email} onChange={set('email')} />
            </label>
            <label className="report-field">Department
              <input value={form.department} onChange={set('department')} />
            </label>
            <label className="report-field">Password (min 8)
              <input required type="password" minLength={8} value={form.password} onChange={set('password')} />
            </label>
          </div>
          <fieldset className="report-field">
            <legend>Skills</legend>
            {SKILLS.map((skill) => (
              <label key={skill} style={{ marginRight: '1rem' }}>
                <input
                  type="checkbox"
                  checked={form.skills.includes(skill)}
                  onChange={() => toggleSkill(skill)}
                />{' '}
                {skill}
              </label>
            ))}
          </fieldset>
          <button className="primary-button" disabled={busy || form.skills.length === 0}>
            Add technician
          </button>
        </form>
        <p className="report-help">The skills decide which incidents skill-based assignment sends to this technician.</p>
      </Card>

      <Card title="Technicians">
        <LoadError error={error} />
        {data && <DataTable columns={columns} rows={data} caption="Technicians and availability" />}
        <p className="report-help">An unavailable technician is skipped by automatic assignment.</p>
      </Card>
    </div>
  )
}
