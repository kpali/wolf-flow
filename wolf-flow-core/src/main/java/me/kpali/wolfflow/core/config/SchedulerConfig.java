package me.kpali.wolfflow.core.config;

import org.springframework.stereotype.Component;

/**
 * 任务流调度器配置
 *
 * @author kpali
 */
@Component
public class SchedulerConfig {
    private Integer execRequestScanInterval;
    private Integer cronTaskFlowScanInterval;
    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Boolean allowParallel;

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
