import { useEffect, useRef, useState } from 'react'

const values = ['light', 'soft', 'dim', 'dark', 'midnight']
const labels = ['Light', 'Soft light', 'Dim', 'Dark', 'Midnight']
export default function ThemeToggle({ theme, onToggle }) {
  const [open, setOpen] = useState(false)
  const root = useRef(null)
  const trigger = useRef(null)
  useEffect(() => {
    if (!open) return
    function outside(event) {
      if (!root.current?.contains(event.target)) setOpen(false)
    }
    function escape(event) {
      if (event.key === 'Escape') { setOpen(false); trigger.current?.focus() }
    }
    document.addEventListener('pointerdown', outside)
    document.addEventListener('keydown', escape)
    return () => {
      document.removeEventListener('pointerdown', outside)
      document.removeEventListener('keydown', escape)
    }
  }, [open])
  return <div className="appearance" ref={root}>
    <button ref={trigger} type="button" className="theme-toggle" aria-expanded={open} onClick={() => setOpen(!open)}>Appearance</button>
    {open && <section className="appearance-panel" aria-label="Appearance settings">
      <strong>Appearance</strong>
      <label className="appearance-label">Brightness
        <input type="range" min="0" max="4" step="1" value={Math.max(0, values.indexOf(theme))}
          aria-valuetext={theme === 'system' ? 'System preference' : labels[values.indexOf(theme)]}
          onChange={(event) => onToggle(values[Number(event.target.value)])} />
      </label>
      <div className="appearance-stops">{values.map((value, index) => <button type="button" key={value} aria-pressed={theme === value} onClick={() => onToggle(value)}>{labels[index]}</button>)}</div>
      <label className="system-theme"><input type="checkbox" checked={theme === 'system'} onChange={(event) => onToggle(event.target.checked ? 'system' : 'light')} /> Use system theme</label>
    </section>}
  </div>
}
