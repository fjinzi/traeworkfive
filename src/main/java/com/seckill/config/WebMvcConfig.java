package com.seckill.config;

import com.seckill.interceptor.AdminInterceptor;
import com.seckill.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/auth/**");

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/seckill/execute/**", "/api/seckill/*")
                .excludePathPatterns("/api/auth/**", "/api/seckill/products");
    }
}
