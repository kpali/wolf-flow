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
        Integer execCorePoolSize = scheduleProperties.getExecCorePoolSize();
        Integer execMaximumPoolSize = scheduleProperties.getExecMaximumPoolSize();
        return new TaskFlowScheduler(scanInterval, execCorePoolSize, execMaximumPoolSize);
    }
}
