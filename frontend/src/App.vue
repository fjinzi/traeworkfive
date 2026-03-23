<template>
  <div id="app">
    <nav class="nav-bar">
      <div class="nav-left">
        <router-link to="/" class="nav-link">秒杀活动</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/admin" class="nav-link">商品管理</router-link>
      </div>
      <div class="nav-right">
        <template v-if="userStore.isLoggedIn">
          <span class="user-info">
            {{ userStore.user?.nickname || userStore.user?.username }}
            <span class="role-badge" :class="{ admin: userStore.isAdmin }">
              {{ userStore.isAdmin ? '管理员' : '用户' }}
            </span>
          </span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-link">登录/注册</router-link>
        </template>
      </div>
    </nav>
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

onMounted(async () => {
  await userStore.initUser()
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f5f5;
  min-height: 100vh;
}

#app {
  min-height: 100vh;
}

.nav-bar {
  background: #fff;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.nav-left {
  display: flex;
  gap: 20px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.nav-link {
  display: inline-block;
  padding: 16px 20px;
  color: #333;
  text-decoration: none;
  font-weight: 500;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.nav-link:hover {
  color: #1890ff;
}

.nav-link.router-link-active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #333;
  font-size: 14px;
}

.role-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  background: #f0f0f0;
  color: #666;
}

.role-badge.admin {
  background: #e6f7ff;
  color: #1890ff;
}

.logout-btn {
  padding: 6px 16px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.logout-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}
</style>
