<template>
  <div class="seckill-container">
    <h1>商品秒杀活动</h1>
    
    <div class="product-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else-if="products.length === 0" class="empty">暂无秒杀商品</div>
      <div v-else class="product-card" v-for="product in products" :key="product.id">
        <div class="product-image">
          <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" />
          <div v-else class="placeholder-image">商品图片</div>
        </div>
        <div class="product-info">
          <h2>{{ product.name }}</h2>
          <p class="description" v-if="product.description">{{ product.description }}</p>
          <div class="price-row">
            <span class="seckill-price">¥{{ product.price }}</span>
            <span class="original-price" v-if="product.originalPrice">¥{{ product.originalPrice }}</span>
          </div>
          <div class="stock-info">
            <span>库存: {{ product.stock }}件</span>
            <span :class="['status', getStatusClass(product.status)]">
              {{ getStatusText(product.status) }}
            </span>
          </div>
          <div class="time-info">
            <span>开始: {{ formatDateTime(product.startTime) }}</span>
            <span>结束: {{ formatDateTime(product.endTime) }}</span>
          </div>
          <button 
            @click="handleSeckill(product.id)" 
            :disabled="!canSeckill(product)"
            :class="['seckill-btn', { disabled: !canSeckill(product) }]"
          >
            {{ getButtonText(product) }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="message.show" :class="['message', message.type]">
      {{ message.text }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { seckillApi } from '@/api/product'
import { useUserStore } from '@/store/user'
import type { SeckillProduct } from '@/types/product'

const userStore = useUserStore()

const loading = ref(false)
const error = ref('')
const products = ref<SeckillProduct[]>([])

const message = reactive({
  show: false,
  type: 'success',
  text: ''
})

const showMessage = (text: string, type: 'success' | 'error' = 'success') => {
  message.text = text
  message.type = type
  message.show = true
  setTimeout(() => {
    message.show = false
  }, 3000)
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '未开始',
    1: '进行中',
    2: '已结束',
    3: '已下架'
  }
  return texts[status] || '未知'
}

const getStatusClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'status-pending',
    1: 'status-active',
    2: 'status-ended',
    3: 'status-offline'
  }
  return classes[status] || ''
}

const canSeckill = (product: SeckillProduct) => {
  return product.status === 1 && product.stock > 0 && userStore.isLoggedIn
}

const getButtonText = (product: SeckillProduct) => {
  if (product.status === 0) return '未开始'
  if (product.status === 2 || product.status === 3) return '已结束'
  if (product.stock <= 0) return '已售罄'
  if (!userStore.isLoggedIn) return '请先登录'
  return '立即秒杀'
}

const fetchProducts = async () => {
  loading.value = true
  error.value = ''
  try {
    const result = await seckillApi.getProducts()
    if (result.success) {
      products.value = result.data || []
    } else {
      error.value = result.message
    }
  } catch (e) {
    error.value = '获取商品失败'
  } finally {
    loading.value = false
  }
}

const handleSeckill = async (productId: number) => {
  if (!userStore.isLoggedIn) {
    showMessage('请先登录后再进行秒杀', 'error')
    return
  }
  try {
    const result = await seckillApi.executeSeckill(productId)
    if (result.success) {
      showMessage('秒杀成功！订单正在处理中...')
      fetchProducts()
    } else {
      showMessage(result.message || '秒杀失败', 'error')
    }
  } catch (e) {
    showMessage('秒杀失败', 'error')
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.seckill-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  color: #e74c3c;
  margin-bottom: 30px;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.product-card {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.product-image {
  height: 200px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: cover;
}

.placeholder-image {
  color: #999;
  font-size: 14px;
}

.product-info {
  padding: 16px;
}

.product-info h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #333;
}

.description {
  color: #666;
  font-size: 14px;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.seckill-price {
  font-size: 24px;
  color: #e74c3c;
  font-weight: bold;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.stock-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: #666;
}

.status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-pending {
  color: #faad14;
  background: #fffbe6;
}

.status-active {
  color: #52c41a;
  background: #f6ffed;
}

.status-ended,
.status-offline {
  color: #999;
  background: #f5f5f5;
}

.time-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-bottom: 16px;
}

.seckill-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.3s;
}

.seckill-btn:hover:not(.disabled) {
  background: linear-gradient(135deg, #c0392b, #a93226);
}

.seckill-btn.disabled {
  background: #ccc;
  cursor: not-allowed;
}

.loading,
.error,
.empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: #666;
}

.error {
  color: #e74c3c;
}

.message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 24px;
  border-radius: 4px;
  z-index: 1001;
  animation: fadeIn 0.3s;
}

.message.success {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #52c41a;
}

.message.error {
  background: #fff1f0;
  border: 1px solid #ffa39e;
  color: #f5222d;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
