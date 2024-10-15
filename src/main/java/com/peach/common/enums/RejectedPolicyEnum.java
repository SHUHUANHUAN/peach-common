package com.peach.common.enums;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // 拒绝策略枚举
 * @CreateTime 2024/10/10 15:17
 */
public enum RejectedPolicyEnum {

    /**
     * 当线程池和队列满时，调用者线程会直接执行被拒绝的任务。
     */
    CALLER,

    /**
     * 当线程池和队列满时，抛出一个 RejectedExecutionException 异常
     */
    ABORT,

    /**
     * 当线程池和队列都满时，直接丢弃新提交的任务，不抛出异常
     */
    DISCARD,

    /**
     * 当线程池和队列满时，丢弃队列中最旧的任务，然后尝试提交新的任务
     */
    DISCARD_OLDEST;
}
