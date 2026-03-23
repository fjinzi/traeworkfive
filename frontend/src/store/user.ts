import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { User, LoginRequest, RegisterRequest } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.roleType === 1)

  const login = async (loginData: LoginRequest) => {
    const result = await authApi.login(loginData)
    if (result.success && result.data) {
      user.value = result.data
      token.value = result.data.token || null
      if (result.data.token) {
        localStorage.setItem('token', result.data.token)
      }
    }
    return result
  }

  const register = async (registerData: RegisterRequest) => {
    const result = await authApi.register(registerData)
    if (result.success && result.data) {
      user.value = result.data
      token.value = result.data.token || null
      if (result.data.token) {
        localStorage.setItem('token', result.data.token)
      }
    }
    return result
  }

  const logout = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
  }

  const fetchUserInfo = async () => {
    if (!token.value) return null
    
    try {
      const result = await authApi.getUserInfo()
      if (result.success && result.data) {
        user.value = result.data
        return result.data
      } else {
        logout()
        return null
      }
    } catch {
      logout()
      return null
    }
  }

  const initUser = async () => {
    if (token.value && !user.value) {
      await fetchUserInfo()
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    login,
    register,
    logout,
    fetchUserInfo,
    initUser
  }
})
