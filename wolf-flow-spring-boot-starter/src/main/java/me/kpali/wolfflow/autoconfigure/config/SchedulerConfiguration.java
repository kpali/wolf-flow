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
        Integer execRequestScanInterval = schedulerProperties.getExecRequestScanInterval();
        Integer cronScanInterval = schedulerProperties.getCronScanInterval();
        Integer cronScanLockWaitTime = schedulerProperties.getCronScanLockWaitTime();
        Integer cronScanLockLeaseTime = schedulerProperties.getCronScanLockLeaseTime();
        Integer corePoolSize = schedulerProperties.getCorePoolSize();
        Integer maximumPoolSize = schedulerProperties.getMaximumPoolSize();
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setExecRequestScanInterval(execRequestScanInterval);
        schedulerConfig.setCronScanInterval(cronScanInterval);
        schedulerConfig.setCronScanLockWaitTime(cronScanLockWaitTime);
        schedulerConfig.setCronScanLockLeaseTime(cronScanLockLeaseTime);
        schedulerConfig.setCorePoolSize(corePoolSize);
        schedulerConfig.setMaximumPoolSize(maximumPoolSize);
        return schedulerConfig;
    }
}
