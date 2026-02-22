import { useState } from 'react';
import Panel from '../components/Panel.jsx';
import { ROLE_OPTIONS } from '../constants.js';

export default function AuthPanel({ api, session, setSession, notify }) {
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [signupForm, setSignupForm] = useState({ username: '', password: '', role: 'RECEPTIONIST' });

  const onLogin = async (e) => {
    e.preventDefault();
    try {
      const data = await api.login(loginForm.username, loginForm.password);
      const next = {
        username: loginForm.username,
        role: data?.role || 'UNKNOWN',
        loggedIn: true
      };
      localStorage.setItem('medid_ui_session', JSON.stringify(next));
      setSession(next);
      notify('success', `Logged in as ${next.role}`);
    } catch (error) {
      notify('error', error.message);
    }
  };

  const onLogout = async () => {
    try {
      await api.logout();
    } catch {
      // ignore logout errors and clear local state anyway
    }
    localStorage.removeItem('medid_ui_session');
    setSession({ username: '', role: '', loggedIn: false });
    notify('success', 'Logged out');
  };

  const onSignup = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        username: signupForm.username,
        password: signupForm.password,
        role: signupForm.role
      };
      const res = await api.signup(payload);
      notify('success', typeof res === 'string' ? res : 'Signup successful');
    } catch (error) {
      notify('error', error.message);
    }
  };

  return (
    <Panel
      title="Access Control"
      subtitle="JWT cookie-based login for role-specific modules"
      right={session.loggedIn ? <span className="badge">{session.role}</span> : null}
    >
      <div className="auth-grid">
        <form onSubmit={onLogin} className="stack">
          <h4>Login</h4>
          <input
            placeholder="Username"
            value={loginForm.username}
            onChange={(e) => setLoginForm((s) => ({ ...s, username: e.target.value }))}
            required
          />
          <input
            type="password"
            placeholder="Password"
            value={loginForm.password}
            onChange={(e) => setLoginForm((s) => ({ ...s, password: e.target.value }))}
            required
          />
          <button type="submit">Login</button>
          {session.loggedIn ? (
            <button type="button" className="ghost" onClick={onLogout}>Logout</button>
          ) : null}
        </form>

        <form onSubmit={onSignup} className="stack">
          <h4>Quick Signup</h4>
          <input
            placeholder="Username"
            value={signupForm.username}
            onChange={(e) => setSignupForm((s) => ({ ...s, username: e.target.value }))}
            required
          />
          <input
            type="password"
            placeholder="Password"
            value={signupForm.password}
            onChange={(e) => setSignupForm((s) => ({ ...s, password: e.target.value }))}
            required
          />
          <select
            value={signupForm.role}
            onChange={(e) => setSignupForm((s) => ({ ...s, role: e.target.value }))}
          >
            {ROLE_OPTIONS.map((role) => (
              <option key={role} value={role}>{role}</option>
            ))}
          </select>
          <button type="submit">Create User (Public Signup)</button>
        </form>
      </div>
    </Panel>
  );
}
