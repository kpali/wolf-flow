package me.kpali.wolfflow.admin;

import me.kpali.wolfflow.admin.taskflow.TaskFlowExecutor;
import me.kpali.wolfflow.admin.taskflow.TaskFlowLogger;
import me.kpali.wolfflow.admin.taskflow.TaskFlowMonitor;
import me.kpali.wolfflow.admin.taskflow.TaskFlowScaner;
import me.kpali.wolfflow.core.schedule.TaskFlowScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class TaskFlowSchedulerStarter implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    TaskFlowScheduler taskFlowScheduler;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        taskFlowScheduler.startup(new TaskFlowScaner(), new TaskFlowExecutor(), new TaskFlowMonitor(), new TaskFlowLogger());
    }
}
