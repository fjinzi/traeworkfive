package com.seckill.consumer;

import com.seckill.entity.SeckillOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RocketMQMessageListener(topic = "seckill-topic", consumerGroup = "seckill-consumer-group")
public class SeckillOrderConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        try {
            log.info("接收到秒杀订单消息: {}", message);

            String[] parts = message.split(",");
            Long userId = Long.parseLong(parts[0]);
            Long productId = Long.parseLong(parts[1]);
            BigDecimal price = new BigDecimal(parts[2]);

            SeckillOrder order = new SeckillOrder();
            order.setUserId(userId);
            order.setProductId(productId);
            order.setPrice(price);
            order.setStatus(0);

            log.info("创建订单成功: userId={}, productId={}, price={}", userId, productId, price);

        } catch (Exception e) {
            log.error("处理秒杀订单失败", e);
        }
    }
}