package me.kpali.wolfflow.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wolf-flow.scheduler")
public class SchedulerProperties {
    private Integer cronTaskFlowScanInterval = 10;
    private Integer triggerCorePoolSize = 10;
    private Integer triggerMaximumPoolSize = 10;
    private Integer taskFlowExecutorCorePoolSize = 3;
    private Integer taskFlowExecutorMaximumPoolSize = 3;
    private Boolean taskFlowAllowParallel = false;

    public Integer getCronTaskFlowScanInterval() {
        return cronTaskFlowScanInterval;
    }

    public void setCronTaskFlowScanInterval(Integer cronTaskFlowScanInterval) {
        this.cronTaskFlowScanInterval = cronTaskFlowScanInterval;
    }

    public Integer getTriggerCorePoolSize() {
        return triggerCorePoolSize;
    }

    public void setTriggerCorePoolSize(Integer triggerCorePoolSize) {
        this.triggerCorePoolSize = triggerCorePoolSize;
    }

    public Integer getTriggerMaximumPoolSize() {
        return triggerMaximumPoolSize;
    }

    public void setTriggerMaximumPoolSize(Integer triggerMaximumPoolSize) {
        this.triggerMaximumPoolSize = triggerMaximumPoolSize;
    }

    public Integer getTaskFlowExecutorCorePoolSize() {
        return taskFlowExecutorCorePoolSize;
    }

    public void setTaskFlowExecutorCorePoolSize(Integer taskFlowExecutorCorePoolSize) {
        this.taskFlowExecutorCorePoolSize = taskFlowExecutorCorePoolSize;
    }

    public Integer getTaskFlowExecutorMaximumPoolSize() {
        return taskFlowExecutorMaximumPoolSize;
    }

    public void setTaskFlowExecutorMaximumPoolSize(Integer taskFlowExecutorMaximumPoolSize) {
        this.taskFlowExecutorMaximumPoolSize = taskFlowExecutorMaximumPoolSize;
    }

    public Boolean getTaskFlowAllowParallel() {
        return taskFlowAllowParallel;
    }

    public void setTaskFlowAllowParallel(Boolean taskFlowAllowParallel) {
        this.taskFlowAllowParallel = taskFlowAllowParallel;
    }
}
