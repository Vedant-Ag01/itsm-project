import { useState } from 'react'
import Card from '../../components/Card'
import { getSlaPolicies, updateSlaPolicy } from '../../api/admin'
import { LoadError, Notice, useAction, useLoad } from './adminUi'

function PolicyRow({ policy, onSaved }) {
  const [response, setResponse] = useState(policy.responseMinutes)
  const [resolution, setResolution] = useState(policy.resolutionMinutes)
  const { notice, busy, run } = useAction()

  async function save() {
    const done = await run(
      () => updateSlaPolicy(policy.priority, {
        responseMinutes: Number(response),
        resolutionMinutes: Number(resolution),
      }),
      `${policy.priority} saved.`,
    )
    if (done) onSaved()
  }

  return (
    <tr>
      <th scope="row">{policy.priority}</th>
      <td>
        <input type="number" min="1" aria-label={`${policy.priority} response minutes`}
          value={response} onChange={(e) => setResponse(e.target.value)} />
      </td>
      <td>
        <input type="number" min="1" aria-label={`${policy.priority} resolution minutes`}
          value={resolution} onChange={(e) => setResolution(e.target.value)} />
      </td>
      <td>
        <button className="secondary-button" disabled={busy} onClick={save}>Save</button>
        {notice && <span role={notice.ok ? 'status' : 'alert'}> {notice.text}</span>}
      </td>
    </tr>
  )
}

export default function AdminSlaPolicies() {
  const { data, error, reload } = useLoad(getSlaPolicies)

  return (
    <div className="workspace-stack">
      <Card title="SLA policies">
        <LoadError error={error} />
        {data && (
          <div className="table-scroll">
            <table className="data-table">
              <caption className="sr-only">SLA targets per priority</caption>
              <thead>
                <tr>
                  <th scope="col">Priority</th>
                  <th scope="col">Response (minutes)</th>
                  <th scope="col">Resolution (minutes)</th>
                  <th scope="col">Action</th>
                </tr>
              </thead>
              <tbody>
                {data.map((p) => (
                  <PolicyRow key={`${p.priority}-${p.responseMinutes}-${p.resolutionMinutes}`}
                    policy={p} onSaved={reload} />
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="report-help">
          Response must not be longer than resolution. New targets apply to incidents from now on
          and are used by the SLA monitor straight away.
        </p>
      </Card>
    </div>
  )
}
