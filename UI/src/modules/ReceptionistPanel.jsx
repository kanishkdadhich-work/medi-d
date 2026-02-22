import { useState } from 'react';
import Panel from '../components/Panel.jsx';

function pretty(data) {
  if (data == null) return '';
  return JSON.stringify(data, null, 2);
}

export default function ReceptionistPanel({ api, notify }) {
  const [patientForm, setPatientForm] = useState({
    fullName: '',
    phoneNumber: '',
    gender: '',
    medicalHistoryBlob: ''
  });
  const [findId, setFindId] = useState('');
  const [bookForm, setBookForm] = useState({ patientId: '', doctorId: '', slot: '' });
  const [cancelId, setCancelId] = useState('');
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
    <Panel title="Reception Desk" subtitle="Patients + appointments lifecycle">
      <div className="grid-2">
        <form
          className="stack"
          onSubmit={(e) => {
            e.preventDefault();
            run(() => api.registerPatient(patientForm));
          }}
        >
          <h4>Register Patient</h4>
          <input placeholder="Full name" value={patientForm.fullName} onChange={(e) => setPatientForm((s) => ({ ...s, fullName: e.target.value }))} required />
          <input placeholder="Phone (10 digits)" value={patientForm.phoneNumber} onChange={(e) => setPatientForm((s) => ({ ...s, phoneNumber: e.target.value }))} required />
          <input placeholder="Gender" value={patientForm.gender} onChange={(e) => setPatientForm((s) => ({ ...s, gender: e.target.value }))} />
          <textarea placeholder="Medical history" value={patientForm.medicalHistoryBlob} onChange={(e) => setPatientForm((s) => ({ ...s, medicalHistoryBlob: e.target.value }))} />
          <button type="submit">POST /api/patients/register</button>
        </form>

        <div className="stack">
          <h4>Find Patients</h4>
          <button type="button" onClick={() => run(() => api.getAllPatients())}>GET /api/patients</button>
          <div className="row">
            <input placeholder="Patient ID" value={findId} onChange={(e) => setFindId(e.target.value)} />
            <button type="button" onClick={() => run(() => api.getPatientById(findId))}>GET /api/patients/{'{id}'}</button>
          </div>

          <h4>Book Appointment</h4>
          <input placeholder="Patient ID" value={bookForm.patientId} onChange={(e) => setBookForm((s) => ({ ...s, patientId: e.target.value }))} required />
          <input placeholder="Doctor ID" value={bookForm.doctorId} onChange={(e) => setBookForm((s) => ({ ...s, doctorId: e.target.value }))} required />
          <input type="datetime-local" value={bookForm.slot} onChange={(e) => setBookForm((s) => ({ ...s, slot: e.target.value }))} required />
          <button type="button" onClick={() => run(() => api.bookAppointment(bookForm))}>POST /api/appointments/book</button>

          <h4>Cancel Appointment</h4>
          <div className="row">
            <input placeholder="Appointment ID" value={cancelId} onChange={(e) => setCancelId(e.target.value)} />
            <button type="button" onClick={() => run(() => api.cancelAppointment(cancelId))}>POST /api/appointments/{'{id}'}/cancel</button>
          </div>
        </div>
      </div>

      <pre className="result-box">{pretty(result)}</pre>
    </Panel>
  );
}
