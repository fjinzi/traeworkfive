package com.seckill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.dto.Result;
import com.seckill.dto.SeckillProductCreateDTO;
import com.seckill.dto.SeckillProductDTO;
import com.seckill.dto.SeckillProductUpdateDTO;
import com.seckill.service.SeckillProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/seckill/product")
public class SeckillProductAdminController {

    @Autowired
    private SeckillProductService productService;

    @PostMapping
    public ResponseEntity<Result<SeckillProductDTO>> createProduct(@RequestBody SeckillProductCreateDTO dto) {
        log.info("创建秒杀商品请求: {}", dto.getName());
        try {
            SeckillProductDTO result = productService.createProduct(dto);
            return ResponseEntity.ok(Result.success("商品创建成功", result));
        } catch (Exception e) {
            log.error("创建商品失败", e);
            return ResponseEntity.ok(Result.fail("创建商品失败: " + e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<Result<SeckillProductDTO>> updateProduct(@RequestBody SeckillProductUpdateDTO dto) {
        log.info("更新秒杀商品请求: id={}", dto.getId());
        try {
            SeckillProductDTO result = productService.updateProduct(dto);
            return ResponseEntity.ok(Result.success("商品更新成功", result));
        } catch (Exception e) {
            log.error("更新商品失败", e);
            return ResponseEntity.ok(Result.fail("更新商品失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteProduct(@PathVariable Long id) {
        log.info("删除秒杀商品请求: id={}", id);
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(Result.success("商品删除成功", null));
        } catch (Exception e) {
            log.error("删除商品失败", e);
            return ResponseEntity.ok(Result.fail("删除商品失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<SeckillProductDTO>> getProductById(@PathVariable Long id) {
        log.info("查询秒杀商品请求: id={}", id);
        try {
            SeckillProductDTO result = productService.getProductById(id);
            if (result == null) {
                return ResponseEntity.ok(Result.fail("商品不存在"));
            }
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.error("查询商品失败", e);
            return ResponseEntity.ok(Result.fail("查询商品失败: " + e.getMessage()));
        }
    }

    @GetMapping("/page")
    public ResponseEntity<Result<Map<String, Object>>> getProductPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询秒杀商品: pageNum={}, pageSize={}, name={}, status={}", pageNum, pageSize, name, status);
        try {
            Page<SeckillProductDTO> page = productService.getProductPage(pageNum, pageSize, name, status);
            Map<String, Object> result = new HashMap<>();
            result.put("records", page.getRecords());
            result.put("total", page.getTotal());
            result.put("current", page.getCurrent());
            result.put("size", page.getSize());
            result.put("pages", page.getPages());
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.error("分页查询商品失败", e);
            return ResponseEntity.ok(Result.fail("查询失败: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<Result<List<SeckillProductDTO>>> getActiveProducts() {
        log.info("查询活动中的秒杀商品");
        try {
            List<SeckillProductDTO> result = productService.getActiveProducts();
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.error("查询活动商品失败", e);
            return ResponseEntity.ok(Result.fail("查询失败: " + e.getMessage()));
        }
    }

    @PostMapping("/sync/{id}")
    public ResponseEntity<Result<Void>> syncToRedis(@PathVariable Long id) {
        log.info("同步商品到Redis: id={}", id);
        try {
            productService.syncToRedis(id);
            return ResponseEntity.ok(Result.success("同步成功", null));
        } catch (Exception e) {
            log.error("同步商品失败", e);
            return ResponseEntity.ok(Result.fail("同步失败: " + e.getMessage()));
        }
    }

    @PostMapping("/sync-all")
    public ResponseEntity<Result<Void>> syncAllToRedis() {
        log.info("同步所有商品到Redis");
        try {
            productService.syncAllToRedis();
            return ResponseEntity.ok(Result.success("同步成功", null));
        } catch (Exception e) {
            log.error("同步所有商品失败", e);
            return ResponseEntity.ok(Result.fail("同步失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/cache/{id}")
    public ResponseEntity<Result<Void>> removeFromRedis(@PathVariable Long id) {
        log.info("从Redis移除商品: id={}", id);
        try {
            productService.removeFromRedis(id);
            return ResponseEntity.ok(Result.success("移除成功", null));
        } catch (Exception e) {
            log.error("移除商品缓存失败", e);
            return ResponseEntity.ok(Result.fail("移除失败: " + e.getMessage()));
        }
    }

    @PostMapping("/update-status")
    public ResponseEntity<Result<Void>> updateProductStatus() {
        log.info("手动更新商品状态");
        try {
            productService.updateProductStatus();
            return ResponseEntity.ok(Result.success("状态更新成功", null));
        } catch (Exception e) {
            log.error("更新商品状态失败", e);
            return ResponseEntity.ok(Result.fail("更新失败: " + e.getMessage()));
        }
    }
}
