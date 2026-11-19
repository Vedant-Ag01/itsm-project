import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '../api/c7'

export default function NotificationBell({ role }) {
  const [open, setOpen] = useState(false)
  const [data, setData] = useState({ unread: 0, notifications: [] })
  const [error, setError] = useState('')
  const containerRef = useRef(null)

  useEffect(() => {
    let controller = new AbortController()
    setError('')

    function load() {
      controller.abort()
      controller = new AbortController()
      const current = controller
      getNotifications({ signal: current.signal })
        .then((res) => {
          if (!current.signal.aborted) {
            setData({
              unread: res.unread ?? 0,
              notifications: Array.isArray(res.notifications) ? res.notifications : [],
            })
          }
        })
        .catch((err) => {
          if (!current.signal.aborted) setError(err.message)
        })
    }

    load()
    // Check for new alerts every 15 seconds, and again when the tab is focused.
    const timer = setInterval(load, 15000)
    window.addEventListener('focus', load)
    return () => {
      controller.abort()
      clearInterval(timer)
      window.removeEventListener('focus', load)
    }
  }, [role])

  useEffect(() => {
    function handleOutsideClick(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false)
      }
    }

    function handleEscape(event) {
      if (event.key === 'Escape') setOpen(false)
    }

    document.addEventListener('mousedown', handleOutsideClick)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [])

  async function handleMarkRead(id) {
    await markNotificationRead(id)
    setData((prev) => ({
      unread: Math.max(0, prev.unread - 1),
      notifications: prev.notifications.map((item) =>
        item.id === id ? { ...item, read: true } : item,
      ),
    }))
  }

  async function handleMarkAllRead() {
    await markAllNotificationsRead()
    setData((prev) => ({
      unread: 0,
      notifications: prev.notifications.map((item) => ({ ...item, read: true })),
    }))
  }

  const allNotificationsRoute = role === 'admin' ? '/admin/audit' : `/${role}/notifications`

  return (
    <div ref={containerRef} style={{ position: 'relative' }}>
      <button
        type="button"
        className="theme-toggle"
        aria-label={
          data.unread > 0
            ? `Notifications (${data.unread} unread)`
            : 'Notifications (no unread alerts)'
        }
        aria-expanded={open}
        aria-haspopup="dialog"
        onClick={() => setOpen((prev) => !prev)}
        style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        <span>Alerts</span>
        {data.unread > 0 && (
          <span
            style={{
              background: 'var(--action)',
              color: 'var(--action-text)',
              borderRadius: '999px',
              padding: '0 6px',
              fontSize: '12px',
              fontWeight: 600,
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {data.unread}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Recent notifications"
          style={{
            position: 'absolute',
            right: 0,
            top: 'calc(100% + 8px)',
            width: '360px',
            maxWidth: '90vw',
            maxHeight: '420px',
            overflowY: 'auto',
            background: 'var(--surface)',
            color: 'var(--text-primary)',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            padding: '16px',
            zIndex: 50,
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.14)',
          }}
        >
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '12px',
            }}
          >
            <strong style={{ fontSize: '15px' }}>
              Notifications ({data.unread} unread)
            </strong>
            {data.unread > 0 && (
              <button
                type="button"
                className="secondary-button"
                onClick={handleMarkAllRead}
              >
                Mark all read
              </button>
            )}
          </div>

          {error && <p role="alert">{error}</p>}

          {data.notifications.length === 0 ? (
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', margin: '8px 0' }}>
              You have no notifications yet.
            </p>
          ) : (
            <ul
              style={{
                listStyle: 'none',
                padding: 0,
                margin: 0,
                display: 'grid',
                gap: '8px',
              }}
            >
              {data.notifications.slice(0, 6).map((n) => (
                <li
                  key={n.id}
                  style={{
                    background: n.read ? 'var(--surface)' : 'var(--surface-muted)',
                    border: '1px solid var(--border)',
                    borderRadius: '8px',
                    padding: '12px',
                  }}
                >
                  <div style={{ fontWeight: 500, fontSize: '14px', marginBottom: '4px' }}>
                    {n.subject}
                  </div>
                  <div
                    style={{
                      fontSize: '13px',
                      color: 'var(--text-secondary)',
                      marginBottom: '8px',
                    }}
                  >
                    {n.message}
                  </div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                      {n.read ? 'Read' : 'Unread'}
                    </span>
                    {!n.read && (
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => handleMarkRead(n.id)}
                      >
                        Mark read
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}

          <div
            style={{
              marginTop: '12px',
              paddingTop: '8px',
              borderTop: '1px solid var(--border)',
              textAlign: 'right',
              fontSize: '13px',
            }}
          >
            <Link
              to={allNotificationsRoute}
              style={{ color: 'var(--accent-text)', fontWeight: 500 }}
              onClick={() => setOpen(false)}
            >
              {role === 'admin' ? 'View system audit logs →' : 'View all notifications →'}
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
