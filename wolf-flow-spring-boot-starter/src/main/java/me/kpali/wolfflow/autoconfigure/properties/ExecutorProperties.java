package me.kpali.wolfflow.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 执行器配置
 *
 * @author kpali
 */
@ConfigurationProperties(prefix = "wolf-flow.executor")
public class ExecutorProperties {
    private Integer corePoolSize = 3;
    private Integer maximumPoolSize = 3;

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
