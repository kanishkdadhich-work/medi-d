import { useState } from 'react';
import Panel from '../components/Panel.jsx';

function pretty(data) {
  if (data == null) return '';
  return JSON.stringify(data, null, 2);
}

export default function PharmacistPanel({ api, notify }) {
  const [dispenseId, setDispenseId] = useState('');
  const [result, setResult] = useState(null);

  const run = async (fn) => {
    try {
      const data = await fn();
      setResult(data);
      notify('success', 'Request completed');
    } catch (error) {
      notify('error', error.message);
    }
  };

  return (
    <Panel title="Pharmacy Operations" subtitle="Queue, dispense workflow, stock alerts">
      <div className="grid-2">
        <div className="stack">
          <h4>Queue & Pending</h4>
          <button type="button" onClick={() => run(() => api.pharmacyQueue())}>GET /api/pharmacy/queue</button>
          <button type="button" onClick={() => run(() => api.getPendingPrescriptions())}>GET /api/prescriptions/pending</button>

          <h4>Dispense Prescription</h4>
          <div className="row">
            <input placeholder="Prescription ID" value={dispenseId} onChange={(e) => setDispenseId(e.target.value)} />
            <button type="button" onClick={() => run(() => api.dispensePrescription(dispenseId))}>POST /api/pharmacy/dispense/{'{id}'}</button>
          </div>

          <h4>Inventory Alerts</h4>
          <button type="button" onClick={() => run(() => api.inventoryAlerts())}>GET /api/pharmacy/inventory/alerts</button>
        </div>

        <div className="stack">
          <h4>Admin Daily Summary (Shared endpoint)</h4>
          <p className="small">This endpoint is admin-protected on backend, shown here for full endpoint visibility.</p>
          <button type="button" onClick={() => run(() => api.dailySummary())}>GET /api/pharmacy/reports/daily-summary</button>
        </div>
      </div>

      <pre className="result-box">{pretty(result)}</pre>
    </Panel>
  );
}
