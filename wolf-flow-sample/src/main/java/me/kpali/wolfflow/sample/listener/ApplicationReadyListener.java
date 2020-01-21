package me.kpali.wolfflow.sample.listener;

import me.kpali.wolfflow.core.scheduler.TaskFlowScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    TaskFlowScheduler taskFlowScheduler;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        try {
            String taskFlowExecId = taskFlowScheduler.triggerTo(100L, 5L, null);
            System.out.println(">>>>>>>>>> 任务流执行ID：" + taskFlowExecId);
            Thread.sleep(5 * 1000);
            //taskFlowScheduler.stop(100L);
            Thread.sleep(15 * 1000);
            taskFlowExecId = taskFlowScheduler.triggerTo(100L, 6L, null);
            System.out.println(">>>>>>>>>> 任务流执行ID：" + taskFlowExecId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
