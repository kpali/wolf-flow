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
    private Integer cronTaskFlowScanInterval = 10;
    private Integer corePoolSize = 10;
    private Integer maximumPoolSize = 10;
    private Boolean allowParallel = false;

    public Integer getExecRequestScanInterval() {
        return execRequestScanInterval;
    }

    public void setExecRequestScanInterval(Integer execRequestScanInterval) {
        this.execRequestScanInterval = execRequestScanInterval;
    }

    public Integer getCronTaskFlowScanInterval() {
        return cronTaskFlowScanInterval;
    }

    public void setCronTaskFlowScanInterval(Integer cronTaskFlowScanInterval) {
        this.cronTaskFlowScanInterval = cronTaskFlowScanInterval;
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
