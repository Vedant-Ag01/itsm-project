export default function DataTable({
  columns,
  rows,
  rowKey = 'id',
  caption,
  emptyMessage = 'No matching records.',
}) {
  if (rows.length === 0) {
    return (
      <div className="empty-state">
        <h3>{emptyMessage}</h3>
        <p>Try changing your search or filters.</p>
      </div>
    )
  }

  return (
    <div
      className="table-scroll"
      role="region"
      aria-label={caption}
      tabIndex={0}
    >
      <table className="data-table">
        <caption className="sr-only">{caption}</caption>

        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col">
                {column.label}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {rows.map((row) => (
            <tr key={row[rowKey]}>
              {columns.map((column) => (
                <td key={column.key}>
                  {column.render
                    ? column.render(row)
                    : (row[column.key] ?? '—')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}