package com.seckill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.dto.SeckillProductCreateDTO;
import com.seckill.dto.SeckillProductDTO;
import com.seckill.dto.SeckillProductUpdateDTO;
import com.seckill.entity.SeckillProduct;

import java.util.List;

public interface SeckillProductService {
    
    SeckillProductDTO createProduct(SeckillProductCreateDTO dto);
    
    SeckillProductDTO updateProduct(SeckillProductUpdateDTO dto);
    
    void deleteProduct(Long id);
    
    SeckillProductDTO getProductById(Long id);
    
    Page<SeckillProductDTO> getProductPage(Integer pageNum, Integer pageSize, String name, Integer status);
    
    List<SeckillProductDTO> getActiveProducts();
    
    void syncToRedis(Long productId);
    
    void syncAllToRedis();
    
    void removeFromRedis(Long productId);
    
    void updateProductStatus();
}
