import Card from '../../components/Card'
import DataTable from '../../components/DataTable'
import { getTechnicians, setTechnicianAvailability } from '../../api/admin'
import { LoadError, Notice, useAction, useLoad } from './adminUi'

export default function AdminTechnicians() {
  const { data, error, reload } = useLoad(getTechnicians)
  const { notice, busy, run } = useAction()

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
      <Card title="Technicians">
        <Notice notice={notice} />
        <LoadError error={error} />
        {data && <DataTable columns={columns} rows={data} caption="Technicians and availability" />}
        <p className="report-help">An unavailable technician is skipped by automatic assignment.</p>
      </Card>
    </div>
  )
}
