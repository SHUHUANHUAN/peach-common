package com.peach.common.thead;

import cn.hutool.core.util.ClassUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peach.common.anno.PoolConfig;
import com.peach.common.enums.RejectedPolicyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/14 14:05
 */
@Slf4j
@Component
public final class ThreadPool {

    public static final Map<Class,PoolCacheInfo> THREAD_POOL_CACHE_MAP = new ConcurrentHashMap<>();


    /**
     * 禁止显示创建
     */
    private ThreadPool() {

    }

    private class PoolCacheInfo{

        private final BlockingQueue<Runnable> blockingQueue;

        private final PeachThreadFactory threadFactory;

        private final PeachRejectHandler handler;

        private final PeachThreadPoolExecutor poolExecutor;

        public PoolCacheInfo(BlockingQueue<Runnable> blockingQueue, PeachThreadFactory threadFactory, PeachRejectHandler handler, PeachThreadPoolExecutor poolExecutor) {
            this.blockingQueue = blockingQueue;
            this.handler = handler;
            this.threadFactory = threadFactory;
            this.poolExecutor = poolExecutor;
        }

        @Override
        public String toString() {
            return poolExecutor.toString() +
                    " MaximumPoolSize:" + poolExecutor.getMaximumPoolSize() +
                    " CorePoolSize:" + poolExecutor.getCorePoolSize() +
                    " LargestPoolSize:" + poolExecutor.getLargestPoolSize() +
                    " blockingQueue:" + blockingQueue.size() +
                    " RejectedExecutionCount:" + handler.getRejectedExecutionCount();
        }
    }

    private class PeachThreadPoolExecutor extends ThreadPoolExecutor {

        private int queueMaxSize = 0;

        public PeachThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,threadFactory,handler);
        }

        void setQueueMaxSize(int queueMaxSize) {
            this.queueMaxSize = queueMaxSize;
        }

        @Override
        public void execute(Runnable command) {
            checkQueueSize();
            super.execute(command);
        }

        @Override
        public Future<?> submit(Runnable task) {
            checkQueueSize();
            return super.submit(task);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            checkQueueSize();
            return super.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            checkQueueSize();
            return super.submit(task, result);
        }

        /**
         * 判断队列数最大值
         */
        private void checkQueueSize() {
            if (!(queueMaxSize > 0)) {
                return;
            }
            int queueSize = getQueue().size();
            if (queueSize > queueMaxSize) {
                throw new RuntimeException("queue size :" + queueSize + "  >" + queueMaxSize);
            }
        }
    }


    /**
     * 根据类反射拿到注解上的线程池参数初始化线程池信息
     * @param tClass
     * @return
     */
    private PoolCacheInfo createPool(Class tClass) {
        PoolConfig poolConfig = (PoolConfig) tClass.getAnnotation(PoolConfig.class);
        BlockingQueue<Runnable> blockingQueue;
        PeachThreadFactory threadFactory = new PeachThreadFactory(tClass.getName());
        PeachThreadPoolExecutor threadPoolExecutor;
        PeachRejectHandler proxyHandler;
        if (poolConfig == null){
            proxyHandler = new PeachRejectHandler(RejectedPolicyEnum.CALLER);
            blockingQueue = new SynchronousQueue<>();
            threadPoolExecutor = new PeachThreadPoolExecutor(0, 1, 0L, TimeUnit.MILLISECONDS, blockingQueue,threadFactory,proxyHandler);
        }else {
            proxyHandler = new PeachRejectHandler(poolConfig.hanler());
            int corePoolSize = poolConfig.corePoolSize();
            if (corePoolSize == 0) {
                blockingQueue = new SynchronousQueue<>();
            } else {
                blockingQueue = new LinkedBlockingQueue<>();
            }
            // 构建对象
            threadPoolExecutor = new PeachThreadPoolExecutor(corePoolSize, poolConfig.maximumPoolSize(), poolConfig.keepAliveTime(), poolConfig.timeUnit(), blockingQueue, threadFactory, proxyHandler);
            // 获取线程队列最大值
            threadPoolExecutor.setQueueMaxSize(poolConfig.queueMaxSize());
        }
        return new PoolCacheInfo(blockingQueue, threadFactory, proxyHandler, threadPoolExecutor);
    }


    /**
     * 新建线程池
     * @param tClass
     * @return
     */
    public ExecutorService newThreadPool(Class tClass) {
        if (tClass == null){
            throw new RuntimeException(" tClass can't be null!");
        }
        PoolCacheInfo poolCacheInfo = createPool(tClass);
        log.info("class: [{}], newThreadPool init success! cacheInfo:[{}]",tClass,poolCacheInfo);
        return poolCacheInfo.poolExecutor;
    }


    /**
     * 新建缓存线程池
     * @param tClass
     * @return
     */
    public ExecutorService newCachedThreadPool(Class tClass){
        if (tClass == null){
            throw new RuntimeException("tClass can't be null!");
        }
        PoolCacheInfo poolCacheInfo = THREAD_POOL_CACHE_MAP.computeIfAbsent(tClass,aClass -> {
            // 创建线程方法
            PoolCacheInfo cacheInfo = createPool(tClass);
            log.info("class: [{}], cachedThreadPool init success! cacheInfo:[{}]",tClass,cacheInfo);
            return cacheInfo;
        });
        return poolCacheInfo.poolExecutor;
    }


    /**
     * 获取线程池信息
     *
     * @return 所有线程对象
     */
    public JSONArray getThreadPoolStatusInfo() {
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<Class, PoolCacheInfo> entry : THREAD_POOL_CACHE_MAP.entrySet()) {
            PoolCacheInfo poolCacheInfo = entry.getValue();
            String name = ClassUtil.getShortClassName(entry.getKey().getName());
            JSONObject jsonObject = convertInfo(name, poolCacheInfo);
            jsonArray.add(jsonObject);
        }
        // 排序
        jsonArray.sort((o1, o2) -> {
            JSONObject jsonObject1 = (JSONObject) o1;
            JSONObject jsonObject2 = (JSONObject) o2;
            return jsonObject2.getLong("taskCount").compareTo(jsonObject1.getLong("taskCount"));
        });
        return jsonArray;
    }

    private JSONObject convertInfo(String name, PoolCacheInfo poolCacheInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        ThreadPoolExecutor threadPoolExecutor = poolCacheInfo.poolExecutor;
        // 核心数
        jsonObject.put("corePoolSize", threadPoolExecutor.getCorePoolSize());
        // 工作集数
        jsonObject.put("poolSize", threadPoolExecutor.getPoolSize());
        // 活跃线程数
        jsonObject.put("activeCount", threadPoolExecutor.getActiveCount());
        // 曾经最大线程数
        jsonObject.put("largestPoolSize", threadPoolExecutor.getLargestPoolSize());
        // 已完成数
        jsonObject.put("completedTaskCount", threadPoolExecutor.getCompletedTaskCount());
        // 总任务数
        jsonObject.put("taskCount", threadPoolExecutor.getTaskCount());
        // 任务队列数
        jsonObject.put("queueSize", poolCacheInfo.blockingQueue.size());
        // 拒绝任务数
        jsonObject.put("rejectedExecutionCount", poolCacheInfo.handler.getRejectedExecutionCount());
        // 最大线程编号
        jsonObject.put("maxThreadNumber", poolCacheInfo.threadFactory.getThreadNumber().get());
        // 最大线程数
        jsonObject.put("maximumPoolSize", threadPoolExecutor.getMaximumPoolSize());
        return jsonObject;
    }


}
