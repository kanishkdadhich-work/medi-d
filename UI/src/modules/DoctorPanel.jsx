import { useState } from 'react';
import Panel from '../components/Panel.jsx';

function pretty(data) {
  if (data == null) return '';
  return JSON.stringify(data, null, 2);
}

export default function DoctorPanel({ api, notify }) {
  const [doctorId, setDoctorId] = useState('1');
  const [consultationId, setConsultationId] = useState('');
  const [medicineName, setMedicineName] = useState('para');
  const [prescription, setPrescription] = useState({
    appointmentId: '',
    diagnosisNotes: '',
    items: [{ medicineId: '', quantity: 1, instructions: '' }]
  });
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

  const updateItem = (index, key, value) => {
    setPrescription((prev) => {
      const nextItems = [...prev.items];
      nextItems[index] = { ...nextItems[index], [key]: value };
      return { ...prev, items: nextItems };
    });
  };

  const addItem = () => {
    setPrescription((prev) => ({
      ...prev,
      items: [...prev.items, { medicineId: '', quantity: 1, instructions: '' }]
    }));
  };

  return (
    <Panel title="Doctor Console" subtitle="Consultations, medicine search, prescription authoring">
      <div className="grid-2">
        <div className="stack">
          <h4>Today's Consultations</h4>
          <div className="row">
            <input value={doctorId} onChange={(e) => setDoctorId(e.target.value)} placeholder="Doctor ID" />
            <button type="button" onClick={() => run(() => api.getDoctorToday(doctorId))}>GET /api/appointments/doctor/today</button>
          </div>

          <h4>Mark Consultation Complete</h4>
          <div className="row">
            <input value={consultationId} onChange={(e) => setConsultationId(e.target.value)} placeholder="Appointment ID" />
            <button type="button" onClick={() => run(() => api.completeConsultation(consultationId))}>POST /api/appointments/{'{id}'}/complete</button>
          </div>

          <h4>Medicine Live Search</h4>
          <div className="row">
            <input value={medicineName} onChange={(e) => setMedicineName(e.target.value)} placeholder="Medicine name" />
            <button type="button" onClick={() => run(() => api.searchMedicines(medicineName))}>GET /api/medicines/search</button>
          </div>
        </div>

        <form
          className="stack"
          onSubmit={(e) => {
            e.preventDefault();
            run(() => api.createPrescription({
              appointmentId: Number(prescription.appointmentId),
              diagnosisNotes: prescription.diagnosisNotes,
              items: prescription.items.map((i) => ({
                medicineId: Number(i.medicineId),
                quantity: Number(i.quantity),
                instructions: i.instructions
              }))
            }));
          }}
        >
          <h4>Create Prescription</h4>
          <input placeholder="Appointment ID" value={prescription.appointmentId} onChange={(e) => setPrescription((s) => ({ ...s, appointmentId: e.target.value }))} required />
          <textarea placeholder="Diagnosis notes" value={prescription.diagnosisNotes} onChange={(e) => setPrescription((s) => ({ ...s, diagnosisNotes: e.target.value }))} required />

          {prescription.items.map((item, idx) => (
            <div className="item-card" key={`med-${idx}`}>
              <input placeholder="Medicine ID" value={item.medicineId} onChange={(e) => updateItem(idx, 'medicineId', e.target.value)} required />
              <input type="number" min="1" placeholder="Qty" value={item.quantity} onChange={(e) => updateItem(idx, 'quantity', e.target.value)} required />
              <input placeholder="Instructions" value={item.instructions} onChange={(e) => updateItem(idx, 'instructions', e.target.value)} />
            </div>
          ))}
          <div className="row">
            <button type="button" className="ghost" onClick={addItem}>+ Add Medication</button>
            <button type="submit">POST /api/prescriptions/create</button>
          </div>
        </form>
      </div>

      <pre className="result-box">{pretty(result)}</pre>
    </Panel>
  );
}
