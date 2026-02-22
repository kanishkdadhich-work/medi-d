import { useMemo, useState } from 'react';
import { createApi, getApiErrorMessage } from './api.js';
import LoginPage from './modules/LoginPage.jsx';
import ReceptionistDashboard from './modules/ReceptionistDashboard.jsx';
import DoctorDashboard from './modules/DoctorDashboard.jsx';
import PharmacistDashboard from './modules/PharmacistDashboard.jsx';
import AdminDashboard from './modules/AdminDashboard.jsx';

const STORAGE_KEY = 'medid_auth';

function loadAuth() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
    return {
      username: saved.username || '',
      role: saved.role || '',
      token: saved.token || '',
      loggedIn: Boolean(saved.loggedIn)
    };
  } catch {
    return { username: '', role: '', token: '', loggedIn: false };
  }
}

function saveAuth(auth) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
}

function roleTabs(role) {
  if (role === 'RECEPTIONIST') {
    return [
      { id: 'reception-booking', label: 'Booking' },
      { id: 'reception-all', label: 'All Appointments' }
    ];
  }
  if (role === 'DOCTOR') {
    return [
      { id: 'doctor-today', label: 'Today Queue' },
      { id: 'doctor-completed', label: 'Completed' },
      { id: 'doctor-future', label: 'Future' },
      { id: 'doctor-calendar', label: 'Calendar' }
    ];
  }
  if (role === 'PHARMACIST') {
    return [
      { id: 'pharmacy-pending', label: 'Pending Queue' },
      { id: 'pharmacy-alerts', label: 'Inventory Alerts' },
      { id: 'pharmacy-all', label: 'All Medicines' },
      { id: 'pharmacy-add', label: 'Add Medicine' }
    ];
  }
  if (role === 'ADMIN') {
    return [
      { id: 'admin', label: 'Admin' },
      { id: 'reception-booking', label: 'Booking' },
      { id: 'reception-all', label: 'All Appointments' },
      { id: 'pharmacy-pending', label: 'Pending Queue' },
      { id: 'pharmacy-alerts', label: 'Inventory Alerts' },
      { id: 'pharmacy-all', label: 'All Medicines' },
      { id: 'pharmacy-add', label: 'Add Medicine' }
    ];
  }
  return [];
}

function toastStyle(kind) {
  if (kind === 'success') return 'border-success-100 bg-success-50 text-success-600';
  if (kind === 'error') return 'border-rose-200 bg-rose-50 text-rose-700';
  return 'border-slate-200 bg-white text-slate-700';
}

export default function App() {
  const [auth, setAuth] = useState(loadAuth);
  const [activeTab, setActiveTab] = useState('');
  const [loginLoading, setLoginLoading] = useState(false);
  const [loginError, setLoginError] = useState('');
  const [toast, setToast] = useState({ type: '', text: '' });

  const api = useMemo(() => createApi(() => auth.token), [auth.token]);

  const notify = (type, text) => {
    setToast({ type, text });
    setTimeout(() => {
      setToast((prev) => (prev.text === text ? { type: '', text: '' } : prev));
    }, 3000);
  };

  const onLogin = async ({ username, password }) => {
    setLoginLoading(true);
    setLoginError('');
    try {
      const data = await api.login(username, password);
      const next = {
        username,
        role: data?.role || '',
        token: data?.token || data?.jwt || data?.accessToken || '',
        loggedIn: true
      };

      if (!next.role) {
        throw new Error('Role missing in login response.');
      }

      setAuth(next);
      saveAuth(next);
      const availableTabs = roleTabs(next.role);
      setActiveTab(availableTabs[0]?.id || '');
      notify('success', `Logged in as ${next.role}`);
    } catch (error) {
      setLoginError(getApiErrorMessage(error));
    } finally {
      setLoginLoading(false);
    }
  };

  const onLogout = async () => {
    try {
      await api.logout();
    } catch {
      // ignore backend logout error
    }
    const reset = { username: '', role: '', token: '', loggedIn: false };
    setAuth(reset);
    localStorage.removeItem(STORAGE_KEY);
    setActiveTab('');
  };

  const tabs = roleTabs(auth.role);
  const currentTab = activeTab || tabs[0]?.id || '';

  if (!auth.loggedIn) {
    return <LoginPage onLogin={onLogin} loading={loginLoading} error={loginError} />;
  }

  return (
    <main className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-med-600">MedID</p>
            <h1 className="text-xl font-semibold text-slate-900">Medical & Inventory Management</h1>
          </div>
          <div className="flex items-center gap-3">
            <span className="pill bg-med-100 text-med-700">{auth.role}</span>
            <span className="text-sm text-slate-500">{auth.username}</span>
            <button className="btn-ghost" onClick={onLogout}>Logout</button>
          </div>
        </div>
      </header>

      <div className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-6 sm:px-6 lg:grid-cols-[240px_1fr]">
        <aside className="panel p-3">
          <p className="px-2 pb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Navigation</p>
          <nav className="space-y-1">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`w-full rounded-lg px-3 py-2 text-left text-sm font-medium transition ${currentTab === tab.id ? 'bg-med-600 text-white' : 'text-slate-700 hover:bg-slate-100'}`}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </aside>

        <section className="space-y-4">
          {toast.text ? (
            <div className={`rounded-xl border px-4 py-3 text-sm ${toastStyle(toast.type)}`}>
              {toast.text}
            </div>
          ) : null}

          {currentTab === 'reception-booking' ? <ReceptionistDashboard api={api} notify={notify} view="booking" role={auth.role} /> : null}
          {currentTab === 'reception-all' ? <ReceptionistDashboard api={api} notify={notify} view="all" role={auth.role} /> : null}
          {currentTab === 'doctor-today' ? <DoctorDashboard api={api} notify={notify} view="today" /> : null}
          {currentTab === 'doctor-completed' ? <DoctorDashboard api={api} notify={notify} view="completed" /> : null}
          {currentTab === 'doctor-future' ? <DoctorDashboard api={api} notify={notify} view="future" /> : null}
          {currentTab === 'doctor-calendar' ? <DoctorDashboard api={api} notify={notify} view="calendar" /> : null}
          {currentTab === 'pharmacy-pending' ? <PharmacistDashboard api={api} notify={notify} view="pending" /> : null}
          {currentTab === 'pharmacy-alerts' ? <PharmacistDashboard api={api} notify={notify} view="alerts" /> : null}
          {currentTab === 'pharmacy-all' ? <PharmacistDashboard api={api} notify={notify} view="all" /> : null}
          {currentTab === 'pharmacy-add' ? <PharmacistDashboard api={api} notify={notify} view="add" /> : null}
          {currentTab === 'admin' ? <AdminDashboard api={api} notify={notify} /> : null}
        </section>
      </div>
    </main>
  );
}
