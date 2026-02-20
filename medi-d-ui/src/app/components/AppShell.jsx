import { useMemo, useState } from "react"
import { NavLink } from "react-router-dom"
import {
  Calendar,
  ClipboardList,
  Menu,
  Pill,
  ShieldCheck,
  Stethoscope,
  User,
  X,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { useAuthStore } from "@/lib/auth-store"
import { roleLabels } from "@/lib/roles"

const roleNav = {
  receptionist: [
    { to: "/", label: "Scheduling", icon: Calendar },
    { to: "/appointments", label: "Appointments", icon: ClipboardList },
  ],
  doctor: [
    { to: "/", label: "Workspace", icon: Stethoscope },
    { to: "/prescriptions", label: "Prescriptions", icon: ClipboardList },
  ],
  pharmacist: [
    { to: "/", label: "Dispense Queue", icon: Pill },
    { to: "/prescriptions", label: "Prescriptions", icon: ClipboardList },
  ],
}

export default function AppShell({ role, title, children }) {
  const { user, logout } = useAuthStore()
  const [mobileOpen, setMobileOpen] = useState(false)
  const navItems = useMemo(() => roleNav[role], [role])

  return (
    <div className="min-h-screen">
      <div className="flex min-h-screen">
        <aside
          className={`fixed inset-y-0 left-0 z-40 w-72 transform border-r border-border bg-card/95 shadow-xl backdrop-blur-md transition-transform md:static md:translate-x-0 ${
            mobileOpen ? "translate-x-0" : "-translate-x-full"
          }`}
        >
          <div className="flex items-center justify-between px-6 py-6">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
                <ShieldCheck className="h-6 w-6" />
              </div>
              <div>
                <p className="text-lg font-semibold">Medi-D</p>
                <p className="text-xs text-muted-foreground">Care operations hub</p>
              </div>
            </div>
            <button
              type="button"
              className="rounded-full p-2 text-muted-foreground hover:bg-muted md:hidden"
              onClick={() => setMobileOpen(false)}
              aria-label="Close menu"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <nav className="px-4 pb-6">
            <p className="px-3 text-xs uppercase tracking-[0.24em] text-muted-foreground">Navigation</p>
            <div className="mt-4 space-y-2">
              {navItems.map((item) => {
                const Icon = item.icon
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      `flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all ${
                        isActive
                          ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20"
                          : "text-muted-foreground hover:bg-muted/70 hover:text-foreground"
                      }`
                    }
                  >
                    <Icon className="h-5 w-5" />
                    {item.label}
                  </NavLink>
                )
              })}
            </div>
          </nav>

          <div className="mt-auto px-6 pb-8">
            <div className="rounded-2xl border border-border bg-muted/60 p-4">
              <p className="text-xs text-muted-foreground">Logged in as</p>
              <div className="mt-2 flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                  <User className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="text-sm font-semibold">{user?.username || "User"}</p>
                  <p className="text-xs text-muted-foreground">{roleLabels[role]}</p>
                </div>
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-4 w-full"
                onClick={logout}
              >
                Sign out
              </Button>
            </div>
          </div>
        </aside>

        <div className="flex min-h-screen flex-1 flex-col">
          <header className="flex items-center justify-between gap-4 border-b border-border bg-card/80 px-6 py-4 backdrop-blur">
            <div className="flex items-center gap-3">
              <button
                type="button"
                className="rounded-xl border border-border bg-muted/60 p-2 text-muted-foreground hover:text-foreground md:hidden"
                onClick={() => setMobileOpen(true)}
                aria-label="Open menu"
              >
                <Menu className="h-5 w-5" />
              </button>
              <div>
                <p className="text-xs uppercase tracking-[0.24em] text-muted-foreground">{roleLabels[role]}</p>
                <h1 className="text-xl font-semibold">{title || "Dashboard"}</h1>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="hidden items-center gap-2 rounded-full border border-border bg-background px-4 py-2 text-sm text-muted-foreground shadow-sm md:flex">
                <span className="h-2 w-2 rounded-full bg-emerald-500"></span>
                Security checks enforced server-side
              </div>
              <Badge variant="accent">{new Date().toLocaleDateString()}</Badge>
            </div>
          </header>

          <main className="flex-1 p-6 md:p-8">
            <div className="pulse-once">{children}</div>
          </main>
        </div>
      </div>
    </div>
  )
}
