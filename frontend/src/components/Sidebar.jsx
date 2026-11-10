import { NavLink } from 'react-router'
import { pageUrl, workspaces } from '../layouts/workspaces'

export default function Sidebar({ role }) {
  const workspace = workspaces[role]

  return (
    <aside className="sidebar">
      <div>
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          I-ITSM
        </div>

        <p className="workspace-label">
          {workspace.label} workspace
        </p>

        <nav className="navigation" aria-label="Main navigation">
          {workspace.pages.map((page) => (
            <NavLink
              key={page.path}
              to={pageUrl(role, page.path)}
              end={page.path === ''}
              className={({ isActive }) =>
                `nav-item ${isActive ? 'active' : ''}`
              }
            >
              {page.label}
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="profile">
        <span className="avatar" aria-hidden="true">P</span>
        <div>
          <strong>Layout preview</strong>
          <span className="profile-role">{workspace.label}</span>
        </div>
      </div>
    </aside>
  )
}