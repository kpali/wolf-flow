package me.kpali.wolfflow.admin;

import me.kpali.wolfflow.admin.taskflow.TaskFlowExecutor;
import me.kpali.wolfflow.admin.taskflow.TaskFlowMonitor;
import me.kpali.wolfflow.admin.taskflow.TaskFlowScaner;
import me.kpali.wolfflow.core.schedule.TaskFlowScheduler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public class TaskFlowSchedulerStarter implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        TaskFlowScaner taskFlowScaner = new TaskFlowScaner();
        TaskFlowExecutor taskFlowExecutor = new TaskFlowExecutor();
        TaskFlowMonitor taskFlowMonitor = new TaskFlowMonitor();
        TaskFlowScheduler.startup(taskFlowScaner, 10, taskFlowExecutor, taskFlowMonitor, 10);
    }
}
