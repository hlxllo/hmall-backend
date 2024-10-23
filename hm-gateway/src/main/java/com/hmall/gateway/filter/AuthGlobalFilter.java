package com.hmall.gateway.filter;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.common.utils.CollUtils;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.config.JwtProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@EnableConfigurationProperties(AuthProperties.class)
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final AuthProperties authProperties;

    private final JwtTool jwtTool;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求和响应
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        // 获取请求路径
        RequestPath path = request.getPath();
        // 判断路径是否需要拦截
        if (isExcluded(path.toString())) {
            // 直接放行
            return chain.filter(exchange);
        }
        // 拦截并验证
        String token = null;
        // 获取请求头中的token
        List<String> tokens = request.getHeaders().get("authorization");
        if (!CollUtils.isEmpty(tokens)) {
            token = tokens.get(0);
        }
        // 解析token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            // 无效就拦截
            System.err.println("无效的token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            // 结束响应
            return response.setComplete();
        }
        // 有效，向下游传递用户信息
        String userInfo = userId.toString();
        ServerWebExchange webExchange = exchange
                .mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();
        // 放行
        return chain.filter(webExchange);
    }

    public boolean isExcluded(String path) {
        for (String excludePath : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(excludePath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
