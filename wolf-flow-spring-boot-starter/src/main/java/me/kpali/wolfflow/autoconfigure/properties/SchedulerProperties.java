package me.kpali.wolfflow.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 调度器配置
 *
 * @author kpali
 */
@ConfigurationProperties(prefix = "wolf-flow.scheduler")
public class SchedulerProperties {
    private Integer execRequestScanInterval = 1;
    private Integer cronScanInterval = 10;
    private Integer cronScanLockWaitTime = 10;
    private Integer cronScanLockLeaseTime = 60;
    private Integer corePoolSize = 10;
    private Integer maximumPoolSize = 10;

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
}
