package com.seckill.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillProductCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private Integer stock;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String description;
    private String imageUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
