import { useEffect, useMemo, useState } from 'react';
import AsyncState from '../components/AsyncState.jsx';
import Modal from '../components/Modal.jsx';
import Pagination from '../components/Pagination.jsx';
import { getApiErrorMessage } from '../api.js';

function toDateInput(date = new Date()) {
  return date.toISOString().slice(0, 10);
}

function timeFromIso(value) {
  if (!value) return '-';
  return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function slotKeyFromIso(value) {
  if (!value) return '';
  const date = new Date(value);
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
}

function buildSlots() {
  const slots = [];
  for (let hour = 9; hour < 18; hour += 1) {
    slots.push(`${String(hour).padStart(2, '0')}:00`);
    slots.push(`${String(hour).padStart(2, '0')}:30`);
  }
  return slots;
}

const STATUS_OPTIONS = ['SCHEDULED', 'COMPLETED', 'CANCELLED'];
const PAGE_SIZE = 8;

export default function ReceptionistDashboard({ api, notify, view = 'booking', role = 'RECEPTIONIST' }) {
  const [date, setDate] = useState(toDateInput());
  const [doctorRef, setDoctorRef] = useState('1');
  const [appointments, setAppointments] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [openModal, setOpenModal] = useState(false);
  const [creating, setCreating] = useState(false);
  const [checkingPatient, setCheckingPatient] = useState(false);
  const [existingPatient, setExistingPatient] = useState(null);
  const [patientQuery, setPatientQuery] = useState('');
  const [patientSuggestions, setPatientSuggestions] = useState([]);
  const [searchingPatients, setSearchingPatients] = useState(false);
  const [dailyPage, setDailyPage] = useState(1);
  const [allPage, setAllPage] = useState(1);
  const [allFilters, setAllFilters] = useState({
    patientName: '',
    appointmentId: '',
    doctorName: '',
    doctorId: '',
    date: ''
  });
  const [newAppointment, setNewAppointment] = useState({
    patientId: '',
    fullName: '',
    phoneNumber: '',
    slot: '',
    doctorRef: '1',
    gender: 'NA',
    medicalHistoryBlob: ''
  });

  const slots = useMemo(buildSlots, []);

  const loadDoctors = async () => {
    try {
      const data = await api.getDoctors();
      const list = Array.isArray(data) ? data : [];
      setDoctors(list);
      if (list.length > 0 && !doctorRef) {
        setDoctorRef(String(list[0].appointmentDoctorId));
      }
    } catch {
      setDoctors([]);
    }
  };

  const loadAppointments = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getAllAppointments();
      setAppointments(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(getApiErrorMessage(err));
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDoctors();
    loadAppointments();
  }, []);

  useEffect(() => {
    setDailyPage(1);
  }, [date, doctorRef, appointments.length]);

  useEffect(() => {
    setAllPage(1);
  }, [appointments.length, allFilters]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!patientQuery || patientQuery.trim().length < 2) {
        setPatientSuggestions([]);
        return;
      }
      setSearchingPatients(true);
      try {
        const result = await api.searchPatients(patientQuery.trim());
        if (!cancelled) {
          setPatientSuggestions(Array.isArray(result) ? result.slice(0, 8) : []);
        }
      } catch {
        if (!cancelled) setPatientSuggestions([]);
      } finally {
        if (!cancelled) setSearchingPatients(false);
      }
    };

    const handle = setTimeout(run, 250);
    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [api, patientQuery]);

  const selectedDoctorAppointments = useMemo(() => {
    const numericDoctor = Number(doctorRef);
    if (!Number.isNaN(numericDoctor)) {
      return appointments.filter((item) => Number(item?.doctorId) === numericDoctor);
    }
    const doctorEntry = doctors.find((d) => d.username === doctorRef);
    const resolvedId = doctorEntry?.appointmentDoctorId;
    if (!resolvedId) return appointments;
    return appointments.filter((item) => Number(item?.doctorId) === Number(resolvedId));
  }, [appointments, doctorRef, doctors]);

  const doctorMeta = useMemo(() => {
    const map = new Map();
    doctors.forEach((d) => {
      const id = String(d.appointmentDoctorId);
      map.set(id, d);
    });
    return map;
  }, [doctors]);

  const dailyAppointments = useMemo(() => {
    return selectedDoctorAppointments.filter((item) => item?.appointmentTime?.slice(0, 10) === date);
  }, [selectedDoctorAppointments, date]);

  const occupied = useMemo(() => {
    const map = new Map();
    dailyAppointments.forEach((appt) => {
      const time = slotKeyFromIso(appt.appointmentTime);
      const status = (appt.status || '').toUpperCase();
      if (status === 'SCHEDULED' || status === 'BOOKED' || status === 'UNAVAILABLE') {
        map.set(time, appt);
      }
    });
    return map;
  }, [dailyAppointments]);

  const pagedDailyAppointments = useMemo(() => {
    const start = (dailyPage - 1) * PAGE_SIZE;
    return dailyAppointments.slice(start, start + PAGE_SIZE);
  }, [dailyAppointments, dailyPage]);

  const filteredAllAppointments = useMemo(() => {
    return appointments.filter((appt) => {
      const patientName = (appt?.patient?.fullName || '').toLowerCase();
      const appointmentId = String(appt?.appointmentId || '');
      const doctorId = String(appt?.doctorId || '');
      const date = appt?.appointmentTime?.slice(0, 10) || '';
      const doctor = doctorMeta.get(doctorId);
      const doctorName = (doctor?.username || '').toLowerCase();

      if (allFilters.patientName && !patientName.includes(allFilters.patientName.toLowerCase())) return false;
      if (allFilters.appointmentId && !appointmentId.includes(allFilters.appointmentId.trim())) return false;
      if (allFilters.doctorId && !doctorId.includes(allFilters.doctorId.trim())) return false;
      if (allFilters.doctorName && !doctorName.includes(allFilters.doctorName.toLowerCase())) return false;
      if (allFilters.date && date !== allFilters.date) return false;
      return true;
    });
  }, [appointments, allFilters, doctorMeta]);

  const pagedAllAppointments = useMemo(() => {
    const start = (allPage - 1) * PAGE_SIZE;
    return filteredAllAppointments.slice(start, start + PAGE_SIZE);
  }, [allPage, filteredAllAppointments]);

  const onCheckExistingPatient = async (phone) => {
    if (!phone || phone.length !== 10) {
      setExistingPatient(null);
      return;
    }

    setCheckingPatient(true);
    try {
      const patient = await api.findPatientByPhone(phone);
      setExistingPatient(patient);
      setNewAppointment((s) => ({
        ...s,
        patientId: patient?.patientId || s.patientId,
        fullName: patient?.fullName || s.fullName,
        gender: patient?.gender || s.gender,
        medicalHistoryBlob: ''
      }));
    } catch {
      setExistingPatient(null);
    } finally {
      setCheckingPatient(false);
    }
  };

  const onCreateAppointment = async (e) => {
    e.preventDefault();
    setCreating(true);
    try {
      await api.createAppointment({ ...newAppointment, doctorRef: newAppointment.doctorRef || doctorRef });
      notify('success', 'Appointment created successfully.');
      setOpenModal(false);
      setExistingPatient(null);
      setPatientQuery('');
      setPatientSuggestions([]);
      setNewAppointment((s) => ({
        ...s,
        patientId: '',
        fullName: '',
        phoneNumber: '',
        medicalHistoryBlob: ''
      }));
      await loadAppointments();
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    } finally {
      setCreating(false);
    }
  };

  const updateStatus = async (id, status) => {
    try {
      await api.updateAppointmentStatus(id, status);
      notify('success', 'Appointment status updated.');
      await loadAppointments();
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    }
  };

  const pickSlot = (time) => {
    const slot = `${date}T${time}`;
    setNewAppointment((s) => ({ ...s, slot, doctorRef }));
    setOpenModal(true);
  };

  const onSelectPatient = (patient) => {
    setExistingPatient(patient);
    setPatientQuery(`${patient.patientId} - ${patient.fullName}`);
    setPatientSuggestions([]);
    setNewAppointment((s) => ({
      ...s,
      patientId: patient.patientId,
      fullName: patient.fullName || s.fullName,
      phoneNumber: patient.phoneNumber || s.phoneNumber,
      gender: patient.gender || s.gender,
      medicalHistoryBlob: ''
    }));
  };

  return (
    <section className="space-y-4">
      <div className="panel p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="panel-title">Receptionist Dashboard</h2>
            <p className="mt-1 text-sm text-slate-500">
              {view === 'all' ? 'All appointments made in the system.' : 'Book quickly with 30-minute slots and live doctor mapping.'}
            </p>
          </div>
          <button className="btn-primary" onClick={() => setOpenModal(true)}>New Appointment</button>
        </div>
      </div>

      <div className="panel p-5">
        <div className="grid gap-3 md:grid-cols-3">
          <div>
            <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Doctor (ID or Name)</label>
            <input className="field" value={doctorRef} onChange={(e) => setDoctorRef(e.target.value)} placeholder="e.g. 1 or doc_alice" />
            {doctors.length > 0 ? (
              <select className="field mt-2" value={doctorRef} onChange={(e) => setDoctorRef(e.target.value)}>
                {doctors.map((doctor) => (
                  <option key={doctor.userId} value={doctor.username}>
                    {doctor.username} (ID: {doctor.appointmentDoctorId}){doctor.specialization ? ` - ${doctor.specialization}` : ''}
                  </option>
                ))}
              </select>
            ) : null}
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Date</label>
            <input type="date" className="field" value={date} onChange={(e) => setDate(e.target.value)} />
          </div>
          <div className="flex items-end">
            <button className="btn-ghost w-full" onClick={loadAppointments}>Refresh Appointments</button>
          </div>
        </div>

        {view === 'booking' ? (
          <div className="mt-4 flex gap-4 text-xs">
            <div className="inline-flex items-center gap-2"><span className="h-3 w-3 rounded bg-emerald-500" />Available</div>
            <div className="inline-flex items-center gap-2"><span className="h-3 w-3 rounded bg-red-500" />Booked</div>
          </div>
        ) : null}

        <div className="mt-4">
          <AsyncState
            loading={loading}
            error={error}
            empty={!loading && !error && appointments.length === 0}
            emptyMessage="No appointments available."
          />

          {view === 'booking' ? (
            <>
              <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {slots.map((slot) => {
                  const appt = occupied.get(slot);
                  return (
                    <button
                      key={slot}
                      type="button"
                      className={`rounded-lg border p-3 text-left text-sm ${appt ? 'border-red-300 bg-red-100 text-red-800' : 'border-emerald-300 bg-emerald-100 text-emerald-800 hover:border-emerald-400'}`}
                      onClick={() => (!appt ? pickSlot(slot) : undefined)}
                    >
                      <p className="font-semibold">{slot}</p>
                      {appt ? (
                        <p className="mt-1 text-xs">Booked: {appt?.patient?.fullName || 'Unknown'}</p>
                      ) : (
                        <p className="mt-1 text-xs">Available</p>
                      )}
                    </button>
                  );
                })}
              </div>

              {!loading && !error && dailyAppointments.length > 0 ? (
                <div className="mt-6 overflow-x-auto rounded-xl border border-slate-200">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-4 py-3 text-left font-semibold text-slate-600">Time</th>
                        <th className="px-4 py-3 text-left font-semibold text-slate-600">Patient</th>
                        <th className="px-4 py-3 text-left font-semibold text-slate-600">Phone</th>
                        <th className="px-4 py-3 text-left font-semibold text-slate-600">Status</th>
                        <th className="px-4 py-3 text-left font-semibold text-slate-600">Update</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {pagedDailyAppointments.map((appt) => (
                        <tr key={appt.appointmentId}>
                          <td className="px-4 py-3">{timeFromIso(appt.appointmentTime)}</td>
                          <td className="px-4 py-3">{appt?.patient?.fullName || 'Unknown'}</td>
                          <td className="px-4 py-3">{appt?.patient?.phoneNumber || '-'}</td>
                          <td className="px-4 py-3"><span className="pill bg-med-100 text-med-700">{appt.status || 'SCHEDULED'}</span></td>
                          <td className="px-4 py-3">
                            <select className="field" defaultValue={appt.status || 'SCHEDULED'} onChange={(e) => updateStatus(appt.appointmentId, e.target.value)}>
                              {STATUS_OPTIONS.map((status) => <option key={status} value={status}>{status}</option>)}
                            </select>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <Pagination page={dailyPage} pageSize={PAGE_SIZE} totalItems={dailyAppointments.length} onPageChange={setDailyPage} />
            </>
          ) : null}

          {view === 'all' && !loading && !error ? (
            <>
              <div className="mb-3 grid gap-3 md:grid-cols-3">
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Patient Name</label>
                  <input
                    className="field"
                    value={allFilters.patientName}
                    onChange={(e) => setAllFilters((s) => ({ ...s, patientName: e.target.value }))}
                    placeholder="Patient name"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Appointment ID</label>
                  <input
                    className="field"
                    value={allFilters.appointmentId}
                    onChange={(e) => setAllFilters((s) => ({ ...s, appointmentId: e.target.value }))}
                    placeholder="Appointment ID"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Doctor Name</label>
                  <input
                    className="field"
                    value={allFilters.doctorName}
                    onChange={(e) => setAllFilters((s) => ({ ...s, doctorName: e.target.value }))}
                    placeholder="Doctor username"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Doctor ID</label>
                  <input
                    className="field"
                    value={allFilters.doctorId}
                    onChange={(e) => setAllFilters((s) => ({ ...s, doctorId: e.target.value }))}
                    placeholder="Doctor ID"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Date</label>
                  <input
                    type="date"
                    className="field"
                    value={allFilters.date}
                    onChange={(e) => setAllFilters((s) => ({ ...s, date: e.target.value }))}
                  />
                </div>
                <div className="flex items-end">
                  <button
                    className="btn-ghost w-full"
                    onClick={() => setAllFilters({ patientName: '', appointmentId: '', doctorName: '', doctorId: '', date: '' })}
                  >
                    Clear Filters
                  </button>
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Doctor (ID or Name)</label>
                  <input
                    className="field"
                    value={doctorRef}
                    onChange={(e) => setDoctorRef(e.target.value)}
                    placeholder="Doctor focus for booking tab"
                  />
                </div>
              </div>
              <div className="mt-6 overflow-x-auto rounded-xl border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Appointment ID</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Doctor ID</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Doctor Name</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Specialization</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Patient</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Time</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Status</th>
                      {role === 'ADMIN' ? <th className="px-4 py-3 text-left font-semibold text-slate-600">Medical Blob</th> : null}
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Update</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {pagedAllAppointments.map((appt) => {
                      const doctor = doctorMeta.get(String(appt.doctorId));
                      return (
                        <tr key={appt.appointmentId}>
                          <td className="px-4 py-3">{appt.appointmentId}</td>
                          <td className="px-4 py-3">{appt.doctorId}</td>
                          <td className="px-4 py-3">{doctor?.username || '-'}</td>
                          <td className="px-4 py-3">{doctor?.specialization || '-'}</td>
                          <td className="px-4 py-3">{appt?.patient?.fullName || 'Unknown'}</td>
                          <td className="px-4 py-3">{appt?.appointmentTime || '-'}</td>
                          <td className="px-4 py-3">{appt?.status || '-'}</td>
                          {role === 'ADMIN' ? <td className="px-4 py-3">{appt?.patient?.medicalHistoryBlob || '-'}</td> : null}
                          <td className="px-4 py-3">
                            <select className="field" defaultValue={appt.status || 'SCHEDULED'} onChange={(e) => updateStatus(appt.appointmentId, e.target.value)}>
                              {STATUS_OPTIONS.map((status) => <option key={status} value={status}>{status}</option>)}
                            </select>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <Pagination page={allPage} pageSize={PAGE_SIZE} totalItems={filteredAllAppointments.length} onPageChange={setAllPage} />
            </>
          ) : null}
        </div>
      </div>

      <Modal open={openModal} title="Create New Appointment" onClose={() => setOpenModal(false)}>
        <form className="space-y-4" onSubmit={onCreateAppointment}>
          <div className="relative">
            <label className="mb-1 block text-sm font-medium text-slate-700">Existing Patient (ID or Name)</label>
            <input
              className="field"
              value={patientQuery}
              onChange={(e) => {
                const value = e.target.value;
                setPatientQuery(value);
                if (!value) {
                  setExistingPatient(null);
                  setNewAppointment((s) => ({ ...s, patientId: '' }));
                }
              }}
              placeholder="Type patient ID or name"
            />
            {searchingPatients ? <p className="mt-1 text-xs text-slate-500">Searching patients...</p> : null}
            {patientSuggestions.length > 0 ? (
              <div className="absolute z-20 mt-1 max-h-56 w-full overflow-auto rounded-lg border border-slate-200 bg-white shadow-lg">
                {patientSuggestions.map((patient) => (
                  <button
                    key={patient.patientId}
                    type="button"
                    className="block w-full border-b border-slate-100 px-3 py-2 text-left text-sm hover:bg-slate-50"
                    onClick={() => onSelectPatient(patient)}
                  >
                    <p className="font-medium text-slate-900">#{patient.patientId} - {patient.fullName}</p>
                    <p className="text-xs text-slate-500">{patient.phoneNumber || '-'} | {patient.gender || '-'}</p>
                  </button>
                ))}
              </div>
            ) : null}
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Phone</label>
              <input
                className="field"
                value={newAppointment.phoneNumber}
                onChange={(e) => {
                  const phone = e.target.value;
                  setNewAppointment((s) => ({ ...s, phoneNumber: phone, patientId: '' }));
                  setExistingPatient(null);
                }}
                onBlur={(e) => onCheckExistingPatient(e.target.value)}
                required
              />
              {checkingPatient ? <p className="mt-1 text-xs text-slate-500">Checking existing patient...</p> : null}
              {existingPatient ? <p className="mt-1 text-xs text-emerald-700">Existing patient found. Medical blob not required.</p> : null}
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Patient Full Name</label>
              <input
                className="field"
                value={newAppointment.fullName}
                onChange={(e) => {
                  setExistingPatient(null);
                  setNewAppointment((s) => ({ ...s, fullName: e.target.value, patientId: '' }));
                }}
                required
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Doctor ID or Name</label>
              <input className="field" value={newAppointment.doctorRef} onChange={(e) => setNewAppointment((s) => ({ ...s, doctorRef: e.target.value }))} required />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Slot Selection</label>
              <input type="datetime-local" className="field" value={newAppointment.slot} onChange={(e) => setNewAppointment((s) => ({ ...s, slot: e.target.value }))} required />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Gender</label>
              <input className="field" value={newAppointment.gender} onChange={(e) => setNewAppointment((s) => ({ ...s, gender: e.target.value }))} />
            </div>
            {!existingPatient ? (
              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-slate-700">Medical Blob</label>
                <textarea className="field min-h-24" value={newAppointment.medicalHistoryBlob} onChange={(e) => setNewAppointment((s) => ({ ...s, medicalHistoryBlob: e.target.value }))} placeholder="Add medical notes for new patient (optional)." />
              </div>
            ) : null}
          </div>
          <button className="btn-primary w-full" disabled={creating}>{creating ? 'Creating...' : 'Create Appointment'}</button>
        </form>
      </Modal>
    </section>
  );
}
