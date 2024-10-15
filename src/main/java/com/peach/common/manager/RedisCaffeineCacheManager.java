
package com.peach.common.manager;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.peach.common.config.mutil.MutilCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/15 10:44
 */
@Slf4j
public class RedisCaffeineCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    private final MutilCacheConfig cacheConfig;

    private final RedisTemplate redisTemplate;

    private final boolean dynamic;

    private final Set<String> cacheNames;

    private final RedissonClient redissonClient;

    public RedisCaffeineCacheManager(MutilCacheConfig cacheConfig, RedisTemplate redisTemplate, RedissonClient redissonClient) {
        super();
        this.cacheConfig = cacheConfig;
        this.redisTemplate = redisTemplate;
        this.dynamic = cacheConfig.isDynamic();
        this.cacheNames = cacheConfig.getCacheNames();
        this.redissonClient = redissonClient;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        cache = new RedisCaffeineCache(name, redisTemplate, caffeineCache(), cacheConfig,redissonClient);
        Cache oldCache = cacheMap.putIfAbsent(name, cache);
        // 添加缓存名称到 cacheNames 集合
        if (oldCache == null) {
            cacheNames.add(name); // 动态添加缓存名称
            log.error("create cache instance, the cache name is : [{}]", name);
            return cache;
        }
        return oldCache;
    }

    public com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache() {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();

        if (cacheConfig.getCaffeine().getExpireAfterAccess() > 0) {
            cacheBuilder.expireAfterAccess(cacheConfig.getCaffeine().getExpireAfterAccess(), TimeUnit.MILLISECONDS);
        }
        if (cacheConfig.getCaffeine().getExpireAfterWrite() > 0) {
            cacheBuilder.expireAfterWrite(cacheConfig.getCaffeine().getExpireAfterWrite(), TimeUnit.MILLISECONDS);
        }
        if (cacheConfig.getCaffeine().getInitialCapacity() > 0) {
            cacheBuilder.initialCapacity(cacheConfig.getCaffeine().getInitialCapacity());
        }
        if (cacheConfig.getCaffeine().getMaximumSize() > 0) {
            cacheBuilder.maximumSize(cacheConfig.getCaffeine().getMaximumSize());
        }
        if (cacheConfig.getCaffeine().getKeyStrength() != null) {
            switch (cacheConfig.getCaffeine().getKeyStrength()) {
                case WEAK:
                    cacheBuilder.weakKeys();
                    break;
                case SOFT:
                    throw new UnsupportedOperationException("caffeine 不支持 key 软引用");
                default:
            }
        }
        if (cacheConfig.getCaffeine().getValueStrength() != null) {
            switch (cacheConfig.getCaffeine().getValueStrength()) {
                case WEAK:
                    cacheBuilder.weakValues();
                    break;
                case SOFT:
                    cacheBuilder.softValues();
                default:
            }
        }
        return cacheBuilder.build();
    }

    public CacheLoader<String, Object> cacheLoader() {
        return new CacheLoader<String, Object>() {
            @Override
            public Object load(String key) throws Exception {
                return null;
            }

            // 重写这个方法将oldValue值返回回去，进而刷新缓存
            @Override
            public Object reload(String key, Object oldValue) throws Exception {
                return oldValue;
            }
        };
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }

    /**
     * 清楚本地缓存
     * @param cacheName
     * @param key
     * @param sender
     */
    public void clearLocal(String cacheName, Object key, Integer sender) {
        Cache cache = cacheMap.get(cacheName);
        if (cache == null) {
            return;
        }
        RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
        if (redisCaffeineCache.getLocalCache().hashCode() != sender) {
            redisCaffeineCache.clearLocal(key);
        }

    }
}
