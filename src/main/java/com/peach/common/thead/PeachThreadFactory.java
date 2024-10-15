package com.peach.common.thead;

import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // 自定义线程池工厂用于初始化线程池
 * @CreateTime 2024/10/10 15:30
 */
public class PeachThreadFactory implements ThreadFactory {

    /**
     * 线程池成员
     */
    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

    /**
     * 线程成员
     */
    public final AtomicInteger threadNumber = new AtomicInteger(1);

    /**
     * 线程名称前缀
     */
    private final String namePrefix;

    /**
     * 线程分组
     */
    private final ThreadGroup group;

    public PeachThreadFactory(String poolName) {
        if (StringUtils.isEmpty(poolName)) {
            poolName = "peach";
        }
        SecurityManager manager = System.getSecurityManager();
        group = (manager != null) ? manager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = poolName + "-pool-" + POOL_NUMBER.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 0);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }

    public AtomicInteger getThreadNumber() {
        return threadNumber;
    }
}
