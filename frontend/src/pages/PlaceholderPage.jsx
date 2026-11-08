import { workspaces } from '../layouts/workspaces'

export default function PlaceholderPage({ title, role }) {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">
            {workspaces[role].label} workspace
          </p>
          <h1>{title}</h1>
        </div>
      </div>

      <section className="panel empty-state">
        <h2>This page is next</h2>
        <p>
          This route is ready. Its page content and API connection
          will be added separately.
        </p>
      </section>
    </>
  )
}