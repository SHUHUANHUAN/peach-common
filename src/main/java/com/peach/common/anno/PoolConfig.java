package com.peach.common.anno;

import com.peach.common.enums.RejectedPolicyEnum;
import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // 线程池参数配置
 * @CreateTime 2024/10/14 14:21
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PoolConfig {

    /**
     * 线程池核心数
     *
     * @return 默认10
     */
    @Value("${thread.pool.corePoolSize:10}")
    int corePoolSize() default 10;

    /**
     * 线程池最大线程数
     *
     * @return 默认 int 的最大值
     */
    @Value("${thread.pool.maximumPoolSize:2147483647}")
    int maximumPoolSize() default Integer.MAX_VALUE;

    /**
     * 线程空闲多久将销毁
     *
     * @return 默认60
     */
    @Value("${thread.pool.keepAliveTime:0}")
    long keepAliveTime() default 0L;

    /**
     * 时间单位
     *
     * @return 默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 线程池拒绝执行处理策略
     *
     * @return 默认立即执行
     */
    @Value(value = "${thread.pool.handler:Caller}")
    RejectedPolicyEnum hanler() default RejectedPolicyEnum.CALLER;

    /**
     * 线程队列数的最大值
     *
     * @return 默认0  不判断
     */
    @Value("${thread.pool.queueMaxSize:500}")
    int queueMaxSize() default 500;

}
