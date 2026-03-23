package com.seckill.controller;

import com.seckill.entity.SeckillProduct;
import com.seckill.service.SeckillProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SeckillProductService productService;

    @PostMapping("/execute/{productId}")
    public ResponseEntity<Map<String, Object>> seckill(@PathVariable String productId, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录后再进行秒杀");
            return ResponseEntity.ok(result);
        }

        try {
            Object productObj = redisTemplate.opsForValue().get("seckill:product:" + productId);
            if (productObj == null) {
                log.warn("商品不存在, productId: {}", productId);
                result.put("success", false);
                result.put("message", "商品不存在，请先初始化缓存");
                return ResponseEntity.ok(result);
            }

            SeckillProduct product;
            if (productObj instanceof SeckillProduct) {
                product = (SeckillProduct) productObj;
            } else if (productObj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) productObj;
                product = new SeckillProduct();
                if (map.get("id") != null) {
                    product.setId(Long.valueOf(map.get("id").toString()));
                }
                product.setName((String) map.get("name"));
                if (map.get("price") != null) {
                    product.setPrice(new BigDecimal(map.get("price").toString()));
                }
                if (map.get("stock") != null) {
                    product.setStock(Integer.valueOf(map.get("stock").toString()));
                }
            } else {
                log.error("商品数据类型异常: {}", productObj.getClass().getName());
                result.put("success", false);
                result.put("message", "商品数据格式异常");
                return ResponseEntity.ok(result);
            }

            String lockKey = "seckill:lock:" + productId;
            RLock lock = redissonClient.getLock(lockKey);

            boolean locked = false;
            try {
                locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!locked) {
                    log.warn("获取锁失败, productId: {}, userId: {}", productId, userId);
                    result.put("success", false);
                    result.put("message", "系统繁忙，请稍后重试");
                    return ResponseEntity.ok(result);
                }

                log.info("成功获取锁, productId: {}, userId: {}", productId, userId);

                String purchasedKey = "seckill:purchased:" + productId + ":" + userId;
                Boolean purchased = redisTemplate.hasKey(purchasedKey);
                if (Boolean.TRUE.equals(purchased)) {
                    log.info("用户已购买过该商品, productId: {}, userId: {}", productId, userId);
                    result.put("success", false);
                    result.put("message", "您已购买过该商品");
                    return ResponseEntity.ok(result);
                }

                String stockKey = "seckill:stock:" + productId;
                Object stockObj = redisTemplate.opsForValue().get(stockKey);
                if (stockObj == null) {
                    log.warn("库存数据不存在, productId: {}", productId);
                    result.put("success", false);
                    result.put("message", "库存数据不存在，请先初始化缓存");
                    return ResponseEntity.ok(result);
                }

                Integer stock;
                if (stockObj instanceof Integer) {
                    stock = (Integer) stockObj;
                } else if (stockObj instanceof Long) {
                    stock = ((Long) stockObj).intValue();
                } else if (stockObj instanceof String) {
                    stock = Integer.parseInt((String) stockObj);
                } else {
                    stock = Integer.valueOf(stockObj.toString());
                }

                if (stock <= 0) {
                    log.info("库存不足, productId: {}, stock: {}", productId, stock);
                    result.put("success", false);
                    result.put("message", "库存不足");
                    return ResponseEntity.ok(result);
                }

                Long newStock = redisTemplate.opsForValue().decrement(stockKey);
                if (newStock == null || newStock < 0) {
                    redisTemplate.opsForValue().increment(stockKey);
                    log.warn("库存扣减失败, productId: {}, newStock: {}", productId, newStock);
                    result.put("success", false);
                    result.put("message", "库存不足");
                    return ResponseEntity.ok(result);
                }

                redisTemplate.opsForValue().set(purchasedKey, "1");

                log.info("秒杀成功, productId: {}, userId: {}, remainingStock: {}", productId, userId, newStock);
                result.put("success", true);
                result.put("message", "抢购成功！订单正在处理中...");
                return ResponseEntity.ok(result);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("获取锁被中断, productId: {}", productId, e);
                result.put("success", false);
                result.put("message", "系统错误，请稍后重试");
                return ResponseEntity.ok(result);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("释放锁, productId: {}", productId);
                }
            }

        } catch (Exception e) {
            log.error("秒杀异常, productId: {}, userId: {}", productId, userId, e);
            result.put("success", false);
            result.put("message", "系统错误：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/init/{productId}")
    public ResponseEntity<Map<String, Object>> initCache(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1000") Integer stock,
            @RequestParam(defaultValue = "iPhone 15 Pro") String name,
            @RequestParam(defaultValue = "5999") Double price) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            SeckillProduct product = new SeckillProduct();
            product.setId(Long.valueOf(productId));
            product.setName(name);
            product.setPrice(BigDecimal.valueOf(price));
            product.setStock(stock);

            redisTemplate.opsForValue().set("seckill:product:" + productId, product);
            redisTemplate.opsForValue().set("seckill:stock:" + productId, stock);

            log.info("缓存初始化成功, productId: {}, stock: {}, name: {}", productId, stock, name);
            
            result.put("success", true);
            result.put("message", "缓存初始化成功");
            result.put("data", Map.of(
                "productKey", "seckill:product:" + productId,
                "stockKey", "seckill:stock:" + productId,
                "stock", stock
            ));
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("缓存初始化失败, productId: {}", productId, e);
            result.put("success", false);
            result.put("message", "缓存初始化失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/stock/{productId}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable String productId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Object stockObj = redisTemplate.opsForValue().get("seckill:stock:" + productId);
            Object productObj = redisTemplate.opsForValue().get("seckill:product:" + productId);
            
            if (stockObj == null || productObj == null) {
                result.put("success", false);
                result.put("message", "商品缓存不存在，请先调用初始化接口");
                return ResponseEntity.ok(result);
            }
            
            result.put("success", true);
            result.put("stock", stockObj);
            result.put("product", productObj);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("查询库存失败, productId: {}", productId, e);
            result.put("success", false);
            result.put("message", "查询失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @DeleteMapping("/cache/{productId}")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable String productId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            redisTemplate.delete("seckill:product:" + productId);
            redisTemplate.delete("seckill:stock:" + productId);
            redisTemplate.delete("seckill:purchased:" + productId + ":1");
            
            log.info("缓存清理成功, productId: {}", productId);
            result.put("success", true);
            result.put("message", "缓存清理成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("缓存清理失败, productId: {}", productId, e);
            result.put("success", false);
            result.put("message", "缓存清理失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getProducts() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> products = new java.util.ArrayList<>();
            
            List<com.seckill.dto.SeckillProductDTO> activeProducts = productService.getActiveProducts();
            
            for (com.seckill.dto.SeckillProductDTO productDTO : activeProducts) {
                Map<String, Object> productData = new HashMap<>();
                productData.put("id", productDTO.getId());
                productData.put("name", productDTO.getName());
                productData.put("price", productDTO.getPrice());
                productData.put("originalPrice", productDTO.getOriginalPrice());
                productData.put("description", productDTO.getDescription());
                productData.put("imageUrl", productDTO.getImageUrl());
                productData.put("startTime", productDTO.getStartTime());
                productData.put("endTime", productDTO.getEndTime());
                productData.put("status", productDTO.getStatus());
                
                Object stockObj = redisTemplate.opsForValue().get("seckill:stock:" + productDTO.getId());
                if (stockObj != null) {
                    productData.put("stock", Integer.valueOf(stockObj.toString()));
                } else {
                    productData.put("stock", productDTO.getStock());
                }
                
                products.add(productData);
            }
            
            if (products.isEmpty()) {
                result.put("success", false);
                result.put("message", "暂无进行中的秒杀商品");
                result.put("data", new java.util.ArrayList<>());
            } else {
                result.put("success", true);
                result.put("data", products);
            }
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            result.put("success", false);
            result.put("message", "获取商品失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> seckillProduct(@PathVariable String productId, HttpServletRequest request) {
        return seckill(productId, request);
    }
}
