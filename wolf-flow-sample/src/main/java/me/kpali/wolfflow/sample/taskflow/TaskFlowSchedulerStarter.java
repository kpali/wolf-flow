package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.schedule.TaskFlowScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class TaskFlowSchedulerStarter implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    TaskFlowScheduler taskFlowScheduler;
    @Autowired
    MyTaskFlowQuerier myTaskFlowQuerier;
    @Autowired
    MyTaskFlowScaner myTaskFlowScaner;
    @Autowired
    MyTaskFlowExecutor myTaskFlowExecutor;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        taskFlowScheduler.startup(myTaskFlowQuerier, myTaskFlowScaner, myTaskFlowExecutor);
        taskFlowScheduler.triggerTo(100L, 5L);
        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        taskFlowScheduler.triggerTo(100L, 6L);
    }
}
