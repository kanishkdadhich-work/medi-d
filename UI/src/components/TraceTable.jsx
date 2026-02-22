function brief(payload) {
  if (payload == null) return '-';
  if (typeof payload === 'string') return payload;
  try {
    return JSON.stringify(payload);
  } catch {
    return String(payload);
  }
}

export default function TraceTable({ traces }) {
  return (
    <div className="trace-wrap">
      <table className="trace-table">
        <thead>
          <tr>
            <th>Time</th>
            <th>Request</th>
            <th>Status</th>
            <th>Request Body</th>
            <th>Response</th>
          </tr>
        </thead>
        <tbody>
          {traces.length === 0 ? (
            <tr>
              <td colSpan="5" className="muted">No API activity yet.</td>
            </tr>
          ) : (
            traces.map((trace) => (
              <tr key={trace.id}>
                <td>{new Date(trace.time).toLocaleTimeString()}</td>
                <td>
                  <code>{trace.method}</code> <span>{trace.url}</span>
                </td>
                <td>
                  <span className={`status ${trace.ok ? 'ok' : 'bad'}`}>
                    {trace.status}
                  </span>
                </td>
                <td><pre>{brief(trace.requestBody)}</pre></td>
                <td><pre>{brief(trace.responseBody)}</pre></td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
