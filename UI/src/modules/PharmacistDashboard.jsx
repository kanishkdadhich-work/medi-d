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

  const [pendingTotal, setPendingTotal] = useState(0);
  const [pendingPages, setPendingPages] = useState(1);
  const [alertsTotal, setAlertsTotal] = useState(0);
  const [alertsPages, setAlertsPages] = useState(1);
  const [allTotal, setAllTotal] = useState(0);
  const [allPages, setAllPages] = useState(1);

  const [loadingPending, setLoadingPending] = useState(false);
  const [loadingAlerts, setLoadingAlerts] = useState(false);
  const [loadingAllMedicines, setLoadingAllMedicines] = useState(false);
  const [savingMedicine, setSavingMedicine] = useState(false);

  const [pendingError, setPendingError] = useState('');
  const [alertsError, setAlertsError] = useState('');
  const [allMedicinesError, setAllMedicinesError] = useState('');

  const [newMedicine, setNewMedicine] = useState({
    name: '',
    stockCount: '',
    minThreshold: '',
    expiryDate: ''
  });
  const [pendingPage, setPendingPage] = useState(1);
  const [alertsPage, setAlertsPage] = useState(1);
  const [allPage, setAllPage] = useState(1);
  const [stockDraft, setStockDraft] = useState({});
  const [updatingStockId, setUpdatingStockId] = useState(null);
  const toSafeText = (value) => String(value || '').replace(/[<>]/g, '').trimStart();
  const parseNonNegativeInt = (value) => {
    if (value === '' || value === null || value === undefined) return null;
    const parsed = Number(value);
    if (!Number.isInteger(parsed) || parsed < 0) return null;
    return parsed;
  };

  const canCreateMedicine = Boolean(
    String(newMedicine.name || '').trim()
    && parseNonNegativeInt(newMedicine.stockCount) !== null
    && parseNonNegativeInt(newMedicine.minThreshold) !== null
  );

  const loadPending = async () => {
    setLoadingPending(true);
    setPendingError('');
    try {
      const data = await api.getPendingPrescriptions();
      const source = Array.isArray(data) ? data : [];
      const clean = source.map((p) => ({
        prescriptionId: p.prescriptionId,
        status: p.status,
        patientName: p.patientName || p?.appointment?.patient?.fullName || 'Unknown Patient',
        medicines: (p.medicineNames || (p.items || []).map((item) => `${item?.medicine?.name || 'Unknown'} x ${item?.quantity || 0}`)).map((name) => ({ name, quantity: '' }))
      }));
      const start = (pendingPage - 1) * PAGE_SIZE;
      setPending(clean.slice(start, start + PAGE_SIZE));
      setPendingTotal(clean.length);
      setPendingPages(Math.max(1, Math.ceil(clean.length / PAGE_SIZE)));
      if (clean.length > 0 && !selectedPrescription) {
        setSelectedPrescription(clean[0]);
      }
    } catch (error) {
      setPendingError(getApiErrorMessage(error));
      setPending([]);
      setPendingTotal(0);
      setPendingPages(1);
      setSelectedPrescription(null);
    } finally {
      setLoadingPending(false);
    }
  };

  const loadAlerts = async () => {
    setLoadingAlerts(true);
    setAlertsError('');
    try {
      const list = await api.getInventoryAlerts();
      const arr = Array.isArray(list) ? list : [];
      const slice = arr.slice((alertsPage - 1) * PAGE_SIZE, alertsPage * PAGE_SIZE);
      setAlerts(slice);
      setAlertsTotal(arr.length);
      setAlertsPages(Math.max(1, Math.ceil(arr.length / PAGE_SIZE)));
    } catch (error) {
      setAlertsError(getApiErrorMessage(error));
      setAlerts([]);
      setAlertsTotal(0);
      setAlertsPages(1);
    } finally {
      setLoadingAlerts(false);
    }
  };

  const loadAllMedicines = async () => {
    setLoadingAllMedicines(true);
    setAllMedicinesError('');
    try {
      const list = await api.getAllMedicines();
      const arr = Array.isArray(list) ? list : [];
      const slice = arr.slice((allPage - 1) * PAGE_SIZE, allPage * PAGE_SIZE);
      setAllMedicines(slice);
      setAllTotal(arr.length);
      setAllPages(Math.max(1, Math.ceil(arr.length / PAGE_SIZE)));
    } catch (error) {
      setAllMedicinesError(getApiErrorMessage(error));
      setAllMedicines([]);
      setAllTotal(0);
      setAllPages(1);
    } finally {
      setLoadingAllMedicines(false);
    }
  };

  useEffect(() => {
    if (view === 'pending') {
      loadPending();
    }
  }, [view, pendingPage]);

  useEffect(() => {
    if (view === 'alerts') {
      loadAlerts();
    }
  }, [view, alertsPage]);

  useEffect(() => {
    if (view === 'all') {
      loadAllMedicines();
    }
  }, [view, allPage]);

  const onDispense = async (id) => {
    // Dispense endpoint is transactional and stock-safe server-side.
    try {
      await api.dispensePrescription(id);
      notify('success', `Prescription ${id} dispensed.`);
      setSelectedPrescription(null);
      await loadPending();
      await loadAlerts();
      await loadAllMedicines();
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    }
  };

  const onCreateMedicine = async (e) => {
    e.preventDefault();
    setSavingMedicine(true);
    try {
      await api.createMedicine({
        name: toSafeText(newMedicine.name),
        stockCount: parseNonNegativeInt(newMedicine.stockCount),
        minThreshold: parseNonNegativeInt(newMedicine.minThreshold),
        expiryDate: newMedicine.expiryDate || null
      });
      notify('success', 'Medicine added successfully.');
      setNewMedicine({ name: '', stockCount: '', minThreshold: '', expiryDate: '' });
      await loadAllMedicines();
      await loadAlerts();
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    } finally {
      setSavingMedicine(false);
    }
  };

  const pendingCount = useMemo(() => pendingTotal, [pendingTotal]);
  const expiredCount = useMemo(() => allMedicines.filter((med) => isExpired(med.expiryDate)).length, [allMedicines]);
  const hasChangedStock = (med) => {
    const draft = stockDraft[med.medicineId];
    if (draft === undefined || draft === null || draft === '') return false;
    const parsed = Number(draft);
    return Number.isFinite(parsed) && parsed >= 0 && parsed !== Number(med.stockCount);
  };

  const onDeleteExpired = async (medicineId) => {
    try {
      await api.deleteExpiredMedicine(medicineId);
      notify('success', 'Expired stock deleted.');
      await loadAllMedicines();
      await loadAlerts();
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    }
  };

  const onUpdateStock = async (medicineId) => {
    const nextStock = Number(stockDraft[medicineId]);
    if (Number.isNaN(nextStock) || nextStock < 0) {
      notify('error', 'Stock count must be zero or greater.');
      return;
    }
    setUpdatingStockId(medicineId);
    try {
      await api.updateMedicineStock(medicineId, nextStock);
      notify('success', 'Medicine stock updated.');
      await loadAllMedicines();
      await loadAlerts();
    } catch (error) {
      notify('error', getApiErrorMessage(error));
    } finally {
      setUpdatingStockId(null);
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
                pending.map((prescription) => (
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
              <Pagination page={pendingPage} pageSize={PAGE_SIZE} totalItems={pendingTotal} totalPages={pendingPages} onPageChange={setPendingPage} />
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
                    <li key={`${selectedPrescription.prescriptionId}-${idx}`}>{med.name}</li>
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
                    {alerts.map((med) => (
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
            <Pagination page={alertsPage} pageSize={PAGE_SIZE} totalItems={alertsTotal} totalPages={alertsPages} onPageChange={setAlertsPage} />
          </div>
        </div>
      ) : null}

      {view === 'all' ? (
        <div className="panel p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h3 className="text-base font-semibold text-slate-900">All Medicines Available</h3>
            <div className="flex items-center gap-2">
              <span className="pill bg-rose-100 text-rose-700">Expired: {expiredCount}</span>
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
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Update Stock</th>
                      <th className="px-4 py-3 text-left font-semibold text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {allMedicines.map((med) => (
                      <tr key={med.medicineId}>
                        <td className="px-4 py-3 font-medium text-slate-900">{med.name}</td>
                        <td className="px-4 py-3">{med.stockCount}</td>
                        <td className="px-4 py-3">{med.minThreshold}</td>
                        <td className="px-4 py-3">
                          {med.expiryDate || '-'}
                          {isExpired(med.expiryDate) ? <span className="ml-2 rounded bg-rose-100 px-2 py-0.5 text-xs text-rose-700">Expired</span> : null}
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <input
                              className="field w-24"
                              type="number"
                              min="0"
                              value={stockDraft[med.medicineId] ?? med.stockCount}
                              onChange={(e) => setStockDraft((s) => ({ ...s, [med.medicineId]: e.target.value }))}
                            />
                            <button
                              className="btn-ghost"
                              onClick={() => onUpdateStock(med.medicineId)}
                              disabled={updatingStockId === med.medicineId || !hasChangedStock(med)}
                            >
                              {updatingStockId === med.medicineId ? 'Saving...' : 'Update'}
                            </button>
                          </div>
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
            <Pagination page={allPage} pageSize={PAGE_SIZE} totalItems={allTotal} totalPages={allPages} onPageChange={setAllPage} />
          </div>
        </div>
      ) : null}

      {view === 'add' ? (
        <div className="panel p-5">
          <h3 className="text-base font-semibold text-slate-900">Add New Medicine</h3>
          <form className="mt-4 space-y-3" onSubmit={onCreateMedicine}>
            <input className="field" placeholder="Medicine name" value={newMedicine.name} onChange={(e) => setNewMedicine((s) => ({ ...s, name: toSafeText(e.target.value) }))} required />
            <div className="grid gap-3 sm:grid-cols-2">
              <input className="field" type="number" min="0" step="1" placeholder="Stock count" value={newMedicine.stockCount} onChange={(e) => setNewMedicine((s) => ({ ...s, stockCount: e.target.value }))} required />
              <input className="field" type="number" min="0" step="1" placeholder="Min threshold" value={newMedicine.minThreshold} onChange={(e) => setNewMedicine((s) => ({ ...s, minThreshold: e.target.value }))} required />
            </div>
            <input className="field" type="date" value={newMedicine.expiryDate} onChange={(e) => setNewMedicine((s) => ({ ...s, expiryDate: e.target.value }))} />
            <button className="btn-primary w-full" disabled={savingMedicine || !canCreateMedicine}>{savingMedicine ? 'Saving...' : 'Add Medicine'}</button>
          </form>
        </div>
      ) : null}
    </section>
  );
}
