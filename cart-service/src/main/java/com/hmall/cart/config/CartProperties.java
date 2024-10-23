package com.hmall.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hm.cart")
public class CartProperties {
    // 购物车上限
    private Integer maxAmount;
}
