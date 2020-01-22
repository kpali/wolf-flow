package me.kpali.wolfflow.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * 任务流调度状态变更事件
 *
 * @author kpali
 */
public class TaskFlowScheduleStatusChangeEvent extends ApplicationEvent {
    public TaskFlowScheduleStatusChangeEvent(Object source, String taskFlowScheduleStatus) {
        super(source);
        this.taskFlowScheduleStatus = taskFlowScheduleStatus;
    }

    private String taskFlowScheduleStatus;

    public String getTaskFlowScheduleStatus() {
        return taskFlowScheduleStatus;
    }
}
