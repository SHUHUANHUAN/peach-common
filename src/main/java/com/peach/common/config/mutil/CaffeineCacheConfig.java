package com.peach.common.config.mutil;

import com.peach.common.enums.CaffeineStrengthEnum;
import lombok.Data;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/15 10:44
 */
@Data
public class CaffeineCacheConfig {

    /**
     * 访问后过期时间
     */
    private long expireAfterAccess = 3 * 60 * 60 * 1000;

    /**
     * 写入后过期时间
     */
    private long expireAfterWrite = 3 * 60 * 60 * 1000;

    /**
     * 写入后刷新时间
     */
    private long refreshAfterWrite = 3 * 60 * 60 * 1000;

    /**
     * 初始化大小
     */
    private int initialCapacity = 500;

    /**
     * 最大缓存对象个数，超过此数量时之前放入的缓存将失效
     */
    private long maximumSize = 5000;

    /**
     * key 对象引用强度
     */
    private CaffeineStrengthEnum keyStrength;

    /**
     * value 对象引用强度
     */
    private CaffeineStrengthEnum valueStrength;
}
