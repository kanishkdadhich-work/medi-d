import { useEffect, useMemo, useState } from "react"
import { CalendarSearch } from "lucide-react"
import { apiClient } from "@/lib/api-client"
import { useAuthStore } from "@/lib/auth-store"
import { getPrimaryRole } from "@/lib/roles"
import AppShell from "@/app/components/AppShell"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export default function AppointmentPage() {
  const { user } = useAuthStore()
  const role = getPrimaryRole(user)
  const [status, setStatus] = useState("SCHEDULED")
  const [appointments, setAppointments] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadAppointments(status)
  }, [status])

  const loadAppointments = async (value: string) => {
    setLoading(true)
    try {
      const data = await apiClient.getAppointmentsByStatus(value)
      setAppointments(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error("Failed to load appointments", error)
      setAppointments([])
    } finally {
      setLoading(false)
    }
  }

  const roleFallback = useMemo(() => role || "receptionist", [role])

  return (
    <AppShell role={roleFallback} title="Appointments">
      <div className="grid gap-6 lg:grid-cols-[0.7fr_1.3fr]">
        <Card className="glass p-6">
          <div className="flex items-center gap-3">
            <CalendarSearch className="h-5 w-5 text-primary" />
            <h2 className="text-lg font-semibold">Filter schedule</h2>
          </div>
          <div className="mt-6 space-y-2">
            <Label htmlFor="status">Status</Label>
            <Input id="status" value={status} onChange={(event) => setStatus(event.target.value)} />
            <Button type="button" className="mt-4 w-full" onClick={() => loadAppointments(status)}>
              Refresh list
            </Button>
          </div>
        </Card>

        <Card className="glass p-6">
          <h2 className="text-lg font-semibold">Appointments</h2>
          <p className="text-sm text-muted-foreground">
            Showing appointments with status: <span className="font-semibold">{status}</span>
          </p>

          <div className="mt-6 space-y-3">
            {loading && <p className="text-sm text-muted-foreground">Loading appointments...</p>}
            {!loading && appointments.length === 0 && (
              <p className="text-sm text-muted-foreground">No appointments match this filter.</p>
            )}
            {!loading &&
              appointments.map((appointment) => (
                <div
                  key={appointment.appointment_id ?? appointment.id}
                  className="flex items-center justify-between rounded-2xl border border-border bg-background px-4 py-3"
                >
                  <div>
                    <p className="text-sm font-semibold">Appointment #{appointment.appointment_id ?? appointment.id}</p>
                    <p className="text-xs text-muted-foreground">
                      Doctor #{appointment.doctor_id ?? appointment.doctorId} · Patient #{appointment.patient_id ?? appointment.patientId}
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
      </div>
    </AppShell>
  )
}
