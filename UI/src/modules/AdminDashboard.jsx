import { useEffect, useState } from 'react';
import AsyncState from '../components/AsyncState.jsx';
import Modal from '../components/Modal.jsx';
import Pagination from '../components/Pagination.jsx';
import { getApiErrorMessage } from '../api.js';

const ROLE_OPTIONS = ['ADMIN', 'DOCTOR', 'PHARMACIST', 'RECEPTIONIST'];
const PAGE_SIZE = 8;

function normalizeRole(roleValue) {
  if (!roleValue) return 'RECEPTIONIST';
  return String(roleValue).replace('ROLE_', '').toUpperCase();
}

export default function AdminDashboard({ api, notify }) {
  const [users, setUsers] = useState([]);
  const [overview, setOverview] = useState(null);
  const [overviewError, setOverviewError] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState({ username: '', password: '', role: 'DOCTOR', doctorId: '', specialization: '' });

  const [editOpen, setEditOpen] = useState(false);
  const [editUser, setEditUser] = useState(null);
  const [editing, setEditing] = useState(false);
  const [page, setPage] = useState(1);

  const loadUsers = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getStaffUsers();
      const normalized = (Array.isArray(data) ? data : []).map((item) => ({
        id: item.id,
        username: item.username || item.userName || 'unknown',
        role: normalizeRole(item.role),
        enabled: item.enabled !== false,
        doctorId: item.doctorId ?? '',
        specialization: item.specialization ?? ''
      }));
        setUsers(normalized);
    } catch (err) {
      setError(getApiErrorMessage(err));
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const loadOverview = async () => {
    setOverviewError('');
    try {
      const data = await api.getAdminOverview();
      setOverview(data);
    } catch (err) {
      setOverviewError(getApiErrorMessage(err));
      setOverview(null);
    }
  };

  useEffect(() => {
    loadUsers();
    loadOverview();
  }, []);

  useEffect(() => {
    setPage(1);
  }, [users.length]);

  const pagedUsers = users.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const onCreateUser = async (e) => {
    e.preventDefault();
    setCreating(true);
    try {
      await api.createUser({
        ...form,
        role: form.role,
        doctorId: form.doctorId === '' ? undefined : Number(form.doctorId),
        specialization: form.specialization || undefined
      });
      setForm({ username: '', password: '', role: 'DOCTOR', doctorId: '', specialization: '' });
      notify('success', 'User created successfully.');
      await Promise.all([loadUsers(), loadOverview()]);
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    } finally {
      setCreating(false);
    }
  };

  const openEdit = (user) => {
    setEditUser({
      id: user.id,
      username: user.username,
      password: '',
      role: user.role,
      doctorId: user.doctorId || '',
      specialization: user.specialization || '',
      enabled: user.enabled
    });
    setEditOpen(true);
  };

  const onEditSave = async (e) => {
    e.preventDefault();
    if (!editUser) return;
    setEditing(true);
    try {
      await api.updateUser(editUser.id, {
        username: editUser.username,
        password: editUser.password || undefined,
        role: editUser.role,
        doctorId: editUser.doctorId === '' ? undefined : Number(editUser.doctorId),
        specialization: editUser.specialization || undefined,
        enabled: editUser.enabled
      });
      notify('success', 'User updated successfully.');
      setEditOpen(false);
      await loadUsers();
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    } finally {
      setEditing(false);
    }
  };

  const onToggleStatus = async (user) => {
    try {
      await api.setUserStatus(user.id, !user.enabled);
      notify('success', `User ${!user.enabled ? 'enabled' : 'disabled'} successfully.`);
      await loadUsers();
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    }
  };

  const onDeleteUser = async (user) => {
    if (!window.confirm(`Delete user ${user.username}?`)) return;
    try {
      await api.deleteUser(user.id);
      notify('success', 'User deleted successfully.');
      await Promise.all([loadUsers(), loadOverview()]);
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    }
  };

  return (
    <section className="space-y-4">
      <div className="panel p-5">
        <h2 className="panel-title">Admin Dashboard</h2>
        <p className="mt-1 text-sm text-slate-500">Create, update, enable/disable and delete staff users.</p>
        {overviewError ? <p className="mt-2 text-sm text-rose-600">{overviewError}</p> : null}
        {overview ? (
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm">Users: <span className="font-semibold">{overview.totalUsers}</span></div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm">Patients: <span className="font-semibold">{overview.totalPatients}</span></div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm">Appointments: <span className="font-semibold">{overview.totalAppointments}</span></div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm">Pending Rx: <span className="font-semibold">{overview.pendingPrescriptions}</span></div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm">Dispensed Rx: <span className="font-semibold">{overview.dispensedPrescriptions}</span></div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm">Low Stock: <span className="font-semibold">{overview.lowStockMedicines}</span></div>
          </div>
        ) : null}
      </div>

      <div className="grid gap-4 xl:grid-cols-5">
        <div className="panel p-5 xl:col-span-2">
          <h3 className="text-base font-semibold text-slate-900">Create New User</h3>
          <form className="mt-4 space-y-3" onSubmit={onCreateUser}>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Username</label>
              <input className="field" value={form.username} onChange={(e) => setForm((s) => ({ ...s, username: e.target.value }))} required />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Password</label>
              <input type="password" className="field" value={form.password} onChange={(e) => setForm((s) => ({ ...s, password: e.target.value }))} required />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Role</label>
              <select className="field" value={form.role} onChange={(e) => setForm((s) => ({ ...s, role: e.target.value }))}>
                {ROLE_OPTIONS.map((role) => <option key={role} value={role}>{role}</option>)}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Doctor ID (optional)</label>
              <input className="field" value={form.doctorId} onChange={(e) => setForm((s) => ({ ...s, doctorId: e.target.value }))} />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Specialization (optional)</label>
              <input className="field" value={form.specialization} onChange={(e) => setForm((s) => ({ ...s, specialization: e.target.value }))} />
            </div>
            <button className="btn-primary w-full" disabled={creating}>{creating ? 'Creating...' : 'Create User'}</button>
          </form>
        </div>

        <div className="panel p-5 xl:col-span-3">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h3 className="text-base font-semibold text-slate-900">Staff Members</h3>
            <div className="flex gap-2">
              <button className="btn-ghost" onClick={loadOverview}>Refresh Overview</button>
              <button className="btn-ghost" onClick={loadUsers}>Refresh Users</button>
            </div>
          </div>

          <AsyncState loading={loading} error={error} empty={!loading && !error && users.length === 0} emptyMessage="No staff members available." />

          {!loading && users.length > 0 ? (
            <>
              <div className="overflow-x-auto rounded-xl border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">ID</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Username</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Role</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Doctor ID</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Specialization</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Status</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {pagedUsers.map((user) => (
                      <tr key={user.id}>
                        <td className="px-4 py-3">{user.id}</td>
                        <td className="px-4 py-3 font-medium text-slate-900">{user.username}</td>
                        <td className="px-4 py-3">{normalizeRole(user.role)}</td>
                        <td className="px-4 py-3">{user.doctorId || '-'}</td>
                        <td className="px-4 py-3">{user.specialization || '-'}</td>
                        <td className="px-4 py-3">{user.enabled ? 'Enabled' : 'Disabled'}</td>
                        <td className="px-4 py-3">
                          <div className="flex gap-2">
                            <button className="btn-ghost" onClick={() => openEdit(user)}>Edit</button>
                            <button className="btn-ghost" onClick={() => onToggleStatus(user)}>{user.enabled ? 'Disable' : 'Enable'}</button>
                            <button className="btn-ghost" onClick={() => onDeleteUser(user)}>Delete</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={page} pageSize={PAGE_SIZE} totalItems={users.length} onPageChange={setPage} />
            </>
          ) : null}
        </div>
      </div>

      <Modal open={editOpen} title="Update User" onClose={() => setEditOpen(false)}>
        {editUser ? (
          <form className="space-y-3" onSubmit={onEditSave}>
            <input className="field" value={editUser.username} onChange={(e) => setEditUser((s) => ({ ...s, username: e.target.value }))} placeholder="Username" required />
            <input type="password" className="field" value={editUser.password} onChange={(e) => setEditUser((s) => ({ ...s, password: e.target.value }))} placeholder="New password (optional)" />
            <select className="field" value={editUser.role} onChange={(e) => setEditUser((s) => ({ ...s, role: e.target.value }))}>
              {ROLE_OPTIONS.map((role) => <option key={role} value={role}>{role}</option>)}
            </select>
            <input className="field" value={editUser.doctorId} onChange={(e) => setEditUser((s) => ({ ...s, doctorId: e.target.value }))} placeholder="Doctor ID (for doctor users)" />
            <input className="field" value={editUser.specialization} onChange={(e) => setEditUser((s) => ({ ...s, specialization: e.target.value }))} placeholder="Specialization" />
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={editUser.enabled} onChange={(e) => setEditUser((s) => ({ ...s, enabled: e.target.checked }))} />
              Enabled
            </label>
            <button className="btn-primary w-full" disabled={editing}>{editing ? 'Saving...' : 'Save Changes'}</button>
          </form>
        ) : null}
      </Modal>
    </section>
  );
}
