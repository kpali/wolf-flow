package me.kpali.wolfflow.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wolf-flow.schedule")
public class ScheduleProperties {
    private Integer scanInterval = 10;
    private Integer executorCorePoolSize = 10;
    private Integer executorMaximumPoolSize = 10;
    private Integer taskFlowCorePoolSize = 3;
    private Integer taskFlowMaximumPoolSize = 3;

    public Integer getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(Integer scanInterval) {
        this.scanInterval = scanInterval;
    }

    public Integer getExecutorCorePoolSize() {
        return executorCorePoolSize;
    }

    public void setExecutorCorePoolSize(Integer executorCorePoolSize) {
        this.executorCorePoolSize = executorCorePoolSize;
    }

    public Integer getExecutorMaximumPoolSize() {
        return executorMaximumPoolSize;
    }

    public void setExecutorMaximumPoolSize(Integer executorMaximumPoolSize) {
        this.executorMaximumPoolSize = executorMaximumPoolSize;
    }

    public Integer getTaskFlowCorePoolSize() {
        return taskFlowCorePoolSize;
    }

    public void setTaskFlowCorePoolSize(Integer taskFlowCorePoolSize) {
        this.taskFlowCorePoolSize = taskFlowCorePoolSize;
    }

    public Integer getTaskFlowMaximumPoolSize() {
        return taskFlowMaximumPoolSize;
    }

    public void setTaskFlowMaximumPoolSize(Integer taskFlowMaximumPoolSize) {
        this.taskFlowMaximumPoolSize = taskFlowMaximumPoolSize;
    }
}
