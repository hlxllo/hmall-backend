package com.hmall.api.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

// 设置openFeign的日志输出级别
public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.FULL;
    }
}
