import { useRef, useState } from 'react'
import { Link } from 'react-router'
import Card from '../../components/Card'
import { createIncident } from '../../api/incidents'

const CATALOGUE_SERVICES = [
  'Campus WiFi',
  'VPN Gateway',
  'Payroll System',
  'Student Portal',
  'Identity Provider',
  'SSO Login',
  'Staff Email',
  'File Server',
  'Cloud Storage',
  'Lab Printing',
  'Classroom AV',
]

const empty = { title: '', description: '', serviceName: '', criticality: '', impact: '', urgency: '', affectedUsers: '1' }
export default function LiveReportIncident() {
  const [form, setForm] = useState(empty)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const lock = useRef(false)
  function change(event) { setForm({ ...form, [event.target.name]: event.target.value }); setError('') }
  async function submit(event) {
    event.preventDefault()
    if (lock.current) return
    if (!form.title.trim() || !form.description.trim() || !form.serviceName.trim()) { setError('Title, description and service must contain text.'); return }
    const affectedUsers = Number(form.affectedUsers)
    if (!Number.isSafeInteger(affectedUsers) || affectedUsers < 1) { setError('Affected users must be a positive whole number.'); return }
    lock.current = true; setBusy(true); setError('')
    try {
      setResult(await createIncident({ ...form, title: form.title.trim(), description: form.description.trim(), serviceName: form.serviceName.trim(), affectedUsers }))
    } catch (failure) { setError(failure.message) }
    finally { lock.current = false; setBusy(false) }
  }
  if (result) return <Card title="Incident reported">
    <p role="status">Your incident {result.incident.id} has been created.</p>
    <Link className="primary-button" to={`/employee/incidents/${encodeURIComponent(result.incident.id)}`}>View incident</Link>
    {(result.possibleDuplicateIds ?? []).length > 0 && <><h2>Possible duplicates</h2><p>Your incident was created. Support can review these potentially related incidents.</p><ul>{result.possibleDuplicateIds.map((id) => <li key={id}>{id}</li>)}</ul></>}
    <div className="report-actions"><button className="theme-toggle" onClick={() => { setForm({ ...empty }); setResult(null) }}>Report another</button></div>
  </Card>
  return <><div className="page-heading"><div><h1>Report incident</h1><p className="subtitle">Describe the service interruption.</p></div></div>
    <Card title="Incident information"><form onSubmit={submit}>
      <fieldset className="report-fields" disabled={busy}>
        <label className="report-field">Title<input name="title" required value={form.title} onChange={change} /></label>
        <label className="report-field">Description<textarea name="description" required rows={5} value={form.description} onChange={change} /></label>
        <div className="report-form-grid">
          <label className="report-field">
            Affected service
            <select name="serviceName" required value={form.serviceName} onChange={change}>
              <option value="">Choose a service</option>
              {CATALOGUE_SERVICES.map((service) => <option key={service} value={service}>{service}</option>)}
            </select>
          </label>
          {['criticality', 'impact', 'urgency'].map((field) => <label className="report-field" key={field}>
            {field === 'criticality' ? 'Service business criticality' : field === 'impact' ? 'Impact' : 'Urgency'}
            <select name={field} required value={form[field]} onChange={change}><option value="">Choose a level</option>{['LOW', 'MEDIUM', 'HIGH'].map((level) => <option key={level} value={level}>{level}</option>)}</select>
            {field === 'criticality' && <span className="report-help">How essential is this service to business operations? Ask support if unsure.</span>}
          </label>)}
          <label className="report-field">Affected users<input name="affectedUsers" type="number" min="1" step="1" required value={form.affectedUsers} onChange={change} /></label>
        </div>
        <button className="primary-button" type="submit">{busy ? 'Submitting…' : 'Submit incident'}</button>
      </fieldset>
      {error && <p role="alert">{error} Your entered values have been preserved.</p>}
    </form></Card>
  </>
}
