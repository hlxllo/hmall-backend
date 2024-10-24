package com.hmall.gateway.route;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.hmall.common.utils.CollUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final RouteDefinitionWriter writer;

    private final NacosConfigManager nacosConfigManager;

    // 路由配置文件的id和分组
    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";
    // 保存更新过的路由id
    HashSet<String> routeIds = new HashSet<>();

    // 实现动态路由加载
    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        // 注册监听器并首次拉取配置
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        updateConfigInfo(configInfo);
                    }
                });
        // 首次启动时，更新一次配置
        updateConfigInfo(configInfo);
    }

    // 更新路由配置
    public void updateConfigInfo(String configInfo) {
        log.debug("监听到路由配置变更：{}", configInfo);
        // 反序列化
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 清除旧路由
        for (String routeId : routeIds) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
        // 判断是否有路由要更新
        if (CollUtils.isEmpty(routeDefinitions)) {
            return;
        }
        // 更新路由配置
        for (RouteDefinition routeDefinition : routeDefinitions) {
            // 更新路由
            writer.save(Mono.just(routeDefinition)).subscribe();
            // 记录id，方便下次删除
            routeIds.add(routeDefinition.getId());
        }
    }
}
