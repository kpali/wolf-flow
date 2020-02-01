package me.kpali.wolfflow.core.config;

import org.springframework.stereotype.Component;

/**
 * 任务流调度器配置
 *
 * @author kpali
 */
@Component
public class SchedulerConfig {
    /**
     * 任务流执行请求扫描间隔
     */
    private Integer execRequestScanInterval;
    /**
     * 定时任务流扫描间隔
     */
    private Integer cronScanInterval;
    /**
     * 定时任务流扫描尝试加锁最大等待时间
     */
    private Integer cronScanLockWaitTime;
    /**
     * 定时任务流扫描加锁后自动解锁时间
     */
    private Integer cronScanLockLeaseTime;
    /**
     * 任务流调度器线程池核心线程数
     */
    private Integer corePoolSize;
    /**
     * 任务流调度器线程池最大线程数
     */
    private Integer maximumPoolSize;
    /**
     * 是否允许同一个任务流同时执行多次
     */
    private Boolean allowParallel;

    public Integer getExecRequestScanInterval() {
        return execRequestScanInterval;
    }

    public void setExecRequestScanInterval(Integer execRequestScanInterval) {
        this.execRequestScanInterval = execRequestScanInterval;
    }

    public Integer getCronScanInterval() {
        return cronScanInterval;
    }

    public void setCronScanInterval(Integer cronScanInterval) {
        this.cronScanInterval = cronScanInterval;
    }

    public Integer getCronScanLockWaitTime() {
        return cronScanLockWaitTime;
    }

    public void setCronScanLockWaitTime(Integer cronScanLockWaitTime) {
        this.cronScanLockWaitTime = cronScanLockWaitTime;
    }

    public Integer getCronScanLockLeaseTime() {
        return cronScanLockLeaseTime;
    }

    public void setCronScanLockLeaseTime(Integer cronScanLockLeaseTime) {
        this.cronScanLockLeaseTime = cronScanLockLeaseTime;
    }

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public Boolean getAllowParallel() {
        return allowParallel;
    }

    public void setAllowParallel(Boolean allowParallel) {
        this.allowParallel = allowParallel;
    }
}
