import { Link } from 'react-router'

export default function NotFound({ home = '/employee' }) {
  return (
    <section className="panel empty-state">
      <h1>Page not found</h1>
      <p>Check the address or return to your workspace.</p>

      <Link className="text-link" to={home}>
        Return to workspace
      </Link>
    </section>
  )
}