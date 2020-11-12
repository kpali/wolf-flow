package me.kpali.wolfflow.core.monitor;

import java.util.concurrent.ExecutorService;

/**
 * 监控器
 *
 * @author kpali
 */
public interface IMonitor {
    /**
     * 监控器初始化
     */
    void init();

    /**
     * 监控线程池
     *
     * @param executor
     * @param executorName
     */
    void monitor(ExecutorService executor, String executorName);

    /**
     * 获取监控指标
     *
     * @return
     */
    String scrape();
}
