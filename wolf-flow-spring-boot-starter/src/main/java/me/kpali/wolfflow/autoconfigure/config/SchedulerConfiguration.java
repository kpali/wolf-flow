package me.kpali.wolfflow.autoconfigure.config;

import me.kpali.wolfflow.autoconfigure.properties.SchedulerProperties;
import me.kpali.wolfflow.core.scheduler.TaskFlowScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"me.kpali.wolfflow.core.scheduler"})
public class SchedulerConfiguration {
    @Bean
    public TaskFlowScheduler getTaskFlowScheduler(SchedulerProperties schedulerProperties) {
        Integer cronTaskFlowScanInterval = schedulerProperties.getCronTaskFlowScanInterval();
        Integer triggerCorePoolSize = schedulerProperties.getTriggerCorePoolSize();
        Integer triggerMaximumPoolSize = schedulerProperties.getTriggerMaximumPoolSize();
        Integer taskFlowExecutorCorePoolSize = schedulerProperties.getTaskFlowExecutorCorePoolSize();
        Integer taskFlowExecutorMaximumPoolSize = schedulerProperties.getTaskFlowExecutorMaximumPoolSize();
        Boolean taskFlowAllowParallel = schedulerProperties.getTaskFlowAllowParallel();
        TaskFlowScheduler taskFlowScheduler = new TaskFlowScheduler(cronTaskFlowScanInterval,
                triggerCorePoolSize, triggerMaximumPoolSize,
                taskFlowExecutorCorePoolSize, taskFlowExecutorMaximumPoolSize,
                taskFlowAllowParallel);
        taskFlowScheduler.startup();
        return taskFlowScheduler;
    }
}
