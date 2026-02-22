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
    message: payload?.message || payload?.error || `HTTP ${status}`,
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

export function createApi(getToken) {
  async function request(method, path, { body, query } = {}) {
    const url = new URL(path, window.location.origin);
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          url.searchParams.set(key, value);
        }
      });
    }

    const token = getToken?.();
    const headers = {};
    if (body) headers['Content-Type'] = 'application/json';
    if (token) headers.Authorization = `Bearer ${token}`;

    const response = await fetch(`${url.pathname}${url.search}`, {
      method,
      credentials: 'include',
      headers,
      body: body ? JSON.stringify(body) : undefined
    });

    const raw = await response.text();
    const payload = parseBodyText(raw);

    if (!response.ok) {
      const apiError = toApiError(response.status, payload);
      const error = new Error(apiError.message);
      error.api = apiError;
      throw error;
    }

    return payload;
  }

  async function createAppointment(form) {
    // Backend canonical flow: register/reuse patient, then /appointments/book.
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
        if (status !== 409) {
          throw registerError;
        }
        patient = await request('GET', '/api/patients/by-phone', {
          query: { phoneNumber: form.phoneNumber }
        });
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
    getAllAppointments: () => request('GET', '/api/appointments'),
    findPatientByPhone: (phoneNumber) => request('GET', '/api/patients/by-phone', { query: { phoneNumber } }),
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
  if (error.api?.error_code) {
    return `${error.api.error_code}: ${error.api.message}`;
  }
  return error.message || 'Request failed';
}
