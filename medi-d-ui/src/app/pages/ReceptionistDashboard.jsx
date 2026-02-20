import { useEffect, useMemo, useState } from "react"
import { CalendarDays, PlusCircle, Users } from "lucide-react"
import { apiClient } from "@/lib/api-client"
import { useAuthStore } from "@/lib/auth-store"
import { getPrimaryRole } from "@/lib/roles"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import AppShell from "@/app/components/AppShell"

export default function ReceptionistDashboard() {
  const { user } = useAuthStore()
  const role = getPrimaryRole(user)
  const [appointments, setAppointments] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [formData, setFormData] = useState({
    name: "",
    contact: "",
    doctorId: "",
    slotTimestamp: "",
    status: "SCHEDULED",
  })

  useEffect(() => {
    loadAppointments()
  }, [])

  const roleReady = useMemo(() => role === "receptionist", [role])

  const loadAppointments = async () => {
    setLoading(true)
    try {
      let data = await apiClient.getAppointmentsByStatus("SCHEDULED")
      if (Array.isArray(data) && data.length === 0) {
        data = await apiClient.getAppointmentsByStatus("scheduled")
      }
      setAppointments(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error("Failed to load appointments", err)
    } finally {
      setLoading(false)
    }
  }

  const normalizeSlotTimestamp = (value: string) => {
    if (!value) return value
    return value.length === 16 ? `${value}:00` : value
  }

  const handleCreateAppointment = async (event) => {
    event.preventDefault()
    setError("")
    setSuccess("")
    setIsSubmitting(true)

    try {
      const patient = await apiClient.createPatient({
        name: formData.name,
        contact: formData.contact,
      })

      const patientId = patient.patient_id ?? patient.id

      await apiClient.createAppointment({
        doctorId: Number(formData.doctorId),
        patientId,
        slotTimestamp: normalizeSlotTimestamp(formData.slotTimestamp),
        status: formData.status,
      })

      setSuccess("Appointment booked and patient registered.")
      setFormData({
        name: "",
        contact: "",
        doctorId: "",
        slotTimestamp: "",
        status: "SCHEDULED",
      })
      loadAppointments()
    } catch (err: any) {
      setError(err.response?.data?.message || "Unable to book appointment.")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AppShell role={roleReady ? role : "receptionist"} title="Scheduling Console">
      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="space-y-6">
          <Card className="glass p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs uppercase tracking-[0.3em] text-muted-foreground">Today</p>
                <h2 className="text-2xl font-semibold">Upcoming appointments</h2>
              </div>
              <div className="flex items-center gap-2 rounded-full border border-border bg-background px-4 py-2 text-sm text-muted-foreground">
                <CalendarDays className="h-4 w-4" />
                {new Date().toLocaleDateString()}
              </div>
            </div>

            <div className="mt-6 space-y-3">
              {loading && <p className="text-sm text-muted-foreground">Loading schedule...</p>}
              {!loading && appointments.length === 0 && (
                <p className="text-sm text-muted-foreground">No scheduled appointments yet.</p>
              )}
              {!loading &&
                appointments.map((appointment) => (
                  <div
                    key={appointment.appointment_id ?? appointment.id}
                    className="flex items-center justify-between rounded-2xl border border-border bg-background px-4 py-3"
                  >
                    <div>
                      <p className="text-sm font-semibold">Patient #{appointment.patient_id ?? appointment.patientId}</p>
                      <p className="text-xs text-muted-foreground">
                        Doctor #{appointment.doctor_id ?? appointment.doctorId}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-semibold">
                        {appointment.slot_timestamp
                          ? new Date(appointment.slot_timestamp).toLocaleString()
                          : "Time TBD"}
                      </p>
                      <p className="text-xs text-muted-foreground">{appointment.status}</p>
                    </div>
                  </div>
                ))}
            </div>
          </Card>

          <Card className="glass p-6">
            <div className="flex items-center gap-3">
              <Users className="h-5 w-5 text-primary" />
              <h3 className="text-lg font-semibold">Patient registry quick-view</h3>
            </div>
            <p className="mt-2 text-sm text-muted-foreground">
              Use the booking panel to create a patient profile and appointment in one step.
            </p>
          </Card>
        </div>

        <Card className="glass p-6">
          <div className="flex items-center gap-3">
            <PlusCircle className="h-5 w-5 text-primary" />
            <h3 className="text-lg font-semibold">Book new appointment</h3>
          </div>

          <form className="mt-6 space-y-4" onSubmit={handleCreateAppointment}>
            {error && <p className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p>}
            {success && <p className="rounded-lg bg-emerald-500/10 px-3 py-2 text-sm text-emerald-700">{success}</p>}

            <div className="space-y-2">
              <Label htmlFor="patient-name">Patient name</Label>
              <Input
                id="patient-name"
                value={formData.name}
                onChange={(event) => setFormData({ ...formData, name: event.target.value })}
                placeholder="Sarah Ahmed"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="patient-contact">Contact</Label>
              <Input
                id="patient-contact"
                value={formData.contact}
                onChange={(event) => setFormData({ ...formData, contact: event.target.value })}
                placeholder="+1-555-0101"
                required
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="doctor-id">Doctor ID</Label>
              <Input
                id="doctor-id"
                type="number"
                value={formData.doctorId}
                  onChange={(event) => setFormData({ ...formData, doctorId: event.target.value })}
                  placeholder="12"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="slot-time">Slot time</Label>
                <Input
                  id="slot-time"
                  type="datetime-local"
                  value={formData.slotTimestamp}
                  onChange={(event) => setFormData({ ...formData, slotTimestamp: event.target.value })}
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="status">Status</Label>
              <Input
                id="status"
                value={formData.status}
                onChange={(event) => setFormData({ ...formData, status: event.target.value })}
                placeholder="SCHEDULED"
                required
              />
            </div>

            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Booking..." : "Book appointment"}
            </Button>
          </form>
        </Card>
      </div>
    </AppShell>
  )
}
