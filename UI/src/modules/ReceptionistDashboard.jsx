import { useEffect, useMemo, useState } from 'react';
import AsyncState from '../components/AsyncState.jsx';
import Modal from '../components/Modal.jsx';
import Pagination from '../components/Pagination.jsx';
import { getApiErrorMessage } from '../api.js';

function toDateInput(date = new Date()) {
  return date.toISOString().slice(0, 10);
}

function extractDateKey(value) {
  if (!value) return '';
  const text = String(value);
  const match = text.match(/^(\d{4}-\d{2}-\d{2})/);
  if (match) return match[1];
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return '';
  const y = parsed.getFullYear();
  const m = String(parsed.getMonth() + 1).padStart(2, '0');
  const d = String(parsed.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
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

function buildSlotsInRange(startHour, endHour) {
  const slots = [];
  for (let hour = startHour; hour < endHour; hour += 1) {
    slots.push(`${String(hour).padStart(2, '0')}:00`);
    slots.push(`${String(hour).padStart(2, '0')}:30`);
  }
  return slots;
}

function buildSlotsForShift(shift) {
  if (shift === 'MORNING') return buildSlotsInRange(6, 14);
  if (shift === 'EVENING') return buildSlotsInRange(14, 22);
  if (shift === 'NIGHT') return [...buildSlotsInRange(22, 24), ...buildSlotsInRange(0, 6)];
  return [];
}

function isWeekendDate(dateStr) {
  if (!dateStr) return false;
  const day = new Date(`${dateStr}T00:00:00`).getDay();
  return day === 0 || day === 6;
}

function getDoctorShiftForDate(doctor, dateStr) {
  return isWeekendDate(dateStr)
    ? String(doctor?.weekendShift || '').toUpperCase()
    : String(doctor?.weekdayShift || '').toUpperCase();
}

function statusPillClass(status) {
  const s = String(status || '').toUpperCase();
  if (s === 'COMPLETED') return 'bg-emerald-100 text-emerald-700';
  if (s === 'CANCELLED') return 'bg-rose-100 text-rose-700';
  if (s === 'UNAVAILABLE') return 'bg-slate-200 text-slate-700';
  return 'bg-amber-100 text-amber-700';
}

function shiftLabel(shift) {
  if (shift === 'MORNING') return 'Morning (06:00 - 14:00)';
  if (shift === 'EVENING') return 'Evening (14:00 - 22:00)';
  if (shift === 'NIGHT') return 'Night (22:00 - 06:00)';
  return shift;
}

const PAGE_SIZE = 8;
const SHIFT_OPTIONS = ['MORNING', 'EVENING', 'NIGHT'];

export default function ReceptionistDashboard({ api, notify, view = 'booking', role = 'RECEPTIONIST' }) {
  const today = toDateInput();
  const now = new Date();

  const [date, setDate] = useState(today);
  const [selectedSpecialization, setSelectedSpecialization] = useState('');
  const [selectedShift, setSelectedShift] = useState('');
  const [doctorRef, setDoctorRef] = useState('');

  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [allSourceAppointments, setAllSourceAppointments] = useState([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [dailyPage, setDailyPage] = useState(1);
  const [allPage, setAllPage] = useState(1);

  const [allFilters, setAllFilters] = useState({
    patientName: '',
    appointmentId: '',
    doctorName: '',
    doctorRefCode: '',
    status: '',
    date: ''
  });

  const [patientModalOpen, setPatientModalOpen] = useState(false);
  const [creatingPatient, setCreatingPatient] = useState(false);
  const [newPatient, setNewPatient] = useState({
    fullName: '',
    phoneNumber: '',
    gender: 'NA',
    medicalHistoryBlob: ''
  });

  const [bookingModalOpen, setBookingModalOpen] = useState(false);
  const [bookingSlot, setBookingSlot] = useState('');
  const [bookingPatientQuery, setBookingPatientQuery] = useState('');
  const [bookingPatientSuggestions, setBookingPatientSuggestions] = useState([]);
  const [searchingPatients, setSearchingPatients] = useState(false);
  const [selectedBookingPatient, setSelectedBookingPatient] = useState(null);
  const [bookingAppointment, setBookingAppointment] = useState(false);
  const [slotStatusModalOpen, setSlotStatusModalOpen] = useState(false);
  const [slotStatusAppointment, setSlotStatusAppointment] = useState(null);
  const [slotStatusValue, setSlotStatusValue] = useState('');
  const [updatingSlotStatus, setUpdatingSlotStatus] = useState(false);

  const slots = useMemo(() => buildSlotsForShift(selectedShift), [selectedShift]);

  const doctorMeta = useMemo(() => {
    const map = new Map();
    doctors.forEach((d) => map.set(String(d.appointmentDoctorId), d));
    return map;
  }, [doctors]);

  const specializationOptions = useMemo(
    () => Array.from(new Set(doctors.map((d) => d.specialization).filter(Boolean))).sort(),
    [doctors]
  );

  const doctorsBySpecialization = useMemo(
    () => (selectedSpecialization ? doctors.filter((d) => String(d.specialization || '') === selectedSpecialization) : []),
    [doctors, selectedSpecialization]
  );

  const doctorsByShift = useMemo(
    () => (selectedShift ? doctorsBySpecialization.filter((d) => getDoctorShiftForDate(d, date) === selectedShift) : []),
    [doctorsBySpecialization, selectedShift, date]
  );

  const selectedDoctor = useMemo(
    () => doctors.find((d) => String(d.username) === String(doctorRef)),
    [doctors, doctorRef]
  );

  const selectedDoctorId = selectedDoctor?.appointmentDoctorId;

  const filteredAllAppointments = useMemo(() => {
    const filtered = allSourceAppointments.filter((appt) => {
      const patientName = (appt?.patient?.fullName || '').toLowerCase();
      const appointmentId = String(appt?.appointmentId || '');
      const doctorId = String(appt?.doctorId || '');
      const apptDate = extractDateKey(appt?.appointmentTime);
      const doctor = doctorMeta.get(doctorId);
      const doctorName = (doctor?.username || '').toLowerCase();
      const doctorRefCode = String(doctor?.doctorRefCode || '');
      const status = String(appt?.status || '').toUpperCase();

      if (allFilters.patientName && !patientName.includes(allFilters.patientName.toLowerCase())) return false;
      if (allFilters.appointmentId && !appointmentId.includes(allFilters.appointmentId.trim())) return false;
      if (allFilters.doctorRefCode && !doctorRefCode.toLowerCase().includes(allFilters.doctorRefCode.toLowerCase().trim())) return false;
      if (allFilters.doctorName && !doctorName.includes(allFilters.doctorName.toLowerCase())) return false;
      if (allFilters.status && status !== allFilters.status) return false;
      if (allFilters.date && apptDate !== allFilters.date) return false;
      return true;
    });
    return filtered.sort((a, b) => String(b?.appointmentTime || '').localeCompare(String(a?.appointmentTime || '')));
  }, [allSourceAppointments, allFilters, doctorMeta]);

  const totalAllAppointments = filteredAllAppointments.length;
  const totalAllPages = Math.max(1, Math.ceil(totalAllAppointments / PAGE_SIZE));

  const allAppointments = useMemo(() => {
    const start = (allPage - 1) * PAGE_SIZE;
    return filteredAllAppointments.slice(start, start + PAGE_SIZE);
  }, [filteredAllAppointments, allPage]);

  const dailyAppointments = useMemo(() => appointments, [appointments]);

  const occupied = useMemo(() => {
    const map = new Map();
    dailyAppointments.forEach((appt) => {
      const time = slotKeyFromIso(appt.appointmentTime);
      const status = (appt.status || '').toUpperCase();
      if (status === 'SCHEDULED' || status === 'BOOKED' || status === 'UNAVAILABLE') map.set(time, appt);
    });
    return map;
  }, [dailyAppointments]);

  const loadDoctors = async () => {
    try {
      const data = await api.getDoctors();
      setDoctors(Array.isArray(data) ? data : []);
    } catch {
      setDoctors([]);
    }
  };

  const loadDayAppointments = async () => {
    if (!selectedDoctorId) {
      setAppointments([]);
      return;
    }

    setLoading(true);
    setError('');
    try {
      const all = await api.getAllAppointments();
      const list = Array.isArray(all) ? all : [];
      const filtered = list.filter((item) => extractDateKey(item?.appointmentTime) === date && Number(item?.doctorId) === Number(selectedDoctorId));
      setAppointments(filtered);
    } catch (err) {
      setError(getApiErrorMessage(err));
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  };

  const loadAllAppointments = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getAllAppointments();
      setAllSourceAppointments(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(getApiErrorMessage(err));
      setAllSourceAppointments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDoctors();
  }, []);

  useEffect(() => {
    if (date < today) setDate(today);
  }, [date, today]);

  useEffect(() => {
    setSelectedShift('');
    setDoctorRef('');
  }, [selectedSpecialization]);

  useEffect(() => {
    setDoctorRef('');
  }, [selectedShift, date]);

  useEffect(() => {
    if (view === 'booking') loadDayAppointments();
  }, [view, date, selectedDoctorId]);

  useEffect(() => {
    if (view === 'all') loadAllAppointments();
  }, [view]);

  useEffect(() => {
    setAllPage(1);
  }, [allFilters]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!bookingModalOpen || bookingPatientQuery.trim().length < 2) {
        setBookingPatientSuggestions([]);
        return;
      }
      setSearchingPatients(true);
      try {
        const data = await api.searchPatients(bookingPatientQuery.trim());
        if (!cancelled) setBookingPatientSuggestions(Array.isArray(data) ? data.slice(0, 8) : []);
      } catch {
        if (!cancelled) setBookingPatientSuggestions([]);
      } finally {
        if (!cancelled) setSearchingPatients(false);
      }
    };

    const t = setTimeout(run, 250);
    return () => {
      cancelled = true;
      clearTimeout(t);
    };
  }, [api, bookingPatientQuery, bookingModalOpen]);

  const isPastSlot = (slot) => new Date(`${date}T${slot}:00`) < now;

  const statusOptionsFor = (currentStatus) => {
    const current = String(currentStatus || '').toUpperCase();
    if (current === 'COMPLETED') return ['COMPLETED'];
    if (current === 'CANCELLED') return ['CANCELLED'];
    if (current === 'SCHEDULED' || current === 'BOOKED') return ['SCHEDULED', 'CANCELLED', 'COMPLETED'];
    if (current === 'UNAVAILABLE') return ['UNAVAILABLE', 'CANCELLED'];
    return ['SCHEDULED', 'COMPLETED', 'CANCELLED'];
  };

  const openSlotBooking = (slot) => {
    setBookingSlot(`${date}T${slot}`);
    setSelectedBookingPatient(null);
    setBookingPatientQuery('');
    setBookingPatientSuggestions([]);
    setBookingModalOpen(true);
  };

  const openSlotStatus = (appointment) => {
    if (!appointment) return;
    setSlotStatusAppointment(appointment);
    setSlotStatusValue(String(appointment.status || 'SCHEDULED').toUpperCase());
    setSlotStatusModalOpen(true);
  };

  const createPatient = async (e) => {
    e.preventDefault();
    if (!newPatient.fullName.trim() || !newPatient.phoneNumber.trim()) return;
    setCreatingPatient(true);
    try {
      await api.registerPatient({
        fullName: newPatient.fullName.trim(),
        phoneNumber: newPatient.phoneNumber.trim(),
        gender: newPatient.gender || 'NA',
        medicalHistoryBlob: newPatient.medicalHistoryBlob || ''
      });
      notify('success', 'Patient created successfully. Now select a slot to book.');
      setPatientModalOpen(false);
      setNewPatient({ fullName: '', phoneNumber: '', gender: 'NA', medicalHistoryBlob: '' });
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    } finally {
      setCreatingPatient(false);
    }
  };

  const bookSelectedPatient = async (e) => {
    e.preventDefault();
    if (!selectedBookingPatient || !bookingSlot || !doctorRef) return;
    setBookingAppointment(true);
    try {
      await api.bookAppointmentByDoctorRef({
        patientId: selectedBookingPatient.patientId,
        doctor: doctorRef,
        slot: bookingSlot
      });
      notify('success', 'Appointment booked successfully.');
      setBookingModalOpen(false);
      await loadDayAppointments();
      await loadAllAppointments();
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    } finally {
      setBookingAppointment(false);
    }
  };

  const updateStatus = async (id, status) => {
    try {
      await api.updateAppointmentStatus(id, status);
      notify('success', 'Appointment status updated.');
      if (view !== 'all') await loadDayAppointments();
      await loadAllAppointments();
    } catch (err) {
      notify('error', getApiErrorMessage(err));
    }
  };

  const submitSlotStatus = async (e) => {
    e.preventDefault();
    if (!slotStatusAppointment) return;
    setUpdatingSlotStatus(true);
    try {
      await updateStatus(slotStatusAppointment.appointmentId, slotStatusValue);
      setSlotStatusModalOpen(false);
      setSlotStatusAppointment(null);
    } finally {
      setUpdatingSlotStatus(false);
    }
  };

  return (
    <section className="space-y-4">
      <div className="panel p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="panel-title">Receptionist Dashboard</h2>
            <p className="mt-1 text-sm text-slate-500">
              {view === 'all' ? 'All appointments made in the system.' : 'Create patient first, then book from available slots.'}
            </p>
          </div>
          <button className="btn-primary" onClick={() => setPatientModalOpen(true)}>New Patient</button>
        </div>
      </div>

      <div className="panel p-5">
        {view === 'booking' ? (
          <div className="grid gap-3 md:grid-cols-4">
            <div>
              <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Date</label>
              <input type="date" min={today} className="field" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Specialization</label>
              <select className="field" value={selectedSpecialization} onChange={(e) => setSelectedSpecialization(e.target.value)}>
                <option value="">Select specialization</option>
                {specializationOptions.map((sp) => <option key={sp} value={sp}>{sp}</option>)}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Shift</label>
              <select className="field" value={selectedShift} onChange={(e) => setSelectedShift(e.target.value)} disabled={!selectedSpecialization}>
                <option value="">Select shift</option>
                {SHIFT_OPTIONS.map((shift) => <option key={shift} value={shift}>{shiftLabel(shift)}</option>)}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Doctor Name</label>
              <select className="field" value={doctorRef} onChange={(e) => setDoctorRef(e.target.value)} disabled={!selectedSpecialization || !selectedShift}>
                <option value="">Select doctor</option>
                {doctorsByShift.map((doctor) => (
                  <option key={doctor.userId} value={doctor.username}>{doctor.username} ({doctor.doctorRefCode || 'MEDID-NA'})</option>
                ))}
              </select>
            </div>
          </div>
        ) : (
          <div className="grid gap-3 md:grid-cols-3">
            <div className="md:col-span-3 flex items-end">
              <button className="btn-ghost w-full" onClick={loadAllAppointments}>Refresh Appointments</button>
            </div>
          </div>
        )}

        <div className="mt-4">
          <AsyncState
            loading={loading}
            error={error}
            empty={!loading && !error && (view === 'all' ? allAppointments.length === 0 : appointments.length === 0)}
            emptyMessage={view === 'booking' && !doctorRef ? 'Choose date, specialization, shift, and doctor to view slots.' : 'No appointments available.'}
          />

          {view === 'booking' ? (
            <>
              {doctorRef && selectedShift && selectedSpecialization && date ? (
                <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                  {slots.map((slot) => {
                    const appt = occupied.get(slot);
                    const blockedPast = isPastSlot(slot);
                    return (
                      <button
                        key={slot}
                        type="button"
                        className={`rounded-lg border p-3 text-left text-sm ${blockedPast ? 'border-slate-300 bg-slate-100 text-slate-500' : appt ? 'border-red-300 bg-red-100 text-red-800' : 'border-emerald-300 bg-emerald-100 text-emerald-800 hover:border-emerald-400'}`}
                        onClick={() => {
                          if (blockedPast) return;
                          if (!appt) openSlotBooking(slot);
                          else openSlotStatus(appt);
                        }}
                        disabled={blockedPast}
                      >
                        <p className="font-semibold">{slot}</p>
                        {blockedPast ? <p className="mt-1 text-xs">Past time</p> : appt ? <p className="mt-1 text-xs">Booked: {appt?.patient?.fullName || 'Unavailable'}</p> : <p className="mt-1 text-xs">Available</p>}
                      </button>
                    );
                  })}
                </div>
              ) : (
                <div className="mt-4 rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600">
                  Select date, specialization, shift, and doctor to view slots.
                </div>
              )}

            </>
          ) : null}

          {view === 'all' && !loading && !error ? (
            <>
              <div className="mb-3 grid gap-3 md:grid-cols-3">
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Patient Name</label>
                  <input className="field" value={allFilters.patientName} onChange={(e) => setAllFilters((s) => ({ ...s, patientName: e.target.value }))} placeholder="Patient name" />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Appointment ID</label>
                  <input className="field" value={allFilters.appointmentId} onChange={(e) => setAllFilters((s) => ({ ...s, appointmentId: e.target.value }))} placeholder="Appointment ID" />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Doctor Name</label>
                  <input className="field" value={allFilters.doctorName} onChange={(e) => setAllFilters((s) => ({ ...s, doctorName: e.target.value }))} placeholder="Doctor username" />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Doctor Ref ID</label>
                  <input className="field" value={allFilters.doctorRefCode} onChange={(e) => setAllFilters((s) => ({ ...s, doctorRefCode: e.target.value }))} placeholder="MEDID-XX" />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Status</label>
                  <select className="field" value={allFilters.status} onChange={(e) => setAllFilters((s) => ({ ...s, status: e.target.value }))}>
                    <option value="">All statuses</option>
                    <option value="SCHEDULED">SCHEDULED</option>
                    <option value="COMPLETED">COMPLETED</option>
                    <option value="CANCELLED">CANCELLED</option>
                    <option value="UNAVAILABLE">UNAVAILABLE</option>
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-slate-500">Date</label>
                  <input type="date" className="field" value={allFilters.date} onChange={(e) => setAllFilters((s) => ({ ...s, date: e.target.value }))} />
                </div>
                <div className="md:col-span-3">
                  <button className="btn-ghost w-full" onClick={() => setAllFilters({ patientName: '', appointmentId: '', doctorName: '', doctorRefCode: '', status: '', date: '' })}>Clear Filters</button>
                </div>
              </div>

              <div className="mt-6 overflow-x-auto rounded-xl border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Patient Name</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Appointment ID</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Doctor Ref ID</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Doctor Name</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Specialization</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Time</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600 whitespace-nowrap min-w-[140px]">Status</th>
                      {role === 'ADMIN' ? <th className="px-4 py-3 text-left font-semibold text-slate-600">Medical Blob</th> : null}
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Update</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {allAppointments.map((appt) => {
                      const doctor = doctorMeta.get(String(appt.doctorId));
                      return (
                        <tr key={appt.appointmentId}>
                          <td className="px-4 py-3">{appt?.patient?.fullName || 'Unknown'}</td>
                          <td className="px-4 py-3">{appt.appointmentId}</td>
                          <td className="px-4 py-3">{doctor?.doctorRefCode || 'MEDID-NA'}</td>
                          <td className="px-4 py-3">{doctor?.username || '-'}</td>
                          <td className="px-4 py-3">{doctor?.specialization || '-'}</td>
                          <td className="px-4 py-3">{appt?.appointmentTime || '-'}</td>
                          <td className="px-4 py-3 whitespace-nowrap"><span className={`pill ${statusPillClass(appt?.status || '-')}`}>{appt?.status || '-'}</span></td>
                          {role === 'ADMIN' ? <td className="px-4 py-3">{appt?.patient?.medicalHistoryBlob || '-'}</td> : null}
                          <td className="px-4 py-3">
                            <button
                              className="btn-ghost"
                              onClick={() => openSlotStatus(appt)}
                              disabled={String(appt.status || '').toUpperCase() === 'UNAVAILABLE'}
                            >
                              Update Status
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <Pagination page={allPage} pageSize={PAGE_SIZE} totalItems={totalAllAppointments} totalPages={totalAllPages} onPageChange={setAllPage} />
            </>
          ) : null}
        </div>
      </div>

      <Modal open={patientModalOpen} title="Create New Patient" onClose={() => setPatientModalOpen(false)}>
        <form className="space-y-4" onSubmit={createPatient}>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Patient Full Name</label>
              <input className="field" value={newPatient.fullName} onChange={(e) => setNewPatient((s) => ({ ...s, fullName: e.target.value }))} required />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Phone</label>
              <input className="field" value={newPatient.phoneNumber} onChange={(e) => setNewPatient((s) => ({ ...s, phoneNumber: e.target.value }))} required />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Gender</label>
              <input className="field" value={newPatient.gender} onChange={(e) => setNewPatient((s) => ({ ...s, gender: e.target.value }))} />
            </div>
            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium text-slate-700">Medical Blob</label>
              <textarea className="field min-h-24" value={newPatient.medicalHistoryBlob} onChange={(e) => setNewPatient((s) => ({ ...s, medicalHistoryBlob: e.target.value }))} />
            </div>
          </div>
          <button className="btn-primary w-full" disabled={creatingPatient}>{creatingPatient ? 'Creating...' : 'Create Patient'}</button>
        </form>
      </Modal>

      <Modal open={bookingModalOpen} title="Book Slot" onClose={() => setBookingModalOpen(false)}>
        <form className="space-y-4" onSubmit={bookSelectedPatient}>
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-600">
            Slot: <span className="font-semibold">{bookingSlot || '-'}</span>
          </div>
          <div className="relative">
            <label className="mb-1 block text-sm font-medium text-slate-700">Search Existing Patient (ID/Name)</label>
            <input
              className="field"
              value={bookingPatientQuery}
              onChange={(e) => setBookingPatientQuery(e.target.value)}
              placeholder="Type patient Ref ID or name"
              required
            />
            {searchingPatients ? <p className="mt-1 text-xs text-slate-500">Searching patients...</p> : null}
            {bookingPatientSuggestions.length > 0 ? (
              <div className="absolute z-20 mt-1 max-h-56 w-full overflow-auto rounded-lg border border-slate-200 bg-white shadow-lg">
                {bookingPatientSuggestions.map((patient) => (
                  <button
                    key={patient.patientId}
                    type="button"
                    className="block w-full border-b border-slate-100 px-3 py-2 text-left text-sm hover:bg-slate-50"
                    onClick={() => {
                      setSelectedBookingPatient(patient);
                      setBookingPatientQuery(`${patient.patientRefCode || `PAT-${String(patient.patientId || '').padStart(4, '0')}`} - ${patient.fullName}`);
                      setBookingPatientSuggestions([]);
                    }}
                  >
                    <p className="font-medium text-slate-900">{patient.patientRefCode || `PAT-${String(patient.patientId || '').padStart(4, '0')}`} - {patient.fullName}</p>
                    <p className="text-xs text-slate-500">{patient.phoneNumber || '-'} | {patient.gender || '-'}</p>
                  </button>
                ))}
              </div>
            ) : null}
          </div>

          {selectedBookingPatient ? (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">
              Selected patient: <span className="font-semibold">{selectedBookingPatient.patientRefCode || `PAT-${String(selectedBookingPatient.patientId || '').padStart(4, '0')}`} - {selectedBookingPatient.fullName}</span>
            </div>
          ) : null}

          <button className="btn-primary w-full" disabled={bookingAppointment || !selectedBookingPatient || !bookingSlot || !doctorRef}>
            {bookingAppointment ? 'Booking...' : 'Book Appointment'}
          </button>
        </form>
      </Modal>

      <Modal open={slotStatusModalOpen} title="Update Slot Status" onClose={() => setSlotStatusModalOpen(false)}>
        {slotStatusAppointment ? (
          <form className="space-y-4" onSubmit={submitSlotStatus}>
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
              <p><span className="font-semibold">Time:</span> {timeFromIso(slotStatusAppointment.appointmentTime)}</p>
              <p><span className="font-semibold">Patient:</span> {slotStatusAppointment?.patient?.fullName || 'Unknown'}</p>
              <p><span className="font-semibold">Current Status:</span> {slotStatusAppointment.status || '-'}</p>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">New Status</label>
              <select
                className="field"
                value={slotStatusValue}
                onChange={(e) => setSlotStatusValue(e.target.value)}
                disabled={String(slotStatusAppointment.status || '').toUpperCase() === 'UNAVAILABLE'}
              >
                {statusOptionsFor(slotStatusAppointment.status).map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </div>
            <button
              className="btn-primary w-full"
              disabled={updatingSlotStatus || String(slotStatusAppointment.status || '').toUpperCase() === 'UNAVAILABLE'}
            >
              {updatingSlotStatus ? 'Updating...' : 'Update Status'}
            </button>
          </form>
        ) : null}
      </Modal>
    </section>
  );
}
