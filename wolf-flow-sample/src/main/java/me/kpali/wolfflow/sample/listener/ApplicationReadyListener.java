package me.kpali.wolfflow.sample.listener;

import me.kpali.wolfflow.core.schedule.TaskFlowScheduler;
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
            taskFlowScheduler.triggerTo(100L, 5L, null);
            Thread.sleep(5 * 1000);
            taskFlowScheduler.stop(100L);
            Thread.sleep(15 * 1000);
            taskFlowScheduler.triggerTo(100L, 6L, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
