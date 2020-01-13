package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.model.TaskFlowStatus;
import org.springframework.context.ApplicationEvent;

/**
 * 任务流状态变更事件
 *
 * @author kpali
 */
public class TaskFlowStatusChangeEvent extends ApplicationEvent {
    public TaskFlowStatusChangeEvent(Object source, TaskFlowStatus taskFlowStatus) {
        super(source);
        this.taskFlowStatus = taskFlowStatus;
    }

    private TaskFlowStatus taskFlowStatus;

    public TaskFlowStatus getTaskFlowStatus() {
        return taskFlowStatus;
    }

    public void setTaskFlowStatus(TaskFlowStatus taskFlowStatus) {
        this.taskFlowStatus = taskFlowStatus;
    }
}
