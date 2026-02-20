import { create } from "zustand"
import { apiClient } from "./api-client"

export const useAuthStore = create((set) => ({
  user: null,
  token: null,
  isLoading: false,
  error: null,

  login: async (username, password) => {
    set({ isLoading: true, error: null })
    try {
      const response = await apiClient.login(username, password)
      const token = response.token
      
      // Decode JWT to get user info
      const base64Url = token.split('.')[1]
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      )
      const user = JSON.parse(jsonPayload)

      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))

      set({
        token,
        user: {
          username: user.sub,
          roles: user.roles,
        },
        isLoading: false,
      })
    } catch (error) {
      set({
        error: error.response?.data?.message || "Login failed",
        isLoading: false,
      })
      throw error
    }
  },

  logout: () => {
    localStorage.removeItem("token")
    localStorage.removeItem("user")
    set({ user: null, token: null, error: null })
  },

  loadAuth: () => {
    const token = localStorage.getItem("token")
    const user = localStorage.getItem("user")
    
    if (token && user) {
      set({
        token,
        user: JSON.parse(user),
      })
    }
  },
}))
