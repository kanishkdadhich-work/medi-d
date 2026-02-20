import axios from "axios"

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080"

class APIClient {
  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000,
      headers: {
        "Content-Type": "application/json",
      },
    })

    // Add request interceptor for JWT token
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem("token")
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    })

    // Add response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem("token")
          localStorage.removeItem("user")
          window.location.href = "/login"
        }
        return Promise.reject(error)
      }
    )
  }

  // Auth endpoints
  async login(username: string, password: string) {
    const response = await this.client.post("/api/auth/login", {
      username,
      password,
    })
    return response.data
  }

  // Appointment endpoints
  async getAppointmentsByStatus(status: string) {
    const response = await this.client.get(`/api/appointments/by-status/${status}`)
    return response.data
  }

  async getAppointmentsByDoctor(doctorId: number) {
    const response = await this.client.get(`/api/appointments/doctor/${doctorId}`)
    return response.data
  }

  async getAppointmentsByPatient(patientId: number) {
    const response = await this.client.get(`/api/appointments/patient/${patientId}`)
    return response.data
  }

  async createAppointment(data: any) {
    const response = await this.client.post("/api/appointments", data)
    return response.data
  }

  async checkAvailableSlots(doctorId: number) {
    const response = await this.client.get(`/api/appointments/check-available?doctorId=${doctorId}`)
    return response.data
  }

  // Patient endpoints
  async getPatient(id: number) {
    const response = await this.client.get(`/api/patients/${id}`)
    return response.data
  }

  async createPatient(data: any) {
    const response = await this.client.post("/api/patients", data)
    return response.data
  }

  // Prescription endpoints
  async getPrescriptionsByStatus(status: string) {
    const response = await this.client.get(`/api/prescriptions/by-status/${status}`)
    return response.data
  }

  async getPendingPrescriptions() {
    const response = await this.client.get("/api/prescriptions/pending")
    return response.data
  }

  async createPrescription(data: any) {
    const response = await this.client.post("/api/prescriptions", data)
    return response.data
  }

  async createPrescriptionItem(data: any) {
    const response = await this.client.post("/api/prescription-items", data)
    return response.data
  }

  async getPrescriptionItemsByPrescription(prescriptionId: number) {
    const response = await this.client.get(`/api/prescription-items/prescription/${prescriptionId}`)
    return response.data
  }

  // Medicine endpoints
  async getAllMedicines() {
    const response = await this.client.get("/api/medicines")
    return response.data
  }

  // Pharmacy endpoints
  async getPharmacyPendingPrescriptions() {
    const response = await this.client.get("/api/pharmacy/prescriptions/pending")
    return response.data
  }

  async dispensePrescription(prescriptionId: number) {
    const response = await this.client.post(`/api/pharmacy/prescriptions/${prescriptionId}/dispense`)
    return response.data
  }

  // Health check
  async healthCheck() {
    const response = await this.client.get("/api/health")
    return response.data
  }
}

export const apiClient = new APIClient()
