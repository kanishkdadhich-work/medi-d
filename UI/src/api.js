function parseBodyText(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function toApiError(status, payload) {
  if (typeof payload === 'string') {
    return {
      status,
      message: payload,
      error_code: status >= 500 ? 'SYSTEM_ERROR' : 'HTTP_ERROR'
    };
  }

  return {
    status,
    message: payload?.message || payload?.error || `Request failed (${status})`,
    error_code: payload?.error_code || payload?.code || 'HTTP_ERROR',
    details: payload?.details || null,
    raw: payload || null
  };
}

function toLocalDateTime(slotValue) {
  if (!slotValue) return slotValue;
  if (slotValue.length === 16) return `${slotValue}:00`;
  return slotValue;
}

export function createApi(getToken, onUnauthorized, onTokenRefresh) {
  async function request(method, path, { body, query, _retried } = {}) {
    const url = new URL(path, window.location.origin);
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          url.searchParams.set(key, value);
        }
      });
    }

    const headers = {};
    if (body) headers['Content-Type'] = 'application/json';

    let response;
    try {
      response = await fetch(`${url.pathname}${url.search}`, {
        method,
        credentials: 'include',
        headers,
        body: body ? JSON.stringify(body) : undefined
      });
    } catch (networkError) {
      const error = new Error('Server is unreachable. Please check backend connectivity.');
      error.api = {
        status: 0,
        message: 'Server is unreachable. Please check backend connectivity.',
        error_code: 'SERVER_UNREACHABLE'
      };
      throw error;
    }

    const raw = await response.text();
    const payload = parseBodyText(raw);

    if (!response.ok) {
      const apiError = toApiError(response.status, payload);
      const isAuthRoute = path.startsWith('/api/auth/');
      if (response.status === 401 && !_retried && !isAuthRoute) {
        try {
          const refreshed = await request('POST', '/api/auth/refresh', { _retried: true });
          const nextToken = refreshed?.token;
          if (nextToken && typeof onTokenRefresh === 'function') {
            onTokenRefresh(nextToken);
          }
          return request(method, path, { body, query, _retried: true });
        } catch {
          // fallthrough to unauthorized handling
        }
      }
      // Centralized unauthorized handling keeps session expiry behavior consistent.
      if (response.status === 401 && typeof onUnauthorized === 'function') {
        onUnauthorized();
      }
      const error = new Error(apiError.message);
      error.api = apiError;
      throw error;
    }

    return payload;
  }

  async function createAppointment(form) {
    // Canonical booking flow:
    // 1) register/reuse patient
    // 2) book appointment with doctor reference
    let patient;

    if (form.patientId) {
      patient = { patientId: Number(form.patientId) };
    } else {
      try {
        patient = await request('POST', '/api/patients/register', {
          body: {
            fullName: form.fullName,
            phoneNumber: form.phoneNumber,
            gender: form.gender || 'NA',
            medicalHistoryBlob: form.medicalHistoryBlob || ''
          }
        });
      } catch (registerError) {
        // If patient already exists (unique phone), reuse existing patient record.
        const status = registerError?.api?.status;
        const message = String(registerError?.api?.message || registerError?.message || '').toLowerCase();
        const looksLikeDuplicatePatient =
          status === 409
          || (status >= 400 && /already|exists|duplicate|unique|constraint|phone/i.test(message));

        if (!looksLikeDuplicatePatient || !form.phoneNumber) {
          throw registerError;
        }
        try {
          patient = await request('GET', '/api/patients/by-phone', {
            query: { phoneNumber: form.phoneNumber }
          });
        } catch {
          throw registerError;
        }
      }
    }

    const doctorRef = form.doctorRef || form.doctorId;

    return request('POST', '/api/appointments/book-flex', {
      query: {
        patientId: patient?.patientId,
        doctor: doctorRef,
        slot: toLocalDateTime(form.slot)
      }
    });
  }

  return {
    login: (username, password) => request('POST', '/api/auth/login', { body: { username, password } }),
    logout: () => request('POST', '/api/auth/logout'),

    createAppointment,
    registerPatient: (payload) => request('POST', '/api/patients/register', { body: payload }),
    getAllAppointments: () => request('GET', '/api/appointments'),
    getDoctorToday: (doctorId) => request('GET', '/api/appointments/doctor/today', { query: { doctorId } }),
    findPatientByPhone: (phoneNumber) => request('GET', '/api/patients/by-phone', { query: { phoneNumber } }),
    getAllPatients: () => request('GET', '/api/patients'),
    getPatientById: (id) => request('GET', `/api/patients/${id}`),
    searchPatients: (q) => request('GET', '/api/patients/search', { query: { q } }),
    getDoctors: () => request('GET', '/api/appointments/doctors'),
    bookAppointmentByDoctorRef: (payload) => request('POST', '/api/appointments/book-flex', {
      query: {
        patientId: payload.patientId,
        doctor: payload.doctor,
        slot: payload.slot
      }
    }),
    updateAppointmentStatus: (id, status) => request('PATCH', `/api/appointments/${id}/status`, { query: { status } }),
    getMyDoctorQueue: () => request('GET', '/api/appointments/doctor/my'),
    getDoctorQueue: (doctorId) => request('GET', '/api/appointments/doctor/today', { query: { doctorId } }),
    markDoctorUnavailable: (slot) => request('POST', '/api/appointments/doctor/unavailable', { query: { slot: toLocalDateTime(slot) } }),
    clearDoctorUnavailable: (slot) => request('DELETE', '/api/appointments/doctor/unavailable', { query: { slot: toLocalDateTime(slot) } }),

    searchMedicines: (name) => request('GET', '/api/medicines/search', { query: { name } }),
    getAllMedicines: () => request('GET', '/api/medicines'),
    createMedicine: (payload) => request('POST', '/api/medicines', { body: payload }),
    updateMedicineStock: (id, stockCount) => request('PUT', `/api/medicines/${id}/stock`, { query: { stockCount } }),
    deleteExpiredMedicine: (id) => request('DELETE', `/api/medicines/expired/${id}`),
    purgeExpiredMedicines: () => request('DELETE', '/api/medicines/expired'),
    createPrescription: (payload) => request('POST', '/api/prescriptions/create', { body: payload }),
    getLatestConsultation: (patientId) => request('GET', '/api/prescriptions/latest', { query: { patientId } }),
    updatePatientMedicalBlob: (id, medicalHistoryBlob) => request('PATCH', `/api/patients/${id}/medical-blob`, { body: { medicalHistoryBlob } }),

    getPendingPrescriptions: () => request('GET', '/api/prescriptions/pending'),
    dispensePrescription: (id) => request('POST', `/api/pharmacy/dispense/${id}`),
    getInventoryAlerts: () => request('GET', '/api/pharmacy/inventory/alerts'),

    getStaffUsers: () => request('GET', '/api/admin/users'),
    getAdminOverview: () => request('GET', '/api/admin/overview'),
    createUser: (payload) => request('POST', '/api/admin/users/create', { body: payload }),
    updateUser: (id, payload) => request('PUT', `/api/admin/users/${id}`, { body: payload }),
    deleteUser: (id) => request('DELETE', `/api/admin/users/${id}`),
    setUserStatus: (id, enabled) => request('PATCH', `/api/admin/users/${id}/status`, { query: { enabled } })
  };
}

export function getApiErrorMessage(error) {
  if (!error) return 'Unknown error';
  const code = error.api?.error_code;
  const message = error.api?.message || error.message || 'Request failed';
  if (!code || code === 'HTTP_ERROR' || code === 'SYSTEM_ERROR') {
    return message;
  }
  return message;
}
