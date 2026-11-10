export default function ThemeToggle({ theme, onToggle }) {
  const nextTheme = theme === 'dark' ? 'light' : 'dark'

  return (
    <button
      className="theme-toggle"
      type="button"
      onClick={onToggle}
      aria-label={`Switch to ${nextTheme} mode`}
    >
      <span aria-hidden="true">
        {theme === 'dark' ? '☀' : '☾'}
      </span>
      {nextTheme === 'light' ? 'Light mode' : 'Dark mode'}
    </button>
  )
}