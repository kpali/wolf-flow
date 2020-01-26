package me.kpali.wolfflow.autoconfigure.config;

import me.kpali.wolfflow.autoconfigure.properties.ExecutorProperties;
import me.kpali.wolfflow.core.config.ExecutorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 执行器配置
 *
 * @author kpali
 */
@ComponentScan(basePackages = {"me.kpali.wolfflow.core.executor"})
public class ExecutorConfiguration {
    @Bean
    public ExecutorConfig getExecutorConfig(ExecutorProperties executorProperties) {
        Integer corePoolSize = executorProperties.getCorePoolSize();
        Integer maximumPoolSize = executorProperties.getMaximumPoolSize();
        ExecutorConfig executorConfig = new ExecutorConfig();
        executorConfig.setCorePoolSize(corePoolSize);
        executorConfig.setMaximumPoolSize(maximumPoolSize);
        return executorConfig;
    }
}
