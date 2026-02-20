import { useEffect, useMemo, useState } from "react"
import { PackageOpen, ShieldAlert } from "lucide-react"
import { apiClient } from "@/lib/api-client"
import { useAuthStore } from "@/lib/auth-store"
import { getPrimaryRole } from "@/lib/roles"
import AppShell from "@/app/components/AppShell"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"

export default function PharmacistDashboard() {
  const { user } = useAuthStore()
  const role = getPrimaryRole(user)
  const [prescriptions, setPrescriptions] = useState<any[]>([])
  const [selectedPrescription, setSelectedPrescription] = useState<any | null>(null)
  const [items, setItems] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [dispensing, setDispensing] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  useEffect(() => {
    loadPrescriptions()
  }, [])

  useEffect(() => {
    if (selectedPrescription?.items) {
      setItems(Array.isArray(selectedPrescription.items) ? selectedPrescription.items : [])
    } else {
      setItems([])
    }
  }, [selectedPrescription])

  const roleReady = useMemo(() => role === "pharmacist", [role])

  const loadPrescriptions = async () => {
    setLoading(true)
    try {
      const data = await apiClient.getPharmacyPendingPrescriptions()
      const list = Array.isArray(data) ? data : []
      setPrescriptions(list)
      setSelectedPrescription(list[0] || null)
    } catch (err) {
      console.error("Failed to load prescriptions", err)
    } finally {
      setLoading(false)
    }
  }

  const handleDispense = async () => {
    if (!selectedPrescription) return
    setError("")
    setSuccess("")
    setDispensing(true)
    try {
      await apiClient.dispensePrescription(selectedPrescription.prescription_id ?? selectedPrescription.id)
      setSuccess("Prescription dispensed successfully.")
      loadPrescriptions()
    } catch (err: any) {
      setError(err.response?.data?.message || "Unable to dispense prescription.")
    } finally {
      setDispensing(false)
    }
  }

  return (
    <AppShell role={roleReady ? role : "pharmacist"} title="Dispense Queue">
      <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <Card className="glass p-6">
          <div className="flex items-center gap-3">
            <PackageOpen className="h-5 w-5 text-primary" />
            <div>
              <h2 className="text-lg font-semibold">Pending prescriptions</h2>
              <p className="text-sm text-muted-foreground">Select a prescription to review items.</p>
            </div>
          </div>

          <div className="mt-6 space-y-3">
            {loading && <p className="text-sm text-muted-foreground">Loading queue...</p>}
            {!loading && prescriptions.length === 0 && (
              <p className="text-sm text-muted-foreground">No pending prescriptions.</p>
            )}
            {!loading &&
              prescriptions.map((prescription) => {
                const id = prescription.prescription_id ?? prescription.id
                const isActive =
                  selectedPrescription?.prescription_id === id || selectedPrescription?.id === id
                return (
                  <button
                    key={id}
                    type="button"
                    onClick={() => setSelectedPrescription(prescription)}
                    className={`w-full rounded-2xl border px-4 py-3 text-left transition-all ${
                      isActive
                        ? "border-primary bg-primary/10 text-foreground"
                        : "border-border bg-background hover:border-primary/50"
                    }`}
                  >
                    <p className="text-sm font-semibold">Prescription #{id}</p>
                    <p className="text-xs text-muted-foreground">Medication items ready for review</p>
                  </button>
                )
              })}
          </div>
        </Card>

        <Card className="glass p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold">Dispense details</h2>
              <p className="text-sm text-muted-foreground">Only medication data is displayed.</p>
            </div>
            <div className="flex items-center gap-2 rounded-full border border-border bg-background px-3 py-1 text-xs text-muted-foreground">
              <ShieldAlert className="h-4 w-4" />
              Privacy protected
            </div>
          </div>

          {error && <p className="mt-4 rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p>}
          {success && <p className="mt-4 rounded-lg bg-emerald-500/10 px-3 py-2 text-sm text-emerald-700">{success}</p>}

          <div className="mt-6 space-y-3">
            {items.length === 0 && (
              <p className="text-sm text-muted-foreground">No prescription items available.</p>
            )}
            {items.map((item) => (
              <div
                key={item.item_id ?? item.id ?? item.medicine_id ?? item.medicineId}
                className="flex items-center justify-between rounded-2xl border border-border bg-background px-4 py-3"
              >
                <div>
                  <p className="text-sm font-semibold">{item.medicine_name ?? item.medicineName}</p>
                  <p className="text-xs text-muted-foreground">Medicine #{item.medicine_id ?? item.medicineId}</p>
                </div>
                <span className="text-sm font-semibold">Qty {item.quantity_required ?? item.quantityRequired}</span>
              </div>
            ))}
          </div>

          <Button
            type="button"
            className="mt-6 w-full"
            disabled={!selectedPrescription || dispensing}
            onClick={handleDispense}
          >
            {dispensing ? "Dispensing..." : "Confirm dispense"}
          </Button>
        </Card>
      </div>
    </AppShell>
  )
}
