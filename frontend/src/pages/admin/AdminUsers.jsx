import { useState } from 'react'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'
import { createUser, getUsers, updateUser } from '../../api/admin'
import { gridStyle, LoadError, Notice, useAction, useLoad } from './adminUi'

const EMPTY = { name: '', email: '', department: '', password: '', role: 'EMPLOYEE' }

export default function AdminUsers() {
  const { data, error, reload } = useLoad(getUsers)
  const { notice, busy, run } = useAction()
  const [form, setForm] = useState(EMPTY)

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    const created = await run(() => createUser(form), `Account created for ${form.email}.`)
    if (created) {
      setForm(EMPTY)
      reload()
    }
  }

  async function toggle(user) {
    const done = await run(
      () => updateUser(user.id, { active: !user.active }),
      `${user.name} is now ${user.active ? 'disabled' : 'active'}.`,
    )
    if (done) reload()
  }

  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'email', label: 'Email' },
    { key: 'department', label: 'Department' },
    { key: 'role', label: 'Role' },
    { key: 'active', label: 'Status', render: (u) => (u.active ? 'Active' : 'Disabled') },
    {
      key: 'action',
      label: 'Action',
      render: (u) => (
        <button className="secondary-button" disabled={busy} onClick={() => toggle(u)}>
          {u.active ? 'Disable' : 'Enable'}
        </button>
      ),
    },
  ]

  return (
    <div className="workspace-stack">
      <Card title="Create account">
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
            <label className="report-field">Role
              <select value={form.role} onChange={set('role')}>
                <option value="EMPLOYEE">Employee</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </label>
          </div>
          <button className="primary-button" disabled={busy}>Create account</button>
        </form>
        <p className="report-help">Technician accounts need skills for assignment, so add them from the Technicians page.</p>
      </Card>

      <Card title="All users">
        <LoadError error={error} />
        {data && <DataTable columns={columns} rows={data} caption="All user accounts" />}
      </Card>
    </div>
  )
}
