package me.kpali.wolfflow.autoconfigure.config;

import me.kpali.wolfflow.autoconfigure.properties.ScheduleProperties;
import me.kpali.wolfflow.core.schedule.TaskFlowScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = { "me.kpali.wolfflow.core.schedule" })
public class ScheduleConfiguration {
    @Bean
    public TaskFlowScheduler getTaskFlowScheduler(ScheduleProperties scheduleProperties) {
        Integer scanInterval = scheduleProperties.getScanInterval();
        Integer triggerCorePoolSize = scheduleProperties.getTriggerCorePoolSize();
        Integer triggerMaximumPoolSize = scheduleProperties.getTriggerMaximumPoolSize();
        Integer taskFlowExecutorCorePoolSize = scheduleProperties.getTaskFlowExecutorCorePoolSize();
        Integer taskFlowExecutorMaximumPoolSize = scheduleProperties.getTaskFlowExecutorMaximumPoolSize();
        return new TaskFlowScheduler(scanInterval,
                triggerCorePoolSize, triggerMaximumPoolSize,
                taskFlowExecutorCorePoolSize, taskFlowExecutorMaximumPoolSize);
    }
}
