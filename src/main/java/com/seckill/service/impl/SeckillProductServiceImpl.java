package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.dto.SeckillProductCreateDTO;
import com.seckill.dto.SeckillProductDTO;
import com.seckill.dto.SeckillProductUpdateDTO;
import com.seckill.entity.SeckillProduct;
import com.seckill.mapper.SeckillProductMapper;
import com.seckill.service.SeckillProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillProductServiceImpl implements SeckillProductService {

    private static final String PRODUCT_KEY_PREFIX = "seckill:product:";
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private SeckillProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillProductDTO createProduct(SeckillProductCreateDTO dto) {
        log.info("创建秒杀商品: {}", dto.getName());
        
        SeckillProduct product = new SeckillProduct();
        BeanUtils.copyProperties(dto, product);
        product.setStatus(calculateStatus(dto.getStartTime(), dto.getEndTime()));
        product.setVersion(0);
        
        productMapper.insert(product);
        
        syncToRedis(product.getId());
        
        log.info("秒杀商品创建成功, id: {}", product.getId());
        return convertToDTO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillProductDTO updateProduct(SeckillProductUpdateDTO dto) {
        log.info("更新秒杀商品: id={}", dto.getId());
        
        SeckillProduct existingProduct = productMapper.selectById(dto.getId());
        if (existingProduct == null) {
            throw new RuntimeException("商品不存在: " + dto.getId());
        }
        
        SeckillProduct product = new SeckillProduct();
        BeanUtils.copyProperties(dto, product);
        product.setVersion(existingProduct.getVersion());
        
        if (dto.getStatus() == null) {
            product.setStatus(calculateStatus(dto.getStartTime(), dto.getEndTime()));
        }
        
        productMapper.updateById(product);
        
        syncToRedis(product.getId());
        
        log.info("秒杀商品更新成功, id: {}", product.getId());
        return convertToDTO(productMapper.selectById(dto.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        log.info("删除秒杀商品: id={}", id);
        
        productMapper.deleteById(id);
        removeFromRedis(id);
        
        log.info("秒杀商品删除成功, id: {}", id);
    }

    @Override
    public SeckillProductDTO getProductById(Long id) {
        SeckillProduct product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        return convertToDTO(product);
    }

    @Override
    public Page<SeckillProductDTO> getProductPage(Integer pageNum, Integer pageSize, String name, Integer status) {
        Page<SeckillProduct> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SeckillProduct> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(name)) {
            wrapper.like(SeckillProduct::getName, name);
        }
        if (status != null) {
            wrapper.eq(SeckillProduct::getStatus, status);
        }
        wrapper.orderByDesc(SeckillProduct::getCreateTime);
        
        Page<SeckillProduct> productPage = productMapper.selectPage(page, wrapper);
        
        Page<SeckillProductDTO> dtoPage = new Page<>();
        dtoPage.setCurrent(productPage.getCurrent());
        dtoPage.setSize(productPage.getSize());
        dtoPage.setTotal(productPage.getTotal());
        dtoPage.setRecords(productPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
        
        return dtoPage;
    }

    @Override
    public List<SeckillProductDTO> getActiveProducts() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SeckillProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillProduct::getStatus, 1)
               .le(SeckillProduct::getStartTime, now)
               .ge(SeckillProduct::getEndTime, now)
               .gt(SeckillProduct::getStock, 0);
        
        return productMapper.selectList(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void syncToRedis(Long productId) {
        log.info("同步商品到Redis: productId={}", productId);
        
        SeckillProduct product = productMapper.selectById(productId);
        if (product == null) {
            log.warn("商品不存在，无法同步到Redis: productId={}", productId);
            return;
        }
        
        String productKey = PRODUCT_KEY_PREFIX + productId;
        String stockKey = STOCK_KEY_PREFIX + productId;
        
        redisTemplate.opsForValue().set(productKey, product, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(stockKey, product.getStock(), CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        log.info("商品同步到Redis成功: productId={}, stock={}", productId, product.getStock());
    }

    @Override
    public void syncAllToRedis() {
        log.info("同步所有商品到Redis");
        
        List<SeckillProduct> products = productMapper.selectList(null);
        for (SeckillProduct product : products) {
            syncToRedis(product.getId());
        }
        
        log.info("所有商品同步到Redis完成，共{}个商品", products.size());
    }

    @Override
    public void removeFromRedis(Long productId) {
        log.info("从Redis移除商品: productId={}", productId);
        
        String productKey = PRODUCT_KEY_PREFIX + productId;
        String stockKey = STOCK_KEY_PREFIX + productId;
        
        redisTemplate.delete(productKey);
        redisTemplate.delete(stockKey);
        
        log.info("商品从Redis移除成功: productId={}", productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatus() {
        log.info("开始更新商品状态");
        
        LocalDateTime now = LocalDateTime.now();
        
        LambdaQueryWrapper<SeckillProduct> startWrapper = new LambdaQueryWrapper<>();
        startWrapper.eq(SeckillProduct::getStatus, 0)
                    .le(SeckillProduct::getStartTime, now)
                    .gt(SeckillProduct::getEndTime, now);
        
        SeckillProduct startUpdate = new SeckillProduct();
        startUpdate.setStatus(1);
        productMapper.update(startUpdate, startWrapper);
        
        LambdaQueryWrapper<SeckillProduct> endWrapper = new LambdaQueryWrapper<>();
        endWrapper.eq(SeckillProduct::getStatus, 1)
                 .le(SeckillProduct::getEndTime, now);
        
        SeckillProduct endUpdate = new SeckillProduct();
        endUpdate.setStatus(2);
        productMapper.update(endUpdate, endWrapper);
        
        log.info("商品状态更新完成");
    }

    private Integer calculateStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return 0;
        } else if (now.isAfter(endTime)) {
            return 2;
        } else {
            return 1;
        }
    }

    private SeckillProductDTO convertToDTO(SeckillProduct product) {
        if (product == null) {
            return null;
        }
        SeckillProductDTO dto = new SeckillProductDTO();
        BeanUtils.copyProperties(product, dto);
        return dto;
    }
}
