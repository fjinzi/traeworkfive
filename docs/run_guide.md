# 秒杀商品管理系统运行指南

## 一、环境要求

### 1.1 软件要求
| 软件 | 版本 | 说明 |
|-----|------|-----|
| JDK | 17+ | Java运行环境 |
| Maven | 3.6+ | 项目构建工具 |
| MySQL | 8.0.32 | 数据库 |
| Redis | 6.2.12 | 缓存服务 |
| Node.js | 18+ | 前端运行环境 |

### 1.2 网络配置
| 服务 | 地址 | 端口 |
|-----|------|-----|
| MySQL | localhost | 3306 |
| Redis | 192.168.124.132 | 6379 |
| 后端服务 | localhost | 8080 |
| 前端服务 | localhost | 5173 |

---

## 二、安装部署

### 2.1 数据库初始化

1. 连接MySQL数据库:
```bash
mysql -u root -p123456
```

2. 执行初始化脚本:
```bash
source sql/init_database.sql
```

或直接在MySQL客户端中执行脚本内容。

### 2.2 Redis配置

确保Redis服务已启动并可访问:
```bash
redis-cli -h 192.168.124.132 -p 6379 ping
```

预期返回: `PONG`

### 2.3 后端服务启动

1. 进入项目根目录:
```bash
cd d:\aigithub\traework\traeworksed
```

2. 编译项目:
```bash
mvn clean package -DskipTests
```

3. 运行服务:
```bash
java -jar target/seckill-system-1.0-SNAPSHOT.jar
```

或使用Maven直接运行:
```bash
mvn spring-boot:run
```

### 2.4 前端服务启动

1. 进入前端目录:
```bash
cd frontend
```

2. 安装依赖:
```bash
npm install
```

3. 启动开发服务器:
```bash
npm run dev
```

---

## 三、功能使用

### 3.1 访问地址

| 页面 | 地址 |
|-----|------|
| 秒杀活动页 | http://localhost:5173/ |
| 商品管理页 | http://localhost:5173/admin |

### 3.2 商品管理操作

#### 新增商品
1. 访问商品管理页
2. 点击"新增商品"按钮
3. 填写商品信息（带*为必填项）
4. 点击"创建"按钮

#### 编辑商品
1. 在商品列表中找到目标商品
2. 点击"编辑"按钮
3. 修改商品信息
4. 点击"更新"按钮

#### 删除商品
1. 在商品列表中找到目标商品
2. 点击"删除"按钮
3. 确认删除操作

#### 同步到Redis
- 单个同步: 点击商品行的"同步"按钮
- 全量同步: 点击工具栏的"同步所有到Redis"按钮

### 3.3 秒杀操作

1. 访问秒杀活动页
2. 浏览进行中的秒杀商品
3. 点击"立即秒杀"按钮
4. 等待结果提示

---

## 四、API接口文档

### 4.1 商品管理接口

#### 创建商品
```
POST /api/admin/seckill/product
Content-Type: application/json

Request:
{
  "name": "商品名称",
  "stock": 100,
  "price": 99.00,
  "originalPrice": 199.00,
  "description": "商品描述",
  "imageUrl": "http://example.com/image.jpg",
  "startTime": "2024-01-01T10:00:00",
  "endTime": "2024-01-02T10:00:00"
}

Response:
{
  "success": true,
  "message": "商品创建成功",
  "data": { ... }
}
```

#### 更新商品
```
PUT /api/admin/seckill/product
Content-Type: application/json

Request:
{
  "id": 1,
  "name": "商品名称",
  "stock": 100,
  "price": 99.00,
  "startTime": "2024-01-01T10:00:00",
  "endTime": "2024-01-02T10:00:00",
  "status": 1
}
```

#### 删除商品
```
DELETE /api/admin/seckill/product/{id}
```

#### 查询商品详情
```
GET /api/admin/seckill/product/{id}
```

#### 分页查询商品
```
GET /api/admin/seckill/product/page?pageNum=1&pageSize=10&name=商品名&status=1
```

#### 同步商品到Redis
```
POST /api/admin/seckill/product/sync/{id}
```

#### 同步所有商品到Redis
```
POST /api/admin/seckill/product/sync-all
```

#### 更新商品状态
```
POST /api/admin/seckill/product/update-status
```

### 4.2 秒杀接口

#### 获取秒杀商品列表
```
GET /api/seckill/products
```

#### 执行秒杀
```
POST /api/seckill/{productId}
```

---

## 五、常见问题

### 5.1 数据库连接失败
**现象**: 启动时报错 "Communications link failure"

**解决方案**:
1. 检查MySQL服务是否启动
2. 检查数据库连接配置 (application.yml)
3. 确认用户名密码正确

### 5.2 Redis连接失败
**现象**: 启动时报错 "Unable to connect to Redis"

**解决方案**:
1. 检查Redis服务是否启动
2. 检查Redis连接配置 (application.yml)
3. 确认网络连通性: `telnet 192.168.124.132 6379`

### 5.3 前端代理失败
**现象**: 前端请求API返回404

**解决方案**:
1. 确认后端服务已启动
2. 检查vite.config.ts中的代理配置
3. 重启前端开发服务器

### 5.4 商品创建后Redis无数据
**现象**: 创建商品成功但Redis中没有缓存

**解决方案**:
1. 检查Redis连接是否正常
2. 手动调用同步接口
3. 查看后端日志排查异常

---

## 六、配置说明

### 6.1 后端配置 (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seckill
    username: root
    password: 123456
  data:
    redis:
      host: 192.168.124.132
      port: 6379

server:
  port: 8080
```

### 6.2 前端配置 (vite.config.ts)
```typescript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

---

## 七、生产环境部署建议

### 7.1 后端部署
1. 使用`mvn clean package`构建jar包
2. 配置JVM参数: `-Xms512m -Xmx1024m`
3. 使用systemd或supervisor管理进程
4. 配置Nginx反向代理

### 7.2 前端部署
1. 使用`npm run build`构建静态文件
2. 将dist目录部署到Nginx
3. 配置API代理

### 7.3 数据库优化
1. 配置主从复制
2. 开启慢查询日志
3. 定期备份数据

### 7.4 Redis优化
1. 配置持久化 (RDB/AOF)
2. 设置内存淘汰策略
3. 监控内存使用

---

## 八、监控与日志

### 8.1 后端日志
日志文件位置: `logs/seckill.log`

日志级别配置:
```yaml
logging:
  level:
    com.seckill: DEBUG
    org.springframework: INFO
```

### 8.2 关键监控指标
- 商品创建/更新/删除操作次数
- Redis缓存命中率
- 秒杀请求成功率
- 接口响应时间
