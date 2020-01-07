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
        Integer executorCorePoolSize = scheduleProperties.getExecutorCorePoolSize();
        Integer executorMaximumPoolSize = scheduleProperties.getExecutorMaximumPoolSize();
        Integer taskFlowCorePoolSize = scheduleProperties.getTaskFlowCorePoolSize();
        Integer taskFlowMaximumPoolSize = scheduleProperties.getTaskFlowMaximumPoolSize();
        return new TaskFlowScheduler(scanInterval,
                executorCorePoolSize, executorMaximumPoolSize,
                taskFlowCorePoolSize, taskFlowMaximumPoolSize);
    }
}
