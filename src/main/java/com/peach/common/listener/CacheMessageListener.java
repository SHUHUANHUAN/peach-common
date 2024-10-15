
package com.peach.common.listener;


import com.peach.common.manager.RedisCaffeineCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/15 10:44
 */
@Slf4j
@RequiredArgsConstructor
public class CacheMessageListener<K, V> implements MessageListener {

    private final RedisTemplate<K, V> redisTemplate;

    private final RedisCaffeineCacheManager redisCaffeineCacheManager;


    @Override
    public void onMessage(Message message, byte[] bytes) {
        CacheMessage cacheMessage = (CacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
        assert cacheMessage != null;
        log.warn("receive message, clear local cache, the cacheName is " + cacheMessage.getCacheName() + ", the key is " + cacheMessage.getKey());
        redisCaffeineCacheManager.clearLocal(cacheMessage.getCacheName(), cacheMessage.getKey(), cacheMessage.getSender());
    }
}
