<template>
  <div class="admin-container">
    <h1>秒杀商品管理</h1>
    
    <div class="toolbar">
      <div class="search-bar">
        <input 
          v-model="searchName" 
          placeholder="商品名称" 
          @keyup.enter="handleSearch"
        />
        <select v-model="searchStatus">
          <option :value="undefined">全部状态</option>
          <option :value="0">未开始</option>
          <option :value="1">进行中</option>
          <option :value="2">已结束</option>
          <option :value="3">已下架</option>
        </select>
        <button @click="handleSearch">搜索</button>
        <button @click="handleReset">重置</button>
      </div>
      <div class="action-bar">
        <button class="btn-primary" @click="openCreateDialog">新增商品</button>
        <button class="btn-secondary" @click="handleSyncAll">同步所有到Redis</button>
        <button class="btn-secondary" @click="handleUpdateStatus">更新状态</button>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>商品名称</th>
            <th>价格</th>
            <th>原价</th>
            <th>库存</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="9" class="loading">加载中...</td>
          </tr>
          <tr v-else-if="products.length === 0">
            <td colspan="9" class="empty">暂无数据</td>
          </tr>
          <tr v-else v-for="product in products" :key="product.id">
            <td>{{ product.id }}</td>
            <td>{{ product.name }}</td>
            <td>¥{{ product.price }}</td>
            <td>{{ product.originalPrice ? '¥' + product.originalPrice : '-' }}</td>
            <td>{{ product.stock }}</td>
            <td>{{ formatDateTime(product.startTime) }}</td>
            <td>{{ formatDateTime(product.endTime) }}</td>
            <td>
              <span :class="getStatusClass(product.status)">
                {{ getStatusText(product.status) }}
              </span>
            </td>
            <td class="actions">
              <button class="btn-small" @click="openEditDialog(product)">编辑</button>
              <button class="btn-small btn-info" @click="handleSync(product.id)">同步</button>
              <button class="btn-small btn-danger" @click="handleDelete(product.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination">
      <button :disabled="pagination.current <= 1" @click="handlePageChange(pagination.current - 1)">上一页</button>
      <span>第 {{ pagination.current }} / {{ pagination.pages }} 页，共 {{ pagination.total }} 条</span>
      <button :disabled="pagination.current >= pagination.pages" @click="handlePageChange(pagination.current + 1)">下一页</button>
    </div>

    <div v-if="showDialog" class="dialog-overlay" @click.self="closeDialog">
      <div class="dialog">
        <h2>{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>商品名称 *</label>
            <input v-model="formData.name" required placeholder="请输入商品名称" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>秒杀价格 *</label>
              <input v-model.number="formData.price" type="number" step="0.01" required placeholder="秒杀价格" />
            </div>
            <div class="form-group">
              <label>原价</label>
              <input v-model.number="formData.originalPrice" type="number" step="0.01" placeholder="原价" />
            </div>
          </div>
          <div class="form-group">
            <label>库存数量 *</label>
            <input v-model.number="formData.stock" type="number" required min="0" placeholder="库存数量" />
          </div>
          <div class="form-group">
            <label>商品描述</label>
            <textarea v-model="formData.description" placeholder="商品描述"></textarea>
          </div>
          <div class="form-group">
            <label>商品图片URL</label>
            <input v-model="formData.imageUrl" placeholder="图片URL" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>开始时间 *</label>
              <input v-model="formData.startTime" type="datetime-local" required />
            </div>
            <div class="form-group">
              <label>结束时间 *</label>
              <input v-model="formData.endTime" type="datetime-local" required />
            </div>
          </div>
          <div v-if="isEdit" class="form-group">
            <label>状态</label>
            <select v-model="formData.status">
              <option :value="0">未开始</option>
              <option :value="1">进行中</option>
              <option :value="2">已结束</option>
              <option :value="3">已下架</option>
            </select>
          </div>
          <div class="dialog-actions">
            <button type="button" class="btn-secondary" @click="closeDialog">取消</button>
            <button type="submit" class="btn-primary">{{ isEdit ? '更新' : '创建' }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="message.show" :class="['message', message.type]">
      {{ message.text }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { productAdminApi } from '@/api/product'
import type { SeckillProduct, SeckillProductCreate, SeckillProductUpdate } from '@/types/product'

const loading = ref(false)
const products = ref<SeckillProduct[]>([])
const showDialog = ref(false)
const isEdit = ref(false)

const searchName = ref('')
const searchStatus = ref<number | undefined>(undefined)

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

const formData = reactive<SeckillProductCreate & { id?: number; status?: number }>({
  name: '',
  stock: 0,
  price: 0,
  originalPrice: undefined,
  description: '',
  imageUrl: '',
  startTime: '',
  endTime: '',
  status: undefined
})

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
    year: 'numeric',
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

const fetchProducts = async () => {
  loading.value = true
  try {
    const result = await productAdminApi.getProducts({
      pageNum: pagination.current,
      pageSize: pagination.size,
      name: searchName.value || undefined,
      status: searchStatus.value
    })
    if (result.success) {
      products.value = result.data.records
      pagination.total = result.data.total
      pagination.pages = result.data.pages
      pagination.current = result.data.current
    } else {
      showMessage(result.message, 'error')
    }
  } catch (e) {
    showMessage('获取商品列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchProducts()
}

const handleReset = () => {
  searchName.value = ''
  searchStatus.value = undefined
  pagination.current = 1
  fetchProducts()
}

const handlePageChange = (page: number) => {
  pagination.current = page
  fetchProducts()
}

const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(formData, {
    name: '',
    stock: 0,
    price: 0,
    originalPrice: undefined,
    description: '',
    imageUrl: '',
    startTime: '',
    endTime: '',
    status: undefined
  })
  showDialog.value = true
}

const formatDateTimeForInput = (dateStr: string) => {
  if (!dateStr) return ''
  const normalized = dateStr.replace(' ', 'T')
  return normalized.slice(0, 16)
}

const openEditDialog = (product: SeckillProduct) => {
  isEdit.value = true
  Object.assign(formData, {
    id: product.id,
    name: product.name,
    stock: product.stock,
    price: product.price,
    originalPrice: product.originalPrice || undefined,
    description: product.description || '',
    imageUrl: product.imageUrl || '',
    startTime: formatDateTimeForInput(product.startTime),
    endTime: formatDateTimeForInput(product.endTime),
    status: product.status
  })
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const handleSubmit = async () => {
  try {
    let result
    if (isEdit.value) {
      const updateData: SeckillProductUpdate = {
        id: formData.id!,
        name: formData.name,
        stock: formData.stock,
        price: formData.price,
        originalPrice: formData.originalPrice,
        description: formData.description,
        imageUrl: formData.imageUrl,
        startTime: formData.startTime + ':00',
        endTime: formData.endTime + ':00',
        status: formData.status
      }
      result = await productAdminApi.updateProduct(updateData)
    } else {
      const createData: SeckillProductCreate = {
        name: formData.name,
        stock: formData.stock,
        price: formData.price,
        originalPrice: formData.originalPrice,
        description: formData.description,
        imageUrl: formData.imageUrl,
        startTime: formData.startTime + ':00',
        endTime: formData.endTime + ':00'
      }
      result = await productAdminApi.createProduct(createData)
    }
    
    if (result.success) {
      showMessage(isEdit.value ? '更新成功' : '创建成功')
      closeDialog()
      fetchProducts()
    } else {
      showMessage(result.message, 'error')
    }
  } catch (e) {
    showMessage('操作失败', 'error')
  }
}

const handleDelete = async (id: number) => {
  if (!confirm('确定要删除该商品吗？')) return
  
  try {
    const result = await productAdminApi.deleteProduct(id)
    if (result.success) {
      showMessage('删除成功')
      fetchProducts()
    } else {
      showMessage(result.message, 'error')
    }
  } catch (e) {
    showMessage('删除失败', 'error')
  }
}

const handleSync = async (id: number) => {
  try {
    const result = await productAdminApi.syncToRedis(id)
    if (result.success) {
      showMessage('同步成功')
    } else {
      showMessage(result.message, 'error')
    }
  } catch (e) {
    showMessage('同步失败', 'error')
  }
}

const handleSyncAll = async () => {
  try {
    const result = await productAdminApi.syncAllToRedis()
    if (result.success) {
      showMessage('全部同步成功')
    } else {
      showMessage(result.message, 'error')
    }
  } catch (e) {
    showMessage('同步失败', 'error')
  }
}

const handleUpdateStatus = async () => {
  try {
    const result = await productAdminApi.updateStatus()
    if (result.success) {
      showMessage('状态更新成功')
      fetchProducts()
    } else {
      showMessage(result.message, 'error')
    }
  } catch (e) {
    showMessage('状态更新失败', 'error')
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.admin-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.search-bar {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-bar input,
.search-bar select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.action-bar {
  display: flex;
  gap: 10px;
}

.btn-primary {
  background: #1890ff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary:hover {
  background: #40a9ff;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.table-container {
  overflow-x: auto;
  margin-bottom: 20px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.data-table th {
  background: #fafafa;
  font-weight: 600;
  color: #333;
}

.data-table tr:hover {
  background: #f5f5f5;
}

.loading,
.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}

.actions {
  display: flex;
  gap: 5px;
}

.btn-small {
  padding: 4px 8px;
  font-size: 12px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.btn-small:hover {
  background: #f0f0f0;
}

.btn-small.btn-info {
  background: #e6f7ff;
  border-color: #91d5ff;
  color: #1890ff;
}

.btn-small.btn-danger {
  background: #fff1f0;
  border-color: #ffa39e;
  color: #f5222d;
}

.status-pending {
  color: #faad14;
  background: #fffbe6;
  padding: 2px 8px;
  border-radius: 4px;
}

.status-active {
  color: #52c41a;
  background: #f6ffed;
  padding: 2px 8px;
  border-radius: 4px;
}

.status-ended {
  color: #999;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.status-offline {
  color: #f5222d;
  background: #fff1f0;
  padding: 2px 8px;
  border-radius: 4px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination span {
  color: #666;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog {
  background: white;
  border-radius: 8px;
  padding: 24px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.dialog h2 {
  margin: 0 0 20px;
  color: #333;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-weight: 500;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
}

.form-group textarea {
  min-height: 80px;
  resize: vertical;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-row .form-group {
  flex: 1;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
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

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-bar,
  .action-bar {
    flex-wrap: wrap;
  }
  
  .form-row {
    flex-direction: column;
  }
}
</style>
