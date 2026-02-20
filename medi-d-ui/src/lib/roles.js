export const roleLabels = {
  receptionist: "RECEPTIONIST",
  doctor: "DOCTOR",
  pharmacist: "PHARMACIST",
}

export function getPrimaryRole(user) {
  if (!user?.roles) {
    return null
  }

  const roles = user.roles
    .split(",")
    .map((role) => role.trim().replace("ROLE_", "").toUpperCase())

  if (roles.includes("DOCTOR")) return "doctor"
  if (roles.includes("RECEPTIONIST")) return "receptionist"
  if (roles.includes("PHARMACIST")) return "pharmacist"

  return null
}
