export interface User {
  id: number
  username: string
  nickname: string
  email: string | null
  phone: string | null
  roleType: number
  token?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
}

export interface AuthResult {
  success: boolean
  message: string
  data: User | null
}

export const RoleType = {
  USER: 0,
  ADMIN: 1
} as const

export const RoleTypeText: Record<number, string> = {
  0: '普通用户',
  1: '管理员'
}
