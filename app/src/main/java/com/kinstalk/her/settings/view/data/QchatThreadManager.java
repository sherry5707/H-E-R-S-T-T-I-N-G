/*
 * Copyright (c) 2018. Beijing Shuzijiayuan, All Rights Reserved.
 * Beijing Shuzijiayuan Confidential and Proprietary
 */

package com.kinstalk.her.settings.view.data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池
 *   Created by Knight.Xu on 2018/4/6.
 */
public class QchatThreadManager {

    private static QchatThreadManager _instance;

    private static int KEEP_ALIVE_TIME_IN_SECONDS = 10;
    private static int QAI_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static int QAI_POOL_MAX_SIZE = 3;
    private final BlockingQueue<Runnable> qaiTasksQueue = new LinkedBlockingQueue<>();
    private ExecutorService qaiThreadPool;

    private QchatThreadManager() {
        // Creates a thread pool manager
        try {
            qaiThreadPool = new ThreadPoolExecutor(
                    QAI_POOL_SIZE,       // Initial pool size
                    QAI_POOL_MAX_SIZE,       // Max pool size
                    KEEP_ALIVE_TIME_IN_SECONDS,
                    TimeUnit.SECONDS,
                    qaiTasksQueue,
                    new QAIThreadFactory(Thread.MAX_PRIORITY - 1),
                    new CallerRunsPolicy());
        } catch (Throwable t) {
            qaiThreadPool = Executors.newCachedThreadPool();
        }
    }

    public synchronized static QchatThreadManager getInstance() {
        if (_instance == null) {
            _instance = new QchatThreadManager();
        }
        return _instance;
    }

    public void start(Runnable runnable) {
        qaiThreadPool.submit(runnable);
    }

    public <T> Future<T> start(Callable<T> callable) {
        return qaiThreadPool.submit(callable);
    }

    /**
     * 正常优先级的线程工厂
     */
    private static class QAIThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private String namePrefix;
        private int mPriority = Thread.NORM_PRIORITY;

        QAIThreadFactory(int priority) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            mPriority = priority;
        }

        public Thread newThread(Runnable r) {
            namePrefix = "qchat-pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            t.setPriority(mPriority);
            if (t.isDaemon())
                t.setDaemon(false);
            return t;
        }
    }

    ;
}
