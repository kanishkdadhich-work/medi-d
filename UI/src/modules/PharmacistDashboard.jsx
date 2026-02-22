import { useEffect, useMemo, useState } from 'react';
import AsyncState from '../components/AsyncState.jsx';
import Pagination from '../components/Pagination.jsx';
import { getApiErrorMessage } from '../api.js';

const PAGE_SIZE = 8;

function isExpired(expiryDate) {
  if (!expiryDate) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expiry = new Date(expiryDate);
  expiry.setHours(0, 0, 0, 0);
  return expiry < today;
}

export default function PharmacistDashboard({ api, notify, view = 'pending' }) {
  const [pending, setPending] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [allMedicines, setAllMedicines] = useState([]);
  const [selectedPrescription, setSelectedPrescription] = useState(null);

  const [loadingPending, setLoadingPending] = useState(false);
  const [loadingAlerts, setLoadingAlerts] = useState(false);
  const [loadingAllMedicines, setLoadingAllMedicines] = useState(false);
  const [savingMedicine, setSavingMedicine] = useState(false);
  const [deletingExpired, setDeletingExpired] = useState(false);

  const [pendingError, setPendingError] = useState('');
  const [alertsError, setAlertsError] = useState('');
  const [allMedicinesError, setAllMedicinesError] = useState('');

  const [newMedicine, setNewMedicine] = useState({
    name: '',
    stockCount: 0,
    minThreshold: 0,
    expiryDate: ''
  });
  const [pendingPage, setPendingPage] = useState(1);
  const [alertsPage, setAlertsPage] = useState(1);
  const [allPage, setAllPage] = useState(1);

  const loadPending = async () => {
    setLoadingPending(true);
    setPendingError('');
    try {
      const data = await api.getPendingPrescriptions();
      const clean = (Array.isArray(data) ? data : []).map((p) => ({
        prescriptionId: p.prescriptionId,
        status: p.status,
        patientName: p?.appointment?.patient?.fullName || 'Unknown Patient',
        medicines: (p.items || []).map((item) => ({
          name: item?.medicine?.name || 'Unknown Medicine',
          quantity: item?.quantity ?? 0
        }))
      }));
      setPending(clean);
      if (clean.length > 0 && !selectedPrescription) {
        setSelectedPrescription(clean[0]);
      }
    } catch (error) {
      setPendingError(getApiErrorMessage(error));
      setPending([]);
      setSelectedPrescription(null);
    } finally {
      setLoadingPending(false);
    }
  };

  const loadAlerts = async () => {
    setLoadingAlerts(true);
    setAlertsError('');
    try {
      const data = await api.getInventoryAlerts();
      const list = Array.isArray(data) ? data : [];
      setAlerts(list.filter((item) => Number(item.stockCount) <= Number(item.minThreshold)));
    } catch (error) {
      setAlertsError(getApiErrorMessage(error));
      setAlerts([]);
    } finally {
      setLoadingAlerts(false);
    }
  };

  const loadAllMedicines = async () => {
    setLoadingAllMedicines(true);
    setAllMedicinesError('');
    try {
      const data = await api.getAllMedicines();
      setAllMedicines(Array.isArray(data) ? data : []);
    } catch (error) {
      setAllMedicinesError(getApiErrorMessage(error));
      setAllMedicines([]);
    } finally {
      setLoadingAllMedicines(false);
    }
  };

  useEffect(() => {
    loadPending();
    loadAlerts();
    loadAllMedicines();
  }, []);

  useEffect(() => {
    setPendingPage(1);
  }, [pending.length]);

  useEffect(() => {
    setAlertsPage(1);
  }, [alerts.length]);

  useEffect(() => {
    setAllPage(1);
  }, [allMedicines.length]);

  const onDispense = async (id) => {
    try {
      await api.dispensePrescription(id);
      notify('success', `Prescription ${id} dispensed.`);
      setSelectedPrescription(null);
      await Promise.all([loadPending(), loadAlerts(), loadAllMedicines()]);
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    }
  };

  const onCreateMedicine = async (e) => {
    e.preventDefault();
    setSavingMedicine(true);
    try {
      await api.createMedicine({
        name: newMedicine.name,
        stockCount: Number(newMedicine.stockCount),
        minThreshold: Number(newMedicine.minThreshold),
        expiryDate: newMedicine.expiryDate || null
      });
      notify('success', 'Medicine added successfully.');
      setNewMedicine({ name: '', stockCount: 0, minThreshold: 0, expiryDate: '' });
      await Promise.all([loadAllMedicines(), loadAlerts()]);
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    } finally {
      setSavingMedicine(false);
    }
  };

  const pendingCount = useMemo(() => pending.length, [pending.length]);
  const pagedPending = pending.slice((pendingPage - 1) * PAGE_SIZE, pendingPage * PAGE_SIZE);
  const pagedAlerts = alerts.slice((alertsPage - 1) * PAGE_SIZE, alertsPage * PAGE_SIZE);
  const pagedAllMedicines = allMedicines.slice((allPage - 1) * PAGE_SIZE, allPage * PAGE_SIZE);
  const expiredCount = useMemo(() => allMedicines.filter((med) => isExpired(med.expiryDate)).length, [allMedicines]);

  const onDeleteExpired = async (medicineId) => {
    try {
      await api.deleteExpiredMedicine(medicineId);
      notify('success', 'Expired stock deleted.');
      await Promise.all([loadAllMedicines(), loadAlerts()]);
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    }
  };

  const onPurgeExpired = async () => {
    setDeletingExpired(true);
    try {
      const result = await api.purgeExpiredMedicines();
      notify('success', `Expired medicines removed: ${result?.deletedCount ?? 0}`);
      await Promise.all([loadAllMedicines(), loadAlerts()]);
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    } finally {
      setDeletingExpired(false);
    }
  };

  return (
    <section className="space-y-4">
      <div className="panel p-5">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="panel-title">Pharmacist Dashboard</h2>
            <p className="mt-1 text-sm text-slate-500">Patient queue, stock monitoring, and medicine inventory management.</p>
          </div>
          <div className="flex gap-2">
            <span className="pill bg-amber-100 text-amber-700">Pending: {pendingCount}</span>
            <button className="btn-ghost" onClick={() => { loadPending(); loadAlerts(); loadAllMedicines(); }}>Refresh All</button>
          </div>
        </div>
      </div>

      {view === 'pending' ? (
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="panel p-5">
            <h3 className="text-base font-semibold text-slate-900">Patients Awaiting Dispense</h3>
            <p className="mt-1 text-sm text-slate-500">Click patient name to view required medicines.</p>

            <div className="mt-4 space-y-3">
              <AsyncState
                loading={loadingPending}
                error={pendingError}
                empty={!loadingPending && !pendingError && pending.length === 0}
                emptyMessage="No pending prescriptions."
              />

              {!loadingPending && !pendingError && pending.length > 0 ? (
                pagedPending.map((prescription) => (
                  <button
                    key={prescription.prescriptionId}
                    type="button"
                    className={`w-full rounded-xl border p-4 text-left ${selectedPrescription?.prescriptionId === prescription.prescriptionId ? 'border-med-500 bg-med-50' : 'border-slate-200 hover:border-med-300'}`}
                    onClick={() => setSelectedPrescription(prescription)}
                  >
                    <p className="font-semibold text-slate-900">{prescription.patientName}</p>
                    <p className="mt-1 text-xs text-slate-500">Prescription ID: {prescription.prescriptionId}</p>
                  </button>
                ))
              ) : null}
              <Pagination page={pendingPage} pageSize={PAGE_SIZE} totalItems={pending.length} onPageChange={setPendingPage} />
            </div>
          </div>

          <div className="panel p-5">
            <h3 className="text-base font-semibold text-slate-900">Requirements</h3>
            {selectedPrescription ? (
              <div className="mt-4 space-y-4">
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                  <p className="font-semibold text-slate-900">{selectedPrescription.patientName}</p>
                  <p className="text-sm text-slate-500">Prescription ID: {selectedPrescription.prescriptionId}</p>
                </div>
                <ul className="list-disc space-y-1 pl-5 text-sm text-slate-700">
                  {selectedPrescription.medicines.map((med, idx) => (
                    <li key={`${selectedPrescription.prescriptionId}-${idx}`}>{med.name} x {med.quantity}</li>
                  ))}
                </ul>
                <button className="btn-success w-full" onClick={() => onDispense(selectedPrescription.prescriptionId)}>Dispense</button>
              </div>
            ) : (
              <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-6 text-sm text-slate-600">
                Select a patient to see required medicines.
              </div>
            )}
          </div>
        </div>
      ) : null}

      {view === 'alerts' ? (
        <div className="panel p-5">
          <h3 className="text-base font-semibold text-slate-900">Inventory Alerts</h3>
          <div className="mt-4">
            <AsyncState
              loading={loadingAlerts}
              error={alertsError}
              empty={!loadingAlerts && !alertsError && alerts.length === 0}
              emptyMessage="No low stock alerts."
            />
            {!loadingAlerts && !alertsError && alerts.length > 0 ? (
              <div className="overflow-x-auto rounded-xl border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Medicine</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Stock</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Threshold</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {pagedAlerts.map((med) => (
                      <tr key={med.medicineId} className="bg-rose-50/40">
                        <td className="px-4 py-3 font-medium text-slate-900">{med.name}</td>
                        <td className="px-4 py-3 text-rose-700">{med.stockCount}</td>
                        <td className="px-4 py-3">{med.minThreshold}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
            <Pagination page={alertsPage} pageSize={PAGE_SIZE} totalItems={alerts.length} onPageChange={setAlertsPage} />
          </div>
        </div>
      ) : null}

      {view === 'all' ? (
        <div className="panel p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h3 className="text-base font-semibold text-slate-900">All Medicines Available</h3>
            <div className="flex items-center gap-2">
              <span className="pill bg-rose-100 text-rose-700">Expired: {expiredCount}</span>
              <button className="btn-ghost" disabled={deletingExpired || expiredCount === 0} onClick={onPurgeExpired}>
                {deletingExpired ? 'Deleting...' : 'Delete All Expired'}
              </button>
            </div>
          </div>
          <div className="mt-4">
            <AsyncState
              loading={loadingAllMedicines}
              error={allMedicinesError}
              empty={!loadingAllMedicines && !allMedicinesError && allMedicines.length === 0}
              emptyMessage="No medicines available."
            />
            {!loadingAllMedicines && !allMedicinesError && allMedicines.length > 0 ? (
              <div className="overflow-x-auto rounded-xl border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Medicine</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Stock</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Threshold</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Expiry</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {pagedAllMedicines.map((med) => (
                      <tr key={med.medicineId}>
                        <td className="px-4 py-3 font-medium text-slate-900">{med.name}</td>
                        <td className="px-4 py-3">{med.stockCount}</td>
                        <td className="px-4 py-3">{med.minThreshold}</td>
                        <td className="px-4 py-3">
                          {med.expiryDate || '-'}
                          {isExpired(med.expiryDate) ? <span className="ml-2 rounded bg-rose-100 px-2 py-0.5 text-xs text-rose-700">Expired</span> : null}
                        </td>
                        <td className="px-4 py-3">
                          {isExpired(med.expiryDate) ? (
                            <button className="btn-ghost" onClick={() => onDeleteExpired(med.medicineId)}>Delete Expired</button>
                          ) : (
                            <span className="text-xs text-slate-400">-</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
            <Pagination page={allPage} pageSize={PAGE_SIZE} totalItems={allMedicines.length} onPageChange={setAllPage} />
          </div>
        </div>
      ) : null}

      {view === 'add' ? (
        <div className="panel p-5">
          <h3 className="text-base font-semibold text-slate-900">Add New Medicine</h3>
          <form className="mt-4 space-y-3" onSubmit={onCreateMedicine}>
            <input className="field" placeholder="Medicine name" value={newMedicine.name} onChange={(e) => setNewMedicine((s) => ({ ...s, name: e.target.value }))} required />
            <div className="grid gap-3 sm:grid-cols-2">
              <input className="field" type="number" placeholder="Stock count" value={newMedicine.stockCount} onChange={(e) => setNewMedicine((s) => ({ ...s, stockCount: e.target.value }))} required />
              <input className="field" type="number" placeholder="Min threshold" value={newMedicine.minThreshold} onChange={(e) => setNewMedicine((s) => ({ ...s, minThreshold: e.target.value }))} required />
            </div>
            <input className="field" type="date" value={newMedicine.expiryDate} onChange={(e) => setNewMedicine((s) => ({ ...s, expiryDate: e.target.value }))} />
            <button className="btn-primary w-full" disabled={savingMedicine}>{savingMedicine ? 'Saving...' : 'Add Medicine'}</button>
          </form>
        </div>
      ) : null}
    </section>
  );
}
