package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MyListener {
    @EventListener
    public void beforeScaning(BeforeScaningEvent event) {
    }

    @EventListener
    public void afterScaning(AfterScaningEvent event) {
    }

    @EventListener
    public void taskFlowJoinSchedule(TaskFlowJoinScheduleEvent event) {
    }

    @EventListener
    public void taskFlowUpdateSchedule(TaskFlowUpdateScheduleEvent event) {
    }

    @EventListener
    public void taskFlowScheduleFail(TaskFlowScheduleFailEvent event) {
    }

    @EventListener
    public void taskFlowStatusChange(TaskFlowStatusChangeEvent event) {
    }

    @EventListener
    public void taskStatusChange(TaskStatusChangeEvent event) {
    }
}
