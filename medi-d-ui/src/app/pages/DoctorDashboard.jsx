import { useEffect, useMemo, useState } from "react"
import { ClipboardCheck, Stethoscope, Syringe } from "lucide-react"
import { apiClient } from "@/lib/api-client"
import { useAuthStore } from "@/lib/auth-store"
import { getPrimaryRole } from "@/lib/roles"
import AppShell from "@/app/components/AppShell"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"

export default function DoctorDashboard() {
  const { user } = useAuthStore()
  const role = getPrimaryRole(user)
  const [appointments, setAppointments] = useState<any[]>([])
  const [selectedAppointment, setSelectedAppointment] = useState<any | null>(null)
  const [medicines, setMedicines] = useState<any[]>([])
  const [diagnosis, setDiagnosis] = useState("")
  const [status, setStatus] = useState("PENDING")
  const [draftItems, setDraftItems] = useState<{ medicineId: string; quantity: string }[]>([])
  const [medicineId, setMedicineId] = useState("")
  const [quantity, setQuantity] = useState("")
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  useEffect(() => {
    loadAppointments()
    loadMedicines()
  }, [])

  const loadAppointments = async () => {
    try {
      setLoading(true)
      let data = await apiClient.getAppointmentsByStatus("SCHEDULED")
      if (Array.isArray(data) && data.length === 0) {
        data = await apiClient.getAppointmentsByStatus("scheduled")
      }
      const appts = Array.isArray(data) ? data : []
      setAppointments(appts)
      setSelectedAppointment(appts[0] || null)
    } catch (error) {
      console.error("Failed to load appointments:", error)
    } finally {
      setLoading(false)
    }
  }

  const loadMedicines = async () => {
    try {
      const data = await apiClient.getAllMedicines()
      setMedicines(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error("Failed to load medicines:", error)
    }
  }

  const roleReady = useMemo(() => role === "doctor", [role])

  const handleAddItem = () => {
    if (!medicineId || !quantity) return
    setDraftItems([...draftItems, { medicineId, quantity }])
    setMedicineId("")
    setQuantity("")
  }

  const handleRemoveItem = (index: number) => {
    setDraftItems(draftItems.filter((_, idx) => idx !== index))
  }

  const handleCreatePrescription = async () => {
    if (!selectedAppointment) return
    setError("")
    setSuccess("")
    setSubmitting(true)
    try {
      const prescription = await apiClient.createPrescription({
        appointmentId: selectedAppointment.appointment_id ?? selectedAppointment.id,
        diagnosis,
        status,
      })

      const prescriptionId = prescription.prescription_id ?? prescription.id

      if (draftItems.length > 0) {
        await Promise.all(
          draftItems.map((item) =>
            apiClient.createPrescriptionItem({
              prescriptionId,
              medicineId: Number(item.medicineId),
              quantityRequired: Number(item.quantity),
            })
          )
        )
      }

      setSuccess("Prescription created and sent to pharmacy.")
      setDiagnosis("")
      setStatus("PENDING")
      setDraftItems([])
    } catch (err: any) {
      setError(err.response?.data?.message || "Unable to create prescription.")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AppShell role={roleReady ? role : "doctor"} title="Doctor Workspace">
      <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <Card className="glass p-6">
          <div className="flex items-center gap-3">
            <Stethoscope className="h-5 w-5 text-primary" />
            <div>
              <h2 className="text-lg font-semibold">Today’s appointments</h2>
              <p className="text-sm text-muted-foreground">Select a patient to prepare a prescription.</p>
            </div>
          </div>

          <div className="mt-6 space-y-3">
            {loading && <p className="text-sm text-muted-foreground">Loading schedule...</p>}
            {!loading && appointments.length === 0 && (
              <p className="text-sm text-muted-foreground">No appointments scheduled yet.</p>
            )}
            {!loading &&
              appointments.map((appointment) => {
                const id = appointment.appointment_id ?? appointment.id
                const isActive = selectedAppointment?.appointment_id === id || selectedAppointment?.id === id
                return (
                  <button
                    key={id}
                    type="button"
                    onClick={() => setSelectedAppointment(appointment)}
                    className={`w-full rounded-2xl border px-4 py-3 text-left transition-all ${
                      isActive
                        ? "border-primary bg-primary/10 text-foreground"
                        : "border-border bg-background hover:border-primary/50"
                    }`}
                  >
                    <p className="text-sm font-semibold">Patient #{appointment.patient_id ?? appointment.patientId}</p>
                    <p className="text-xs text-muted-foreground">
                      {appointment.slot_timestamp
                        ? new Date(appointment.slot_timestamp).toLocaleString()
                        : "Time TBD"}
                    </p>
                  </button>
                )
              })}
          </div>
        </Card>

        <div className="space-y-6">
          <Card className="glass p-6">
            <div className="flex items-center gap-3">
              <ClipboardCheck className="h-5 w-5 text-primary" />
              <h2 className="text-lg font-semibold">Prescription builder</h2>
            </div>

            {error && <p className="mt-4 rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p>}
            {success && <p className="mt-4 rounded-lg bg-emerald-500/10 px-3 py-2 text-sm text-emerald-700">{success}</p>}

            <div className="mt-6 space-y-4">
              <div className="space-y-2">
                <Label htmlFor="diagnosis">Diagnosis notes</Label>
                <Textarea
                  id="diagnosis"
                  value={diagnosis}
                  onChange={(event) => setDiagnosis(event.target.value)}
                  placeholder="Summarize assessment and treatment plan."
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="status">Status</Label>
                <Input
                  id="status"
                  value={status}
                  onChange={(event) => setStatus(event.target.value)}
                  placeholder="PENDING"
                />
              </div>
            </div>
          </Card>

          <Card className="glass p-6">
            <div className="flex items-center gap-3">
              <Syringe className="h-5 w-5 text-primary" />
              <h2 className="text-lg font-semibold">Medication items</h2>
            </div>

            <div className="mt-4 grid gap-4 sm:grid-cols-[1fr_auto]">
              <div className="space-y-2">
                <Label htmlFor="medicine-select">Medicine</Label>
                <select
                  id="medicine-select"
                  value={medicineId}
                  onChange={(event) => setMedicineId(event.target.value)}
                  className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="">Select medicine</option>
                  {medicines.map((medicine) => (
                    <option key={medicine.medicine_id ?? medicine.id} value={medicine.medicine_id ?? medicine.id}>
                      {medicine.name ?? medicine.medicine_name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="quantity">Qty</Label>
                <Input
                  id="quantity"
                  type="number"
                  value={quantity}
                  onChange={(event) => setQuantity(event.target.value)}
                  placeholder="10"
                />
              </div>
            </div>

            <Button type="button" variant="outline" className="mt-4 w-full" onClick={handleAddItem}>
              Add item
            </Button>

            {draftItems.length > 0 && (
              <div className="mt-4 space-y-2">
                {draftItems.map((item, index) => (
                  <div key={`${item.medicineId}-${index}`} className="flex items-center justify-between rounded-lg border border-border px-3 py-2 text-sm">
                    <span>
                      Medicine #{item.medicineId} · Qty {item.quantity}
                    </span>
                    <Button type="button" variant="ghost" size="sm" onClick={() => handleRemoveItem(index)}>
                      Remove
                    </Button>
                  </div>
                ))}
              </div>
            )}

          <Button
              type="button"
              className="mt-6 w-full"
              disabled={!selectedAppointment || submitting || !diagnosis.trim() || !status.trim()}
              onClick={handleCreatePrescription}
            >
              {submitting ? "Sending..." : "Send prescription to pharmacy"}
            </Button>
          </Card>
        </div>
      </div>
    </AppShell>
  )
}
