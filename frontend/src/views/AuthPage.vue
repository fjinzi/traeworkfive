<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-tabs">
        <button 
          :class="['tab-btn', { active: activeTab === 'login' }]" 
          @click="switchTab('login')"
        >
          登录
        </button>
        <button 
          :class="['tab-btn', { active: activeTab === 'register' }]" 
          @click="switchTab('register')"
        >
          注册
        </button>
      </div>

      <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label>用户名</label>
          <input 
            v-model="loginForm.username" 
            type="text" 
            placeholder="请输入用户名" 
            required 
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码" 
            required 
          />
        </div>
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <form v-else @submit.prevent="handleRegister" class="auth-form">
        <div class="form-group">
          <label>用户名 *</label>
          <input 
            v-model="registerForm.username" 
            type="text" 
            placeholder="3-20个字符" 
            required 
            minlength="3"
            maxlength="20"
            @blur="checkUsername"
          />
          <span v-if="usernameError" class="error-text">{{ usernameError }}</span>
        </div>
        <div class="form-group">
          <label>密码 *</label>
          <input 
            v-model="registerForm.password" 
            type="password" 
            placeholder="6-20个字符" 
            required 
            minlength="6"
            maxlength="20"
          />
        </div>
        <div class="form-group">
          <label>确认密码 *</label>
          <input 
            v-model="confirmPassword" 
            type="password" 
            placeholder="请再次输入密码" 
            required 
          />
          <span v-if="passwordError" class="error-text">{{ passwordError }}</span>
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input 
            v-model="registerForm.nickname" 
            type="text" 
            placeholder="请输入昵称（可选）" 
          />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input 
            v-model="registerForm.email" 
            type="email" 
            placeholder="请输入邮箱（可选）" 
          />
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input 
            v-model="registerForm.phone" 
            type="tel" 
            placeholder="请输入手机号（可选）" 
          />
        </div>
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div v-if="message.show" :class="['message', message.type]">
        {{ message.text }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)
const confirmPassword = ref('')
const usernameError = ref('')
const passwordError = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: ''
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

const switchTab = (tab: 'login' | 'register') => {
  activeTab.value = tab
  usernameError.value = ''
  passwordError.value = ''
  message.show = false
}

const checkUsername = async () => {
  if (registerForm.username.length >= 3) {
    try {
      const result = await authApi.checkUsername(registerForm.username)
      if (result.data) {
        usernameError.value = '用户名已存在'
      } else {
        usernameError.value = ''
      }
    } catch (e) {
      console.error('检查用户名失败', e)
    }
  }
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    showMessage('请填写用户名和密码', 'error')
    return
  }

  loading.value = true
  try {
    const result = await userStore.login(loginForm)
    if (result.success) {
      showMessage('登录成功')
      setTimeout(() => {
        router.push('/')
      }, 1000)
    } else {
      showMessage(result.message, 'error')
    }
  } catch (e) {
    showMessage('登录失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (usernameError.value) {
    showMessage(usernameError.value, 'error')
    return
  }

  if (registerForm.password !== confirmPassword.value) {
    passwordError.value = '两次密码输入不一致'
    showMessage('两次密码输入不一致', 'error')
    return
  }
  passwordError.value = ''

  if (registerForm.password.length < 6) {
    passwordError.value = '密码长度至少6位'
    showMessage('密码长度至少6位', 'error')
    return
  }

  loading.value = true
  try {
    const result = await userStore.register(registerForm)
    if (result.success) {
      showMessage('注册成功')
      setTimeout(() => {
        router.push('/')
      }, 1000)
    } else {
      showMessage(result.message, 'error')
    }
  } catch (e) {
    showMessage('注册失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

import { authApi } from '@/api/auth'
</script>

<style scoped>
.auth-container {
  min-height: calc(100vh - 60px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.auth-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  padding: 30px;
}

.auth-tabs {
  display: flex;
  margin-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.tab-btn {
  flex: 1;
  padding: 12px;
  border: none;
  background: none;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.tab-btn:hover {
  color: #1890ff;
}

.tab-btn.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #1890ff;
}

.error-text {
  color: #f5222d;
  font-size: 12px;
}

.submit-btn {
  padding: 12px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s;
  margin-top: 8px;
}

.submit-btn:hover:not(:disabled) {
  background: #40a9ff;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.message {
  margin-top: 16px;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
  font-size: 14px;
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
</style>
