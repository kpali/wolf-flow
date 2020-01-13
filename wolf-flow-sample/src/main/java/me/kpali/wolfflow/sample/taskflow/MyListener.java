package me.kpali.wolfflow.sample.taskflow;

import com.alibaba.fastjson.JSON;
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
        System.out.println(">>>>>>>>>> 任务流状态变更：\r\n" + JSON.toJSON(event.getTaskFlowStatus()));
    }

    @EventListener
    public void taskStatusChange(TaskStatusChangeEvent event) {
        System.out.println(">>>>>>>>>> 任务状态变更：\r\n" + JSON.toJSON(event.getTaskStatus()));
    }
}
