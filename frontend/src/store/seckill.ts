import { defineStore } from 'pinia'

export const useSeckillStore = defineStore('seckill', {
  state: () => ({
    products: [] as any[],
    loading: false,
    error: ''
  }),
  getters: {
    getProduct: (state) => (id: number) => {
      return state.products.find(product => product.id === id)
    }
  },
  actions: {
    async fetchProducts() {
      this.loading = true
      try {
        // 这里应该调用API获取商品列表
        // 简化处理，使用模拟数据
        this.products = [
          {
            id: 1,
            name: 'iPhone 15 Pro',
            price: 5999,
            stock: 1000
          }
        ]
      } catch (error) {
        this.error = '获取商品失败'
      } finally {
        this.loading = false
      }
    }
  }
})