import axios from 'axios'
import type { SeckillProduct, SeckillProductCreate, SeckillProductUpdate, PageResult, Result } from '@/types/product'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export const productAdminApi = {
  getProducts: async (params: {
    pageNum?: number
    pageSize?: number
    name?: string
    status?: number
  }): Promise<Result<PageResult<SeckillProduct>>> => {
    const response = await api.get('/admin/seckill/product/page', { params })
    return response.data
  },

  getProductById: async (id: number): Promise<Result<SeckillProduct>> => {
    const response = await api.get(`/admin/seckill/product/${id}`)
    return response.data
  },

  createProduct: async (data: SeckillProductCreate): Promise<Result<SeckillProduct>> => {
    const response = await api.post('/admin/seckill/product', data)
    return response.data
  },

  updateProduct: async (data: SeckillProductUpdate): Promise<Result<SeckillProduct>> => {
    const response = await api.put('/admin/seckill/product', data)
    return response.data
  },

  deleteProduct: async (id: number): Promise<Result<void>> => {
    const response = await api.delete(`/admin/seckill/product/${id}`)
    return response.data
  },

  syncToRedis: async (id: number): Promise<Result<void>> => {
    const response = await api.post(`/admin/seckill/product/sync/${id}`)
    return response.data
  },

  syncAllToRedis: async (): Promise<Result<void>> => {
    const response = await api.post('/admin/seckill/product/sync-all')
    return response.data
  },

  removeFromRedis: async (id: number): Promise<Result<void>> => {
    const response = await api.delete(`/admin/seckill/product/cache/${id}`)
    return response.data
  },

  updateStatus: async (): Promise<Result<void>> => {
    const response = await api.post('/admin/seckill/product/update-status')
    return response.data
  }
}

export const seckillApi = {
  getProducts: async (): Promise<Result<SeckillProduct[]>> => {
    const response = await api.get('/seckill/products')
    return response.data
  },

  executeSeckill: async (productId: number): Promise<Result<void>> => {
    const response = await api.post(`/seckill/${productId}`)
    return response.data
  }
}
