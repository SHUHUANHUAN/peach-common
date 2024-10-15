package com.peach.common.config.mutil;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/15 10:42
 */
@Data
//@Component
@ConfigurationProperties("peach.cache.mutil")
public class MutilCacheConfig {

    private Set<String> cacheNames = ConcurrentHashMap.newKeySet();

    /**
     * 是否存储空值，默认true，防止缓存穿透
     */
    private boolean cacheNullValues = false;

    /**
     * 是否动态根据cacheName创建Cache的实现，默认true
     */
    private boolean dynamic = true;

    /**
     * 缓存key的前缀
     */
    private String cachePrefix;

    @NestedConfigurationProperty
    private RedisCacheConfig redis = new RedisCacheConfig();

    @NestedConfigurationProperty
    private CaffeineCacheConfig caffeine = new CaffeineCacheConfig();
}
