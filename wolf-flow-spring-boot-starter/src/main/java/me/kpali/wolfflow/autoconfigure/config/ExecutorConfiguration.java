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
        ExecutorConfig executorConfig = new ExecutorConfig();
        executorConfig.setCorePoolSize(executorProperties.getCorePoolSize());
        executorConfig.setMaximumPoolSize(executorProperties.getMaximumPoolSize());
        return executorConfig;
    }
}
