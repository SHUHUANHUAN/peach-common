package com.peach.common.config.mutil;

import lombok.Data;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/15 10:44
 */
@Data
public class RedisCacheConfig {

    /**
     * 全局过期时间，默认不过期
     */
    private Duration defaultExpiration = Duration.ofHours(6);

    /**
     * 每个cacheName的过期时间，优先级比defaultExpiration高
     */
    private Map<String, Duration> expires = new HashMap<>();

    /**
     * 缓存更新时通知其他节点的topic名称
     */
    private String topic = "cache:caffeine:redis:topic";
}
