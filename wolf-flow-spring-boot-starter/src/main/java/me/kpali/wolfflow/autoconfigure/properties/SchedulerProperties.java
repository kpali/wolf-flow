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
    private Integer cronScanWaitTime = 10;
    private Integer cronScanLeaseTime = 60;
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

    public Integer getCronScanWaitTime() {
        return cronScanWaitTime;
    }

    public void setCronScanWaitTime(Integer cronScanWaitTime) {
        this.cronScanWaitTime = cronScanWaitTime;
    }

    public Integer getCronScanLeaseTime() {
        return cronScanLeaseTime;
    }

    public void setCronScanLeaseTime(Integer cronScanLeaseTime) {
        this.cronScanLeaseTime = cronScanLeaseTime;
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
