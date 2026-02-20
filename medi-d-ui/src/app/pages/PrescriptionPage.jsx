import { useEffect, useMemo, useState } from "react"
import { FileCheck } from "lucide-react"
import { apiClient } from "@/lib/api-client"
import { useAuthStore } from "@/lib/auth-store"
import { getPrimaryRole } from "@/lib/roles"
import AppShell from "@/app/components/AppShell"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export default function PrescriptionPage() {
  const { user } = useAuthStore()
  const role = getPrimaryRole(user)
  const [status, setStatus] = useState("PENDING")
  const [prescriptions, setPrescriptions] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadPrescriptions(status)
  }, [status])

  const loadPrescriptions = async (value: string) => {
    setLoading(true)
    try {
      const data = await apiClient.getPrescriptionsByStatus(value)
      setPrescriptions(Array.isArray(data) ? data : [])
    } catch (error) {
      console.error("Failed to load prescriptions", error)
      setPrescriptions([])
    } finally {
      setLoading(false)
    }
  }

  const roleFallback = useMemo(() => role || "doctor", [role])

  return (
    <AppShell role={roleFallback} title="Prescriptions">
      <div className="grid gap-6 lg:grid-cols-[0.7fr_1.3fr]">
        <Card className="glass p-6">
          <div className="flex items-center gap-3">
            <FileCheck className="h-5 w-5 text-primary" />
            <h2 className="text-lg font-semibold">Filter prescriptions</h2>
          </div>
          <div className="mt-6 space-y-2">
            <Label htmlFor="status">Status</Label>
            <Input id="status" value={status} onChange={(event) => setStatus(event.target.value)} />
            <Button type="button" className="mt-4 w-full" onClick={() => loadPrescriptions(status)}>
              Refresh list
            </Button>
          </div>
        </Card>

        <Card className="glass p-6">
          <h2 className="text-lg font-semibold">Prescriptions</h2>
          <p className="text-sm text-muted-foreground">
            Showing prescriptions with status: <span className="font-semibold">{status}</span>
          </p>

          <div className="mt-6 space-y-3">
            {loading && <p className="text-sm text-muted-foreground">Loading prescriptions...</p>}
            {!loading && prescriptions.length === 0 && (
              <p className="text-sm text-muted-foreground">No prescriptions match this filter.</p>
            )}
            {!loading &&
              prescriptions.map((prescription) => (
                <div
                  key={prescription.prescription_id ?? prescription.id}
                  className="flex items-center justify-between rounded-2xl border border-border bg-background px-4 py-3"
                >
                  <div>
                    <p className="text-sm font-semibold">
                      Prescription #{prescription.prescription_id ?? prescription.id}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Appointment #{prescription.appointment_id ?? prescription.appointmentId}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-semibold">{prescription.status}</p>
                    <p className="text-xs text-muted-foreground">
                      {prescription.created_at
                        ? new Date(prescription.created_at).toLocaleString()
                        : "Created time TBD"}
                    </p>
                  </div>
                </div>
              ))}
          </div>
        </Card>
      </div>
    </AppShell>
  )
}
