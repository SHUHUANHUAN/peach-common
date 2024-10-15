package com.peach.common.thead;

import com.peach.common.enums.RejectedPolicyEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // 拒绝策略代理
 * @CreateTime 2024/10/10 15:22
 */
@Slf4j
public class PeachRejectHandler implements RejectedExecutionHandler {

    private final AtomicInteger handlerCount = new AtomicInteger();

    private final RejectedExecutionHandler rejectedExecutionHandler;

    public PeachRejectHandler(RejectedPolicyEnum rejectedPolicyEnum){
        RejectedExecutionHandler rejectedHandler = null;
        switch(rejectedPolicyEnum){
            case CALLER:
                rejectedHandler = new ThreadPoolExecutor.CallerRunsPolicy();
                break;
            case ABORT:
                rejectedHandler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case DISCARD:
                rejectedHandler = new ThreadPoolExecutor.DiscardPolicy();
                break;
            case DISCARD_OLDEST:
                rejectedHandler = new ThreadPoolExecutor.DiscardOldestPolicy();
                break;
            default:
                log.error("初始化线程池-->未知拒绝策略");
                throw new RuntimeException("初始化线程池-->未知拒绝策略");
        }
        rejectedExecutionHandler = rejectedHandler;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        handlerCount.getAndIncrement();
        rejectedExecutionHandler.rejectedExecution(r, executor);
    }

    /**
     * 获取拒绝策略执行次数
     * @return
     */
    public int getRejectedExecutionCount(){
        return handlerCount.get();
    }
}
