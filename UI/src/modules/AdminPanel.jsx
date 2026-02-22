import { useState } from 'react';
import Panel from '../components/Panel.jsx';
import { ROLE_OPTIONS } from '../constants.js';

function pretty(data) {
  if (data == null) return '';
  return JSON.stringify(data, null, 2);
}

export default function AdminPanel({ api, notify }) {
  const [newUser, setNewUser] = useState({ username: '', password: '', role: 'DOCTOR' });
  const [result, setResult] = useState(null);

  const run = async (fn) => {
    try {
      const data = await fn();
      setResult(data);
      notify('success', 'Request completed');
    } catch (error) {
      notify('error', error.message);
    }
  };

  return (
    <Panel title="Admin Center" subtitle="User provisioning + pharmacy daily reporting">
      <div className="grid-2">
        <form
          className="stack"
          onSubmit={(e) => {
            e.preventDefault();
            run(() => api.createUserByAdmin(newUser));
          }}
        >
          <h4>Create User</h4>
          <input placeholder="Username" value={newUser.username} onChange={(e) => setNewUser((s) => ({ ...s, username: e.target.value }))} required />
          <input type="password" placeholder="Password" value={newUser.password} onChange={(e) => setNewUser((s) => ({ ...s, password: e.target.value }))} required />
          <select value={newUser.role} onChange={(e) => setNewUser((s) => ({ ...s, role: e.target.value }))}>
            {ROLE_OPTIONS.map((role) => (
              <option key={role} value={role}>{role}</option>
            ))}
          </select>
          <button type="submit">POST /api/admin/users/create</button>
        </form>

        <div className="stack">
          <h4>Daily Dispense Summary</h4>
          <button type="button" onClick={() => run(() => api.dailySummary())}>GET /api/pharmacy/reports/daily-summary</button>
        </div>
      </div>

      <pre className="result-box">{pretty(result)}</pre>
    </Panel>
  );
}
