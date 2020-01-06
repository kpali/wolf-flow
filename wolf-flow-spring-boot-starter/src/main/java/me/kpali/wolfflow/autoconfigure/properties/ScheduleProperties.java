package me.kpali.wolfflow.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wolf-flow.schedule")
public class ScheduleProperties {
    private Integer scanInterval = 10;
    private Integer execCorePoolSize = 10;
    private Integer execMaximumPoolSize = 10;

    public Integer getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(Integer scanInterval) {
        this.scanInterval = scanInterval;
    }

    public Integer getExecCorePoolSize() {
        return execCorePoolSize;
    }

    public void setExecCorePoolSize(Integer execCorePoolSize) {
        this.execCorePoolSize = execCorePoolSize;
    }

    public Integer getExecMaximumPoolSize() {
        return execMaximumPoolSize;
    }

    public void setExecMaximumPoolSize(Integer execMaximumPoolSize) {
        this.execMaximumPoolSize = execMaximumPoolSize;
    }
}
