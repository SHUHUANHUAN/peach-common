
package com.peach.common.manager;


import com.peach.common.config.RedisConfig;
import com.peach.common.config.mutil.MutilCacheConfig;
import com.peach.common.listener.CacheMessageListener;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Objects;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/15 10:44
 */
@Configuration
@AutoConfigureAfter(RedisConfig.class)
@EnableConfigurationProperties(MutilCacheConfig.class)
public class CacheAutoConfiguration<K, V> {

    @Bean
    @ConditionalOnBean({RedisConfig.class})
    public RedisCaffeineCacheManager cacheManager(MutilCacheConfig cacheConfig, RedisTemplate<K, V> redisTemplate, RedissonClient redissonClient) {
        return new RedisCaffeineCacheManager(cacheConfig, redisTemplate,redissonClient);
    }


    @Bean
    @ConditionalOnBean({RedisConfig.class})
    public RedisMessageListenerContainer cacheListenerContainer(MutilCacheConfig cacheConfig, RedisTemplate<K, V> redisTemplate, RedisCaffeineCacheManager cacheManager) {
        RedisMessageListenerContainer cacheListenerContainer = new RedisMessageListenerContainer();
        cacheListenerContainer.setConnectionFactory(Objects.requireNonNull(redisTemplate.getConnectionFactory()));
        CacheMessageListener<K, V> cacheMessageListener = new CacheMessageListener<>(redisTemplate, cacheManager);
        cacheListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(cacheConfig.getRedis().getTopic()));
        return cacheListenerContainer;
    }
}
