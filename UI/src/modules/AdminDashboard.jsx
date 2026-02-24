import { useEffect, useState } from 'react';
import AsyncState from '../components/AsyncState.jsx';
import Modal from '../components/Modal.jsx';
import Pagination from '../components/Pagination.jsx';
import { getApiErrorMessage } from '../api.js';

const ROLE_OPTIONS = ['ADMIN', 'DOCTOR', 'PHARMACIST', 'RECEPTIONIST'];
const SHIFT_OPTIONS = ['MORNING', 'EVENING', 'NIGHT'];
const PAGE_SIZE = 8;

function normalizeRole(roleValue) {
  if (!roleValue) return 'RECEPTIONIST';
  return String(roleValue).replace('ROLE_', '').toUpperCase();
}


function isDoctor(roleValue) {
  return normalizeRole(roleValue) === 'DOCTOR';
}

function isValidDoctorRefCode(value) {
  return /^MEDID-[0-9]{2}$/.test(String(value || '').trim().toUpperCase());
}

function normalizeDoctorRefInput(value) {
  const cleaned = String(value || '').toUpperCase().replace(/\s+/g, '');
  const digits = cleaned.replace(/^MEDID-?/, '').replace(/[^0-9]/g, '').slice(0, 2);
  return `MEDID-${digits}`;
}

function shiftLabel(shift) {
  if (shift === 'MORNING') return 'Morning (06:00 - 14:00)';
  if (shift === 'EVENING') return 'Evening (14:00 - 22:00)';
  if (shift === 'NIGHT') return 'Night (22:00 - 06:00)';
  return shift || '-';
}

function dayTypeLabel(weekdayShift, weekendShift) {
  if (weekdayShift) return 'WEEKDAY';
  if (weekendShift) return 'WEEKEND';
  return '-';
}

export default function AdminDashboard({ api, notify }) {
  const [users, setUsers] = useState([]);
  const [totalUsers, setTotalUsers] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [overview, setOverview] = useState(null);
  const [overviewError, setOverviewError] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState({
    username: '',
    password: '',
    role: 'DOCTOR',
    doctorRefCode: '',
    specialization: '',
    dayType: 'WEEKDAY',
    shift: ''
  });

  const [editOpen, setEditOpen] = useState(false);
  const [editUser, setEditUser] = useState(null);
  const [editing, setEditing] = useState(false);
  const [page, setPage] = useState(1);
  const [adminTab, setAdminTab] = useState('create');

  const loadUsers = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getStaffUsers();
      const list = Array.isArray(data) ? data : [];
      // Normalize backend variations to one frontend shape.
      const normalized = list.map((item) => ({
        id: item.id,
        username: item.username || item.userName || 'unknown',
        role: normalizeRole(item.role),
        enabled: item.enabled !== false,
        doctorId: item.doctorId ?? '',
        doctorRefCode: item.doctorRefCode ?? '',
        specialization: item.specialization ?? ''
        ,
        weekdayShift: item.weekdayShift ?? '',
        weekendShift: item.weekendShift ?? '',
        dayType: item.weekdayShift ? 'WEEKDAY' : (item.weekendShift ? 'WEEKEND' : ''),
        shift: item.weekdayShift || item.weekendShift || ''
      }));
      const start = (page - 1) * PAGE_SIZE;
      setUsers(normalized.slice(start, start + PAGE_SIZE));
      setTotalUsers(normalized.length);
      setTotalPages(Math.max(1, Math.ceil(normalized.length / PAGE_SIZE)));
    } catch (err) {
      setError(getApiErrorMessage(err));
      setUsers([]);
      setTotalUsers(0);
      setTotalPages(1);
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
    loadOverview();
  }, []);

  useEffect(() => {
    loadUsers();
  }, [page]);

  const canCreateUser = (() => {
    if (!form.username.trim() || !form.password.trim() || !form.role) return false;
    if (!isDoctor(form.role)) return true;
    return Boolean(
      form.specialization.trim()
      && isValidDoctorRefCode(form.doctorRefCode)
      && (form.dayType === 'WEEKDAY' || form.dayType === 'WEEKEND')
      && SHIFT_OPTIONS.includes(form.shift)
    );
  })();

  const onCreateUser = async (e) => {
    e.preventDefault();
    if (!canCreateUser) return;
    setCreating(true);
    try {
      await api.createUser({
        username: form.username.trim(),
        password: form.password,
        role: form.role,
        doctorRefCode: isDoctor(form.role) ? form.doctorRefCode.trim().toUpperCase() : undefined,
        specialization: isDoctor(form.role) ? form.specialization.trim() : undefined,
        weekdayShift: isDoctor(form.role) && form.dayType === 'WEEKDAY' ? form.shift : null,
        weekendShift: isDoctor(form.role) && form.dayType === 'WEEKEND' ? form.shift : null
      });
      setForm({
        username: '',
        password: '',
        role: 'DOCTOR',
        doctorRefCode: '',
        specialization: '',
        dayType: 'WEEKDAY',
        shift: ''
      });
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
      doctorRefCode: user.doctorRefCode || '',
      specialization: user.specialization || '',
      dayType: user.weekdayShift ? 'WEEKDAY' : 'WEEKEND',
      shift: user.weekdayShift || user.weekendShift || '',
      weekdayShift: user.weekdayShift || null,
      weekendShift: user.weekendShift || null,
      enabled: user.enabled
    });
    setEditOpen(true);
  };

  const canEditUser = (() => {
    if (!editUser) return false;
    if (!editUser.username.trim() || !editUser.role) return false;
    if (!isDoctor(editUser.role)) return true;
    return Boolean(
      editUser.specialization.trim()
      && isValidDoctorRefCode(editUser.doctorRefCode)
      && (editUser.dayType === 'WEEKDAY' || editUser.dayType === 'WEEKEND')
      && SHIFT_OPTIONS.includes(editUser.shift)
    );
  })();

  const onEditSave = async (e) => {
    e.preventDefault();
    if (!editUser || !canEditUser) return;
    setEditing(true);
    try {
      await api.updateUser(editUser.id, {
        username: editUser.username.trim(),
        password: editUser.password || undefined,
        role: editUser.role,
        doctorRefCode: isDoctor(editUser.role) ? editUser.doctorRefCode.trim().toUpperCase() : undefined,
        specialization: isDoctor(editUser.role) ? editUser.specialization.trim() : undefined,
        weekdayShift: isDoctor(editUser.role) && editUser.dayType === 'WEEKDAY' ? editUser.shift : null,
        weekendShift: isDoctor(editUser.role) && editUser.dayType === 'WEEKEND' ? editUser.shift : null,
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
    // Explicit confirmation prevents accidental destructive clicks.
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
        <div className="panel p-5 xl:col-span-5">
          <div className="mb-4 flex gap-2">
            <button className={adminTab === 'create' ? 'btn-primary' : 'btn-ghost'} onClick={() => setAdminTab('create')}>Create/Update</button>
            <button className={adminTab === 'staff' ? 'btn-primary' : 'btn-ghost'} onClick={() => setAdminTab('staff')}>Staff Members</button>
          </div>
        </div>
        {adminTab === 'create' ? (
        <div className="panel p-5 xl:col-span-5">
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
            {isDoctor(form.role) ? (
              <>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Doctor Ref ID (Required)</label>
                  <input
                    className="field"
                    value={form.doctorRefCode}
                    onChange={(e) => setForm((s) => ({ ...s, doctorRefCode: normalizeDoctorRefInput(e.target.value) }))}
                    placeholder="MEDID-XX"
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Specialization (Required)</label>
                  <input className="field" value={form.specialization} onChange={(e) => setForm((s) => ({ ...s, specialization: e.target.value }))} required />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Day Type (Required)</label>
                  <select className="field" value={form.dayType} onChange={(e) => setForm((s) => ({ ...s, dayType: e.target.value }))} required>
                    <option value="WEEKDAY">WEEKDAY</option>
                    <option value="WEEKEND">WEEKEND</option>
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">Shift (Required)</label>
                  <select className="field" value={form.shift} onChange={(e) => setForm((s) => ({ ...s, shift: e.target.value }))} required>
                    <option value="">Select shift</option>
                    {SHIFT_OPTIONS.map((shift) => <option key={shift} value={shift}>{shiftLabel(shift)}</option>)}
                  </select>
                </div>
              </>
            ) : null}
            <button className="btn-primary w-full" disabled={creating || !canCreateUser}>{creating ? 'Creating...' : 'Create User'}</button>
          </form>
        </div>
        ) : null}

        {adminTab === 'staff' ? (
        <div className="panel p-5 xl:col-span-5">
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
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Doctor Ref ID</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Specialization</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Day Type</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Shift</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Status</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {users.map((user) => (
                      <tr key={user.id}>
                        <td className="px-4 py-3">{user.id}</td>
                        <td className="px-4 py-3 font-medium text-slate-900">{user.username}</td>
                        <td className="px-4 py-3">{normalizeRole(user.role)}</td>
                        <td className="px-4 py-3">{user.doctorRefCode || '-'}</td>
                        <td className="px-4 py-3">{user.specialization || '-'}</td>
                        <td className="px-4 py-3">{dayTypeLabel(user.weekdayShift, user.weekendShift)}</td>
                        <td className="px-4 py-3">{shiftLabel(user.weekdayShift || user.weekendShift)}</td>
                        <td className="px-4 py-3">
                          <span className={`pill ${user.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>
                            {user.enabled ? 'Enabled' : 'Disabled'}
                          </span>
                        </td>
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
              <Pagination page={page} pageSize={PAGE_SIZE} totalItems={totalUsers} totalPages={totalPages} onPageChange={setPage} />
            </>
          ) : null}
        </div>
        ) : null}
      </div>

      <Modal open={editOpen} title="Update User" onClose={() => setEditOpen(false)}>
        {editUser ? (
          <form className="space-y-3" onSubmit={onEditSave}>
            <input className="field" value={editUser.username} onChange={(e) => setEditUser((s) => ({ ...s, username: e.target.value }))} placeholder="Username" required />
            <input type="password" className="field" value={editUser.password} onChange={(e) => setEditUser((s) => ({ ...s, password: e.target.value }))} placeholder="New password (optional)" />
            <select className="field" value={editUser.role} onChange={(e) => setEditUser((s) => ({ ...s, role: e.target.value }))}>
              {ROLE_OPTIONS.map((role) => <option key={role} value={role}>{role}</option>)}
            </select>
            {isDoctor(editUser.role) ? (
              <>
                <input
                  className="field"
                  value={editUser.doctorRefCode}
                  onChange={(e) => setEditUser((s) => ({ ...s, doctorRefCode: normalizeDoctorRefInput(e.target.value) }))}
                  placeholder="Doctor Ref ID (MEDID-XX)"
                  required
                />
                <input className="field" value={editUser.specialization} onChange={(e) => setEditUser((s) => ({ ...s, specialization: e.target.value }))} placeholder="Specialization" required />
                <select className="field" value={editUser.dayType} onChange={(e) => setEditUser((s) => ({ ...s, dayType: e.target.value }))} required>
                  <option value="WEEKDAY">WEEKDAY</option>
                  <option value="WEEKEND">WEEKEND</option>
                </select>
                <select className="field" value={editUser.shift} onChange={(e) => setEditUser((s) => ({ ...s, shift: e.target.value }))} required>
                  <option value="">Shift</option>
                  {SHIFT_OPTIONS.map((shift) => <option key={shift} value={shift}>{shiftLabel(shift)}</option>)}
                </select>
              </>
            ) : null}
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={editUser.enabled} onChange={(e) => setEditUser((s) => ({ ...s, enabled: e.target.checked }))} />
              Enabled
            </label>
            <button className="btn-primary w-full" disabled={editing || !canEditUser}>{editing ? 'Saving...' : 'Save Changes'}</button>
          </form>
        ) : null}
      </Modal>
    </section>
  );
}
