import { useEffect, useMemo, useState } from 'react';
import AsyncState from '../components/AsyncState.jsx';
import Pagination from '../components/Pagination.jsx';
import { getApiErrorMessage } from '../api.js';

const PAGE_SIZE = 8;

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
  const [queue, setQueue] = useState([]);
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
  const slots = useMemo(buildSlots, []);

  const [latestConsultation, setLatestConsultation] = useState(null);
  const [medicalBlobDraft, setMedicalBlobDraft] = useState('');
  const [savingBlob, setSavingBlob] = useState(false);

  const loadQueue = async () => {
    setLoadingQueue(true);
    setQueueError('');
    try {
      const data = await api.getMyDoctorQueue();
      setQueue(Array.isArray(data) ? data : []);
    } catch (error) {
      setQueueError(getApiErrorMessage(error));
      setQueue([]);
    } finally {
      setLoadingQueue(false);
    }
  };

  const loadMedicines = async () => {
    setLoadingMedicines(true);
    try {
      const data = await api.getAllMedicines();
      setAllMedicines(Array.isArray(data) ? data : []);
    } catch {
      setAllMedicines([]);
    } finally {
      setLoadingMedicines(false);
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
    loadQueue();
    loadMedicines();
  }, []);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      const term = medicineSearch.trim();
      try {
        const data = term ? await api.searchMedicines(term) : await api.getAllMedicines();
        if (!cancelled) {
          setAllMedicines(Array.isArray(data) ? data : []);
        }
      } catch {
        if (!cancelled) setAllMedicines([]);
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
  }, [queue.length, view]);

  useEffect(() => {
    if (!selectedAppointment) {
      setMedicalBlobDraft('');
      setLatestConsultation(null);
      return;
    }
    setMedicalBlobDraft(selectedAppointment?.patient?.medicalHistoryBlob || '');
    loadLatestConsultation(selectedAppointment?.patient?.patientId);
  }, [selectedAppointment]);

  const todayQueue = useMemo(() => {
    const today = new Date().toISOString().slice(0, 10);
    return queue.filter((item) => {
      const status = (item?.status || '').toUpperCase();
      const apptDate = item?.appointmentTime?.slice(0, 10);
      return apptDate === today && (status === 'SCHEDULED' || status === 'BOOKED');
    });
  }, [queue]);

  const completedQueue = useMemo(() => {
    return queue.filter((item) => (item?.status || '').toUpperCase() === 'COMPLETED');
  }, [queue]);

  const futureQueue = useMemo(() => {
    const now = new Date();
    return queue.filter((item) => {
      const status = (item?.status || '').toUpperCase();
      if (status === 'CANCELLED' || status === 'COMPLETED' || status === 'UNAVAILABLE') return false;
      const time = item?.appointmentTime ? new Date(item.appointmentTime) : null;
      return Boolean(time && time > now);
    });
  }, [queue]);

  const activeQueue = useMemo(() => {
    if (view === 'completed') return completedQueue;
    if (view === 'future') return futureQueue;
    return todayQueue;
  }, [view, completedQueue, futureQueue, todayQueue]);

  const pagedQueue = useMemo(() => {
    const start = (queuePage - 1) * PAGE_SIZE;
    return activeQueue.slice(start, start + PAGE_SIZE);
  }, [activeQueue, queuePage]);

  const filteredMedicines = useMemo(() => {
    return allMedicines;
  }, [allMedicines, medicineSearch]);

  const selectedMedicine = useMemo(
    () => filteredMedicines.find((m) => String(m.medicineId) === String(selectedMedicineId)),
    [filteredMedicines, selectedMedicineId]
  );

  const calendarOccupied = useMemo(() => {
    const occupied = new Map();
    queue.forEach((item) => {
      const day = item?.appointmentTime?.slice(0, 10);
      if (day !== calendarDate) return;
      const status = (item?.status || '').toUpperCase();
      if (['SCHEDULED', 'BOOKED', 'UNAVAILABLE'].includes(status)) {
        occupied.set(slotKeyFromIso(item.appointmentTime), item);
      }
    });
    return occupied;
  }, [queue, calendarDate]);

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

    setDraftMedicines((prev) => [
      ...prev,
      {
        medicineId: selectedMedicine.medicineId,
        medicineName: selectedMedicine.name,
        quantity: qty,
        instructions: 'After food'
      }
    ]);
  };

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
    const slotDateTime = `${calendarDate}T${slot}`;
    try {
      await api.markDoctorUnavailable(slotDateTime);
      notify('success', 'Slot marked unavailable.');
      await loadQueue();
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    }
  };

  const onClearUnavailable = async (slot) => {
    const slotDateTime = `${calendarDate}T${slot}`;
    try {
      await api.clearDoctorUnavailable(slotDateTime);
      notify('success', 'Unavailable slot cleared.');
      await loadQueue();
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
              <input type="date" className="field" value={calendarDate} onChange={(e) => setCalendarDate(e.target.value)} />
              <button className="btn-ghost" onClick={loadQueue}>Refresh</button>
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

              return (
                <div
                  key={slot}
                  className={`rounded-lg border p-3 text-sm ${isUnavailable ? 'border-slate-300 bg-slate-200 text-slate-700' : isBooked ? 'border-red-300 bg-red-100 text-red-800' : 'border-emerald-300 bg-emerald-100 text-emerald-800'}`}
                >
                  <p className="font-semibold">{slot}</p>
                  {isUnavailable ? <p className="mt-1 text-xs">Unavailable</p> : null}
                  {isBooked ? <p className="mt-1 text-xs">Booked: {occupied?.patient?.fullName || 'Patient'}</p> : null}
                  {!occupied ? <p className="mt-1 text-xs">Available</p> : null}

                  <div className="mt-2">
                    {!occupied ? (
                      <button className="btn-ghost" onClick={() => onMarkUnavailable(slot)}>Mark Unavailable</button>
                    ) : null}
                    {isUnavailable ? (
                      <button className="btn-ghost" onClick={() => onClearUnavailable(slot)}>Clear Unavailable</button>
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
              empty={!loadingQueue && !queueError && activeQueue.length === 0}
              emptyMessage={view === 'completed' ? 'No completed appointments.' : view === 'future' ? 'No future appointments.' : 'No scheduled patients for today.'}
            />

            {!loadingQueue && !queueError && activeQueue.length > 0 ? (
              pagedQueue.map((item) => (
                <button
                  key={item.appointmentId}
                  onClick={() => { setSelectedAppointment(item); setAppointmentTab('summary'); }}
                  className={`w-full rounded-xl border p-4 text-left transition ${selectedAppointment?.appointmentId === item.appointmentId ? 'border-med-500 bg-med-50' : 'border-slate-200 hover:border-med-300'}`}
                >
                  <p className="font-semibold text-slate-900">{item?.patient?.fullName || 'Slot Block'}</p>
                  <p className="text-sm text-slate-500">Phone: {item?.patient?.phoneNumber || '-'}</p>
                  <p className="text-sm text-slate-500">Time: {formatAppointmentTime(item?.appointmentTime)}</p>
                  <p className="mt-2 text-xs text-slate-500">Appointment ID: {item.appointmentId}</p>
                </button>
              ))
            ) : null}
            <Pagination page={queuePage} pageSize={PAGE_SIZE} totalItems={activeQueue.length} onPageChange={setQueuePage} />
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
                    <p>Status: {selectedAppointment.status}</p>
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
                      <option value="SCHEDULED">SCHEDULED</option>
                      <option value="COMPLETED">COMPLETED</option>
                      <option value="CANCELLED">CANCELLED</option>
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
                    <input
                      className="field"
                      placeholder="Search medicine"
                      value={medicineSearch}
                      onChange={(e) => setMedicineSearch(e.target.value)}
                    />
                    <div className="mt-2 grid gap-2 sm:grid-cols-[1fr_auto_auto]">
                      <select
                        className="field"
                        value={selectedMedicineId}
                        onChange={(e) => setSelectedMedicineId(e.target.value)}
                        disabled={loadingMedicines}
                      >
                        <option value="">Select medicine</option>
                        {filteredMedicines.map((med) => (
                          <option key={med.medicineId} value={med.medicineId}>{med.name} (Stock: {med.stockCount})</option>
                        ))}
                      </select>
                      <input className="field w-24" type="number" min="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
                      <button className="btn-ghost" onClick={addSelectedMedicine}>Add</button>
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

                  <button className="btn-primary w-full" onClick={submitPrescription} disabled={submitting}>
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
