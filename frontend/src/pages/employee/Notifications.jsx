import { useEffect, useState } from 'react'
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '../../api/c7'
import Card from '../../components/Card'

export default function Notifications() {
  const [data, setData] = useState({ unread: 0, notifications: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load(signal) {
    setLoading(true)
    setError('')
    try {
      const res = await getNotifications({ signal })
      if (!signal?.aborted) setData(res)
    } catch (err) {
      if (!signal?.aborted) setError(err.message)
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [])

  async function handleRead(id) {
    await markNotificationRead(id)
    setData((prev) => ({
      unread: Math.max(0, prev.unread - 1),
      notifications: prev.notifications.map((n) => (n.id === id ? { ...n, read: true } : n)),
    }))
  }

  async function handleReadAll() {
    await markAllNotificationsRead()
    setData((prev) => ({
      unread: 0,
      notifications: prev.notifications.map((n) => ({ ...n, read: true })),
    }))
  }

  return (
    <Card
      title={`Notifications (${data.unread} unread)`}
      subtitle="Alerts for incident assignments, status transitions, info requests, and SLA warnings."
    >
      {data.unread > 0 && (
        <div style={{ marginBottom: '1rem' }}>
          <button type="button" className="secondary-button" onClick={handleReadAll}>
            Mark all as read
          </button>
        </div>
      )}

      {error && <p role="alert">{error}</p>}
      {loading ? (
        <p>Loading notifications…</p>
      ) : data.notifications.length === 0 ? (
        <div className="empty-state">
          <h3>No notifications yet</h3>
          <p>You will see alerts here when your incidents are updated.</p>
        </div>
      ) : (
        <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: '0.75rem' }}>
          {data.notifications.map((n) => (
            <li
              key={n.id}
              className="panel"
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: '1rem',
                opacity: n.read ? 0.7 : 1,
              }}
            >
              <div>
                <strong>{n.subject}</strong>
                <p style={{ margin: '0.25rem 0' }}>{n.message}</p>
                <small>{n.createdAt}</small>
              </div>
              {!n.read && (
                <button type="button" className="secondary-button" onClick={() => handleRead(n.id)}>
                  Mark read
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}
