package me.kpali.wolfflow.core.config;

import org.springframework.stereotype.Component;

/**
 * 任务流执行器配置
 *
 * @author kpali
 */
@Component
public class ExecutorConfig {
    /**
     * 任务流执行器线程池核心线程数
     */
    private Integer corePoolSize;
    /**
     * 任务流执行器线程池最大线程数
     */
    private Integer maximumPoolSize;

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
