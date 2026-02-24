import { useEffect, useMemo, useState } from 'react';
import AsyncState from '../components/AsyncState.jsx';
import Pagination from '../components/Pagination.jsx';
import { getApiErrorMessage } from '../api.js';

const PAGE_SIZE = 8;

function statusPillClass(status) {
  const s = String(status || '').toUpperCase();
  if (s === 'COMPLETED') return 'bg-emerald-100 text-emerald-700';
  if (s === 'CANCELLED') return 'bg-rose-100 text-rose-700';
  if (s === 'UNAVAILABLE') return 'bg-slate-200 text-slate-700';
  return 'bg-amber-100 text-amber-700';
}

function formatAppointmentTime(value) {
  if (!value) return '-';
  try {
    return new Date(value).toLocaleString([], {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return value;
  }
}

function toDateInput(date = new Date()) {
  return date.toISOString().slice(0, 10);
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

export default function DoctorDashboard({ api, notify, view = 'today' }) {
  const today = toDateInput();
  const [queue, setQueue] = useState([]);
  const [totalQueueItems, setTotalQueueItems] = useState(0);
  const [totalQueuePages, setTotalQueuePages] = useState(1);
  const [loadingQueue, setLoadingQueue] = useState(false);
  const [queueError, setQueueError] = useState('');
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [appointmentTab, setAppointmentTab] = useState('summary');

  const [diagnosisNotes, setDiagnosisNotes] = useState('');
  const [medicineSearch, setMedicineSearch] = useState('');
  const [allMedicines, setAllMedicines] = useState([]);
  const [selectedMedicineId, setSelectedMedicineId] = useState('');
  const [loadingMedicines, setLoadingMedicines] = useState(false);
  const [draftMedicines, setDraftMedicines] = useState([]);
  const [quantity, setQuantity] = useState('1');
  const [submitting, setSubmitting] = useState(false);
  const [queuePage, setQueuePage] = useState(1);

  const [calendarDate, setCalendarDate] = useState(toDateInput());
  const [calendarAppointments, setCalendarAppointments] = useState([]);
  const slots = useMemo(buildSlots, []);

  const [latestConsultation, setLatestConsultation] = useState(null);
  const [medicalBlobDraft, setMedicalBlobDraft] = useState('');
  const [savingBlob, setSavingBlob] = useState(false);
  const [showMedicineLookup, setShowMedicineLookup] = useState(false);
  const now = new Date();

  useEffect(() => {
    if (calendarDate < today) {
      setCalendarDate(today);
    }
  }, [calendarDate, today]);

  const filterQueueByView = (items, currentView) => {
    const list = Array.isArray(items) ? items : [];
    const now = new Date();
    const today = now.toISOString().slice(0, 10);
    if (currentView === 'completed') {
      return list.filter((item) => String(item?.status || '').toUpperCase() === 'COMPLETED');
    }
    if (currentView === 'future') {
      return list.filter((item) => {
        const status = String(item?.status || '').toUpperCase();
        if (['CANCELLED', 'COMPLETED', 'UNAVAILABLE'].includes(status)) return false;
        const when = item?.appointmentTime ? new Date(item.appointmentTime) : null;
        return Boolean(when && when > now);
      });
    }
    return list.filter((item) => {
      const status = String(item?.status || '').toUpperCase();
      const apptDate = item?.appointmentTime?.slice(0, 10);
      return apptDate === today && (status === 'SCHEDULED' || status === 'BOOKED');
    });
  };

  const loadQueue = async () => {
    setLoadingQueue(true);
    setQueueError('');
    try {
      const all = await api.getMyDoctorQueue();
      const filtered = filterQueueByView(all, view);
      const start = (queuePage - 1) * PAGE_SIZE;
      setQueue(filtered.slice(start, start + PAGE_SIZE));
      setTotalQueueItems(filtered.length);
      setTotalQueuePages(Math.max(1, Math.ceil(filtered.length / PAGE_SIZE)));
    } catch (error) {
      setQueueError(getApiErrorMessage(error));
      setQueue([]);
      setTotalQueueItems(0);
      setTotalQueuePages(1);
    } finally {
      setLoadingQueue(false);
    }
  };

  const loadCalendarAppointments = async () => {
    try {
      const data = await api.getMyDoctorQueue();
      setCalendarAppointments(Array.isArray(data) ? data : []);
    } catch {
      setCalendarAppointments([]);
    }
  };

  const loadLatestConsultation = async (patientId) => {
    if (!patientId) {
      setLatestConsultation(null);
      return;
    }
    try {
      const data = await api.getLatestConsultation(patientId);
      setLatestConsultation(data || null);
    } catch {
      setLatestConsultation(null);
    }
  };

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      const term = medicineSearch.trim();
      setLoadingMedicines(true);
      try {
        const data = term
          ? await api.searchMedicines(term)
          : await api.getAllMedicines();
        if (!cancelled) {
          if (term) {
            setAllMedicines(Array.isArray(data) ? data : []);
          } else {
            setAllMedicines(Array.isArray(data) ? data : []);
          }
        }
      } catch {
        if (!cancelled) setAllMedicines([]);
      } finally {
        if (!cancelled) setLoadingMedicines(false);
      }
    };

    const handle = setTimeout(run, 250);
    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [api, medicineSearch]);

  useEffect(() => {
    setQueuePage(1);
  }, [view]);

  useEffect(() => {
    if (view === 'calendar') {
      loadCalendarAppointments();
      return;
    }
    loadQueue();
  }, [view, queuePage, calendarDate]);

  useEffect(() => {
    if (!selectedAppointment) {
      setMedicalBlobDraft('');
      setLatestConsultation(null);
      return;
    }
    setMedicalBlobDraft(selectedAppointment?.patient?.medicalHistoryBlob || '');
    loadLatestConsultation(selectedAppointment?.patient?.patientId);
  }, [selectedAppointment]);

  const filteredMedicines = useMemo(() => {
    const grouped = new Map();
    allMedicines.forEach((med) => {
      const key = String(med?.name || '').trim().toLowerCase();
      if (!key) return;
      if (!grouped.has(key)) {
        grouped.set(key, {
          medicineId: med.medicineId,
          name: med.name,
          stockCount: Number(med.stockCount || 0),
          expiryDate: med.expiryDate || null
        });
        return;
      }
      const current = grouped.get(key);
      current.stockCount += Number(med.stockCount || 0);
      const currentExpiry = current.expiryDate ? new Date(current.expiryDate) : null;
      const medExpiry = med.expiryDate ? new Date(med.expiryDate) : null;
      if (!currentExpiry || (medExpiry && medExpiry < currentExpiry)) {
        current.medicineId = med.medicineId;
        current.expiryDate = med.expiryDate || null;
      }
      grouped.set(key, current);
    });
    return Array.from(grouped.values()).sort((a, b) => a.name.localeCompare(b.name));
  }, [allMedicines]);

  const selectedMedicine = useMemo(
    () => filteredMedicines.find((m) => String(m.medicineId) === String(selectedMedicineId)),
    [filteredMedicines, selectedMedicineId]
  );

  const calendarOccupied = useMemo(() => {
    // Calendar blocks only actionable statuses (booked/scheduled/unavailable).
    const occupied = new Map();
    calendarAppointments.forEach((item) => {
      const day = item?.appointmentTime?.slice(0, 10);
      if (day !== calendarDate) return;
      const status = (item?.status || '').toUpperCase();
      if (['SCHEDULED', 'BOOKED', 'UNAVAILABLE'].includes(status)) {
        occupied.set(slotKeyFromIso(item.appointmentTime), item);
      }
    });
    return occupied;
  }, [calendarAppointments, calendarDate]);

  const addSelectedMedicine = () => {
    if (!selectedMedicine) {
      notify('error', 'Select a medicine first.');
      return;
    }

    const qty = Number(quantity);
    if (!qty || qty < 1) {
      notify('error', 'Quantity must be at least 1.');
      return;
    }

    setDraftMedicines((prev) => {
      const index = prev.findIndex((item) => String(item.medicineName).toLowerCase() === String(selectedMedicine.name).toLowerCase());
      if (index < 0) {
        return [
          ...prev,
          {
            medicineId: selectedMedicine.medicineId,
            medicineName: selectedMedicine.name,
            quantity: qty,
            instructions: 'After food'
          }
        ];
      }
      return prev.map((item, i) => (i === index ? { ...item, quantity: item.quantity + qty } : item));
    });
  };
  const canAddMedicine = Boolean(selectedMedicineId && Number(quantity) >= 1);
  const canPrescribe = Boolean(selectedAppointment && String(selectedAppointment.status || '').toUpperCase() !== 'COMPLETED');

  const removeDraftMedicine = (index) => {
    setDraftMedicines((prev) => prev.filter((_, i) => i !== index));
  };

  const updateStatus = async (status) => {
    if (!selectedAppointment) return;
    try {
      await api.updateAppointmentStatus(selectedAppointment.appointmentId, status);
      notify('success', 'Appointment status updated.');
      await loadQueue();
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    }
  };

  const statusOptionsFor = (currentStatus) => {
    const current = String(currentStatus || '').toUpperCase();
    if (current === 'COMPLETED') return ['COMPLETED'];
    if (current === 'CANCELLED') return ['CANCELLED'];
    if (current === 'SCHEDULED' || current === 'BOOKED') return ['SCHEDULED', 'CANCELLED', 'COMPLETED'];
    if (current === 'UNAVAILABLE') return ['UNAVAILABLE', 'CANCELLED'];
    return ['SCHEDULED', 'COMPLETED', 'CANCELLED'];
  };

  const saveMedicalBlob = async () => {
    if (!selectedAppointment?.patient?.patientId) return;
    setSavingBlob(true);
    try {
      await api.updatePatientMedicalBlob(selectedAppointment.patient.patientId, medicalBlobDraft);
      notify('success', 'Medical blob updated.');
      await loadQueue();
      await loadLatestConsultation(selectedAppointment.patient.patientId);
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    } finally {
      setSavingBlob(false);
    }
  };

  const submitPrescription = async () => {
    if (!selectedAppointment) {
      notify('error', 'Select a patient first.');
      return;
    }
    if (!canPrescribe) {
      notify('error', 'Completed appointments cannot be prescribed again.');
      return;
    }
    if (!diagnosisNotes.trim() || draftMedicines.length === 0) {
      notify('error', 'Diagnosis notes and at least one medicine are required.');
      return;
    }

    setSubmitting(true);
    try {
      await api.createPrescription({
        appointmentId: selectedAppointment.appointmentId,
        diagnosisNotes,
        items: draftMedicines.map((item) => ({
          medicineId: item.medicineId,
          quantity: item.quantity,
          instructions: item.instructions
        }))
      });
      notify('success', 'Prescription created successfully.');
      setDiagnosisNotes('');
      setDraftMedicines([]);
      await loadQueue();
      await loadLatestConsultation(selectedAppointment?.patient?.patientId);
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  const onMarkUnavailable = async (slot) => {
    // Doctor marks slot-level availability from calendar view.
    const slotDateTime = `${calendarDate}T${slot}`;
    try {
      await api.markDoctorUnavailable(slotDateTime);
      notify('success', 'Slot marked unavailable.');
      await loadCalendarAppointments();
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    }
  };

  const onClearUnavailable = async (slot) => {
    const slotDateTime = `${calendarDate}T${slot}`;
    try {
      await api.clearDoctorUnavailable(slotDateTime);
      notify('success', 'Unavailable slot cleared.');
      await loadCalendarAppointments();
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    }
  };

  if (view === 'calendar') {
    return (
      <section className="space-y-4">
        <div className="panel p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="panel-title">Doctor Calendar</h2>
              <p className="mt-1 text-sm text-slate-500">Mark your slots unavailable or clear unavailable slots.</p>
            </div>
            <div className="flex gap-2">
              <input type="date" min={today} className="field" value={calendarDate} onChange={(e) => setCalendarDate(e.target.value)} />
              <button className="btn-ghost" onClick={loadCalendarAppointments}>Refresh</button>
            </div>
          </div>
        </div>

        <div className="panel p-5">
          <AsyncState
            loading={loadingQueue}
            error={queueError}
            empty={false}
          />
          <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {slots.map((slot) => {
              const occupied = calendarOccupied.get(slot);
              const status = (occupied?.status || '').toUpperCase();
              const isUnavailable = status === 'UNAVAILABLE';
              const isBooked = status === 'SCHEDULED' || status === 'BOOKED';
              const isPast = new Date(`${calendarDate}T${slot}:00`) < now;

              return (
                <div
                  key={slot}
                  className={`rounded-lg border p-3 text-sm ${isPast ? 'border-slate-300 bg-slate-100 text-slate-500' : isUnavailable ? 'border-slate-300 bg-slate-200 text-slate-700' : isBooked ? 'border-red-300 bg-red-100 text-red-800' : 'border-emerald-300 bg-emerald-100 text-emerald-800'}`}
                >
                  <p className="font-semibold">{slot}</p>
                  {isPast ? <p className="mt-1 text-xs">Past slot</p> : null}
                  {isUnavailable ? <p className="mt-1 text-xs">Unavailable</p> : null}
                  {isBooked ? <p className="mt-1 text-xs">Booked: {occupied?.patient?.fullName || 'Patient'}</p> : null}
                  {!occupied ? <p className="mt-1 text-xs">Available</p> : null}

                  <div className="mt-2">
                    {!occupied ? (
                      <button className="btn-ghost" onClick={() => onMarkUnavailable(slot)} disabled={isPast}>Mark Unavailable</button>
                    ) : null}
                    {isUnavailable ? (
                      <button className="btn-ghost" onClick={() => onClearUnavailable(slot)} disabled={isPast}>Clear Unavailable</button>
                    ) : null}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="space-y-4">
      <div className="panel p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="panel-title">Doctor Dashboard</h2>
            <p className="mt-1 text-sm text-slate-500">
              {view === 'completed' ? 'Completed appointments.' : view === 'future' ? 'Upcoming appointments.' : 'Today\'s active queue.'}
            </p>
          </div>
          <button className="btn-ghost" onClick={loadQueue}>Refresh Queue</button>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="panel p-5">
          <h3 className="text-base font-semibold text-slate-900">Appointments</h3>

          <div className="mt-4 space-y-3">
            <AsyncState
              loading={loadingQueue}
              error={queueError}
              empty={!loadingQueue && !queueError && queue.length === 0}
              emptyMessage={view === 'completed' ? 'No completed appointments.' : view === 'future' ? 'No future appointments.' : 'No scheduled patients for today.'}
            />

            {!loadingQueue && !queueError && queue.length > 0 ? (
              queue.map((item) => (
                <button
                  key={item.appointmentId}
                  onClick={() => { setSelectedAppointment(item); setAppointmentTab('summary'); }}
                  className={`w-full rounded-xl border p-4 text-left transition ${selectedAppointment?.appointmentId === item.appointmentId ? 'border-med-500 bg-med-50' : 'border-slate-200 hover:border-med-300'}`}
                >
                  <p className="font-semibold text-slate-900">{item?.patient?.fullName || 'Slot Block'}</p>
                  <p className="text-sm text-slate-500">Phone: {item?.patient?.phoneNumber || '-'}</p>
                  <p className="text-sm text-slate-500">Time: {formatAppointmentTime(item?.appointmentTime)}</p>
                  <p className="mt-2"><span className={`pill ${statusPillClass(item?.status)}`}>{item?.status || 'SCHEDULED'}</span></p>
                  <p className="mt-2 text-xs text-slate-500">Appointment ID: {item.appointmentId}</p>
                </button>
              ))
            ) : null}
            <Pagination page={queuePage} pageSize={PAGE_SIZE} totalItems={totalQueueItems} totalPages={totalQueuePages} onPageChange={setQueuePage} />
          </div>
        </div>

        <div className="panel p-5">
          <h3 className="text-base font-semibold text-slate-900">Appointment Workspace</h3>
          {selectedAppointment ? (
            <>
              <div className="mt-3 flex gap-2">
                <button className={appointmentTab === 'summary' ? 'btn-primary' : 'btn-ghost'} onClick={() => setAppointmentTab('summary')}>Summary</button>
                <button className={appointmentTab === 'consultation' ? 'btn-primary' : 'btn-ghost'} onClick={() => setAppointmentTab('consultation')}>Consultation</button>
              </div>

              {appointmentTab === 'summary' ? (
                <div className="mt-4 space-y-3">
                  <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
                    <p className="font-semibold text-slate-900">{selectedAppointment?.patient?.fullName || 'Unknown Patient'}</p>
                    <p>Appointment ID: {selectedAppointment.appointmentId}</p>
                    <p>Status: <span className={`pill ${statusPillClass(selectedAppointment.status)}`}>{selectedAppointment.status}</span></p>
                    <p>Time: {formatAppointmentTime(selectedAppointment?.appointmentTime)}</p>
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-slate-700">Patient Medical Blob</label>
                    <textarea className="field min-h-24" value={medicalBlobDraft} onChange={(e) => setMedicalBlobDraft(e.target.value)} />
                    <button className="btn-ghost mt-2" onClick={saveMedicalBlob} disabled={savingBlob}>{savingBlob ? 'Saving...' : 'Save Medical Blob'}</button>
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-slate-700">Update Status</label>
                    <select className="field" defaultValue={selectedAppointment.status || 'SCHEDULED'} onChange={(e) => updateStatus(e.target.value)}>
                      {statusOptionsFor(selectedAppointment.status).map((status) => (
                        <option key={status} value={status}>{status}</option>
                      ))}
                    </select>
                  </div>

                  <div className="rounded-xl border border-slate-200 bg-white p-3 text-sm">
                    <p className="font-semibold text-slate-900">Latest Consultation</p>
                    {latestConsultation ? (
                      <>
                        <p className="text-slate-600">Updated: {formatAppointmentTime(latestConsultation.updatedAt)}</p>
                        <p className="mt-2 text-slate-700">{latestConsultation.diagnosisNotes || 'No diagnosis notes.'}</p>
                        <ul className="mt-2 list-disc pl-5 text-slate-700">
                          {(latestConsultation.medicines || []).map((item, idx) => <li key={`${idx}-${item}`}>{item}</li>)}
                        </ul>
                      </>
                    ) : (
                      <p className="text-slate-500">No consultation found for this patient.</p>
                    )}
                  </div>
                </div>
              ) : (
                <div className="mt-4 space-y-4">
                  <div>
                    <label className="mb-1 block text-sm font-medium text-slate-700">Diagnosis Notes</label>
                    <textarea className="field min-h-24" value={diagnosisNotes} onChange={(e) => setDiagnosisNotes(e.target.value)} />
                  </div>

                  <div className="rounded-xl border border-slate-200 p-3">
                    <label className="mb-2 block text-sm font-medium text-slate-700">Select Medicines</label>
                    <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
                      <input
                        className="field"
                        placeholder="Type medicine name"
                        value={medicineSearch}
                        onChange={(e) => {
                          setMedicineSearch(e.target.value);
                          setShowMedicineLookup(true);
                        }}
                      />
                      <button className="btn-ghost" type="button" onClick={() => setShowMedicineLookup((s) => !s)}>
                        {showMedicineLookup ? 'Hide' : 'Search'}
                      </button>
                    </div>
                    {showMedicineLookup && filteredMedicines.length > 0 ? (
                      <div className="mt-2 max-h-44 overflow-auto rounded-lg border border-slate-200 bg-white">
                        {filteredMedicines.slice(0, 12).map((med) => (
                          <button
                            key={med.medicineId}
                            type="button"
                            className={`block w-full border-b border-slate-100 px-3 py-2 text-left text-sm hover:bg-slate-50 ${String(selectedMedicineId) === String(med.medicineId) ? 'bg-med-50 text-med-700' : 'text-slate-700'}`}
                            onClick={() => {
                              setSelectedMedicineId(String(med.medicineId));
                              setShowMedicineLookup(false);
                            }}
                          >
                            {med.name}
                          </button>
                        ))}
                      </div>
                    ) : null}
                    <div className="mt-2 grid gap-2 sm:grid-cols-[1fr_auto_auto]">
                      <input className="field" value={selectedMedicine?.name || ''} readOnly placeholder="Selected medicine" />
                      <input className="field w-24" type="number" min="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
                      <button className="btn-ghost" onClick={addSelectedMedicine} disabled={!canAddMedicine || !canPrescribe}>Add</button>
                    </div>
                  </div>

                  <div>
                    <h4 className="mb-2 text-sm font-semibold text-slate-800">Prescription Draft</h4>
                    {draftMedicines.length === 0 ? (
                      <p className="text-sm text-slate-500">No medicines added yet.</p>
                    ) : (
                      <ul className="space-y-2 text-sm">
                        {draftMedicines.map((item, index) => (
                          <li key={`${item.medicineId}-${index}`} className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2">
                            <span>{item.medicineName} x {item.quantity}</span>
                            <button className="btn-ghost" onClick={() => removeDraftMedicine(index)}>Remove</button>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>

                  <button className="btn-primary w-full" onClick={submitPrescription} disabled={submitting || !canPrescribe}>
                    {submitting ? 'Submitting...' : 'Create Prescription'}
                  </button>
                </div>
              )}
            </>
          ) : (
            <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-6 text-sm text-slate-600">
              Select an appointment to open consultation details.
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
