import { useCallback, useEffect, useState } from 'react'

// Loads data once (and again when reload() is called). Shows errors instead of crashing.
export function useLoad(loader) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [tick, setTick] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    loader({ signal: controller.signal })
      .then((res) => {
        if (!controller.signal.aborted) {
          setData(res)
          setError('')
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted && err.name !== 'AbortError') setError(err.message)
      })
    return () => controller.abort()
  }, [loader, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])
  return { data, error, reload }
}

// Runs an action, then shows "done" or the server's error message.
export function useAction() {
  const [notice, setNotice] = useState(null)
  const [busy, setBusy] = useState(false)

  async function run(fn, successText) {
    setBusy(true)
    setNotice(null)
    try {
      const result = await fn()
      setNotice({ ok: true, text: successText })
      return result
    } catch (err) {
      setNotice({ ok: false, text: err.message })
      return null
    } finally {
      setBusy(false)
    }
  }

  return { notice, busy, run }
}

export function Notice({ notice }) {
  if (!notice) return null
  return (
    <div
      className={`action-notice action-notice--${notice.ok ? 'success' : 'error'}`}
      role={notice.ok ? 'status' : 'alert'}
    >
      {notice.text}
    </div>
  )
}

export function LoadError({ error }) {
  return error ? <p role="alert">Could not load this page: {error}</p> : null
}

export const gridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
  gap: '16px',
  marginBottom: '16px',
}

export const statStyle = {
  background: 'var(--surface-muted)',
  border: '1px solid var(--border)',
  borderRadius: '12px',
  padding: '16px',
}
