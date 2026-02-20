import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { AlertCircle, ShieldCheck } from "lucide-react"
import { useAuthStore } from "@/lib/auth-store"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card } from "@/components/ui/card"

export default function LoginPage() {
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const navigate = useNavigate()
  const { login, isLoading } = useAuthStore()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    try {
      await login(username, password)
      navigate('/')
    } catch (err: any) {
      setError(err.response?.data?.message || "Login failed. Please try again.")
    }
  }

  return (
    <div className="min-h-screen px-4 py-12 md:px-10">
      <div className="mx-auto grid w-full max-w-5xl gap-10 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="flex flex-col justify-center space-y-6">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-lg shadow-primary/30">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <div>
              <p className="text-sm uppercase tracking-[0.4em] text-muted-foreground">Medi-D</p>
              <h1 className="text-4xl font-semibold leading-tight">
                Secure clinical operations in one calm workspace.
              </h1>
            </div>
          </div>
          <p className="max-w-xl text-base text-muted-foreground">
            Authenticate once to coordinate appointments, prescriptions, and inventory without
            exposing sensitive data on the client. All validation and access control runs on the
            backend.
          </p>
          <div className="flex flex-wrap gap-4">
            <div className="rounded-2xl border border-border bg-card/80 px-5 py-4">
              <p className="text-xs uppercase tracking-[0.3em] text-muted-foreground">Doctor</p>
              <p className="text-sm font-semibold">doctor / doctorpass</p>
            </div>
            <div className="rounded-2xl border border-border bg-card/80 px-5 py-4">
              <p className="text-xs uppercase tracking-[0.3em] text-muted-foreground">Receptionist</p>
              <p className="text-sm font-semibold">receptionist / receptpass</p>
            </div>
            <div className="rounded-2xl border border-border bg-card/80 px-5 py-4">
              <p className="text-xs uppercase tracking-[0.3em] text-muted-foreground">Pharmacist</p>
              <p className="text-sm font-semibold">pharmacist / pharmacistpass</p>
            </div>
          </div>
        </div>

        <Card className="glass p-8">
          <div className="space-y-2 text-center">
            <h2 className="text-2xl font-semibold">Sign in to Medi-D</h2>
            <p className="text-sm text-muted-foreground">Encrypted session with role-based access.</p>
          </div>

          <form onSubmit={handleSubmit} className="mt-8 space-y-5">
            {error && (
              <div className="flex items-center gap-2 rounded-xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                <AlertCircle className="h-4 w-4" />
                {error}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="username">Username</Label>
              <Input
                id="username"
                type="text"
                placeholder="Enter your username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isLoading}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isLoading}
                required
              />
            </div>

            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? "Signing in..." : "Sign In"}
            </Button>
          </form>

          <p className="mt-6 text-center text-xs text-muted-foreground">
            By continuing you agree to the secure handling of protected health data.
          </p>
        </Card>
      </div>
    </div>
  )
}
