package me.kpali.wolfflow.autoconfigure.config;

import me.kpali.wolfflow.autoconfigure.properties.SchedulerProperties;
import me.kpali.wolfflow.core.config.SchedulerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 调度器配置
 *
 * @author kpali
 */
@ComponentScan(basePackages = {"me.kpali.wolfflow.core.scheduler"})
public class SchedulerConfiguration {
    @Bean
    public SchedulerConfig getSchedulerConfig(SchedulerProperties schedulerProperties) {
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setExecRequestScanInterval(schedulerProperties.getExecRequestScanInterval());
        schedulerConfig.setCronScanInterval(schedulerProperties.getCronScanInterval());
        schedulerConfig.setCronScanLockWaitTime(schedulerProperties.getCronScanLockWaitTime());
        schedulerConfig.setCronScanLockLeaseTime(schedulerProperties.getCronScanLockLeaseTime());
        schedulerConfig.setCorePoolSize(schedulerProperties.getCorePoolSize());
        schedulerConfig.setMaximumPoolSize(schedulerProperties.getMaximumPoolSize());
        return schedulerConfig;
    }
}
