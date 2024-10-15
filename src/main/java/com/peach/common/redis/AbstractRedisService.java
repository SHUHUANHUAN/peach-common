package com.peach.common.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/10 15:06
 */
public abstract class AbstractRedisService<K, V> {

    @Autowired
    protected RedisTemplate<K, V> redisTemplate;

    /**
     * 模式
     */
    @Value("${spring.redis.mode:standalone}")
    protected String mode;

    /**
     * redis key扫描行数
     */
    @Value("${spring.redis.scanLineNumber:500}")
    protected Integer scanLineNumber;

    /**
     * redis 是否启用扫描命令
     */
    @Value("${spring.redis.scanCommandIsUsed:true}")
    protected Boolean scanCommandIsUsed;

    /**
     * 重试次数
     */
    public static final Integer RETRY_TIMES = 3;

    /**
     * 批量删除key的size
     */
    public static final Integer BATCH_DELETE_SIZE = 10;

    /**
     * 是否启用redis命令 默认启用
     */
    public static final Boolean IS_USED_REDIS_COMMAND = Boolean.TRUE;

    /**
     * 获取redisTemplate序列化
     */
    protected RedisSerializer<String> getRedisSerializer() {
        return redisTemplate.getStringSerializer();
    }


}
