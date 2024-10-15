package com.peach.common.manager;

import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.peach.common.IRedisDao;
import com.peach.common.config.mutil.MutilCacheConfig;
import com.peach.common.listener.CacheMessage;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/15 10:44
 */
@Slf4j
public class RedisCaffeineCache extends AbstractValueAdaptingCache {


    private final String cachePrefix;

    private final Duration defaultExpiration;

    private final Map<String, Duration> expires;


    private final String topic;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 一级缓存
     */
    private Cache<Object, Object> caffeineCache;

    /**
     * 二级缓存
     */
    private RedisTemplate redisTemplate;

    private RedissonClient redissonClient;

    public RedisCaffeineCache(String cacheName, RedisTemplate redisTemplate,
                              Cache<Object, Object> caffeineCache, MutilCacheConfig cacheConfig, RedissonClient redissonClient) {
        super(cacheConfig.isCacheNullValues());
        this.cacheName = cacheName;
        this.redisTemplate = redisTemplate;
        this.caffeineCache = caffeineCache;
        this.cachePrefix = cacheConfig.getCachePrefix();
        this.defaultExpiration = cacheConfig.getRedis().getDefaultExpiration();
        this.expires = cacheConfig.getRedis().getExpires();
        this.topic = cacheConfig.getRedis().getTopic();
        this.redissonClient = redissonClient;
    }


    @Override
    public Object lookup(Object key) {
        Object cacheKey = getKey(key);
        Object value = caffeineCache.getIfPresent(key);
        if (value != null) {
            log.error("get cache from caffeine, the key is : [{}]", cacheKey);
            return value;
        }
        value = this.redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            log.error("get cache from redis and put in caffeine, the key is : [{}]", cacheKey);
            caffeineCache.put(key, value);
        }
        return value;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Object getNativeCache() {
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = lookup(key);
        if (value != null) {
            return (T) value;
        }
        // 使用 Redisson 的分布式锁
        RLock lock = redissonClient.getLock(StringUtil.getStringValue(key));
        lock.lock();
        try {
            value = lookup(key);
            if (value != null) {
                return (T) value;
            }
            value = valueLoader.call();
            Object storeValue = toStoreValue(value);
            put(key, storeValue);
            return (T) value;
        } catch (Exception e) {
            log.error("method: get has been field"+e.getMessage(),e);
            throw new ValueRetrievalException(key, valueLoader, e.getCause());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (!super.isAllowNullValues() && value == null) {
            this.evict(key);
            return;
        }
        doPut(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Object cacheKey = getKey(key);
        Object prevValue;
        // 考虑使用分布式锁，或者将redis的setIfAbsent改为原子性操作
        RLock lock = redissonClient.getLock(StringUtil.getStringValue(key));
        lock.lock();
        try {
            prevValue = redisTemplate.opsForValue().get(cacheKey);
            if (prevValue == null) {
                doPut(key, value);
            }
            return toValueWrapper(prevValue);
        }catch (Exception e){
            log.error("method: putIfAbsent has been field"+e.getMessage(),e);
            throw new RuntimeException(e);
        }finally {
            lock.unlock();
        }
    }

    private void doPut(Object key, Object value) {
        Duration expire = getExpire();
        value = toStoreValue(value);
        if (!expire.isNegative()) {
            redisTemplate.opsForValue().set(getKey(key), value, expire);
        } else {
            redisTemplate.opsForValue().set(getKey(key), value);
        }
        push(new CacheMessage(this.cacheName, key, this.caffeineCache.hashCode()));
        caffeineCache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，
        // 避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        redisTemplate.delete(getKey(key));
        push(new CacheMessage(this.cacheName, key, this.caffeineCache.hashCode()));
        caffeineCache.invalidate(key);
    }

    @Override
    public void clear() {
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，
        // 避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        IRedisDao redisDao = SpringUtil.getBean(IRedisDao.class);
        Set<Object> keys = redisDao.keys(this.cacheName.concat(":*"));
        if (!CollectionUtils.isEmpty(keys)) {
            redisDao.delete(keys);
        }
        push(new CacheMessage(this.cacheName, null, this.caffeineCache.hashCode()));
        caffeineCache.invalidateAll();
    }

    /**
     * @param message
     * @description 缓存变更时通知其他节点清理本地缓存
     */
    private void push(CacheMessage message) {
        redisTemplate.convertAndSend(topic, message);
    }


    /**
     * @param key
     * @description 清理本地缓存
     */
    public void clearLocal(Object key) {
        log.error("clear local cache, the key is : [{}]", key);
        if (key == null) {
            caffeineCache.invalidateAll();
        } else {
            caffeineCache.invalidate(key);
        }
    }

    private Object getKey(Object key) {
        return this.cacheName.concat(":").concat(
                StringUtils.isEmpty(cachePrefix) ? StringUtil.getStringValue(key) : cachePrefix.concat(":").concat(StringUtil.getStringValue(key)));
    }

    private Duration getExpire() {
        Duration cacheNameExpire = expires.get(this.cacheName);
        return cacheNameExpire == null ? defaultExpiration : cacheNameExpire;
    }

    public Cache<Object, Object> getLocalCache() {
        return caffeineCache;
    }
}
