export interface SeckillProduct {
  id: number
  name: string
  stock: number
  price: number
  originalPrice: number | null
  description: string | null
  imageUrl: string | null
  startTime: string
  endTime: string
  status: number
  version: number
  createTime: string
  updateTime: string
}

export interface SeckillProductCreate {
  name: string
  stock: number
  price: number
  originalPrice?: number
  description?: string
  imageUrl?: string
  startTime: string
  endTime: string
}

export interface SeckillProductUpdate {
  id: number
  name: string
  stock: number
  price: number
  originalPrice?: number
  description?: string
  imageUrl?: string
  startTime: string
  endTime: string
  status?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface Result<T> {
  success: boolean
  message: string
  data: T
}

export const ProductStatus = {
  NOT_STARTED: 0,
  IN_PROGRESS: 1,
  ENDED: 2,
  OFFLINE: 3
} as const

export const ProductStatusText: Record<number, string> = {
  0: '未开始',
  1: '进行中',
  2: '已结束',
  3: '已下架'
}

export const ProductStatusClass: Record<number, string> = {
  0: 'status-pending',
  1: 'status-active',
  2: 'status-ended',
  3: 'status-offline'
}
