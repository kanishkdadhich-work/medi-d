export const ROLE_OPTIONS = ['ADMIN', 'DOCTOR', 'PHARMACIST', 'RECEPTIONIST'];

export const ENDPOINT_CATALOG = [
  { method: 'POST', path: '/api/auth/login', role: 'Public' },
  { method: 'POST', path: '/api/auth/logout', role: 'Any Authenticated' },
  { method: 'POST', path: '/api/auth/signup', role: 'Public' },
  { method: 'POST', path: '/api/admin/users/create', role: 'ADMIN' },
  { method: 'POST', path: '/api/patients/register', role: 'RECEPTIONIST' },
  { method: 'GET', path: '/api/patients/{id}', role: 'RECEPTIONIST' },
  { method: 'GET', path: '/api/patients', role: 'RECEPTIONIST' },
  { method: 'POST', path: '/api/appointments/book', role: 'RECEPTIONIST' },
  { method: 'POST', path: '/api/appointments/{id}/cancel', role: 'Authenticated' },
  { method: 'GET', path: '/api/appointments/doctor/today?doctorId=', role: 'DOCTOR' },
  { method: 'POST', path: '/api/appointments/{id}/complete', role: 'Authenticated' },
  { method: 'GET', path: '/api/medicines/search?name=', role: 'DOCTOR/PHARMACIST' },
  { method: 'POST', path: '/api/prescriptions/create', role: 'DOCTOR' },
  { method: 'GET', path: '/api/prescriptions/pending', role: 'PHARMACIST' },
  { method: 'GET', path: '/api/pharmacy/queue', role: 'PHARMACIST' },
  { method: 'POST', path: '/api/pharmacy/dispense/{id}', role: 'PHARMACIST' },
  { method: 'GET', path: '/api/pharmacy/inventory/alerts', role: 'PHARMACIST' },
  { method: 'GET', path: '/api/pharmacy/reports/daily-summary', role: 'ADMIN' }
];
