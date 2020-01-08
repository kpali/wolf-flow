package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.model.TaskFlow;
import org.springframework.context.ApplicationEvent;

/**
 * 任务流状态变更事件
 *
 * @author kpali
 */
public class TaskFlowStatusChangeEvent extends ApplicationEvent {
    public TaskFlowStatusChangeEvent(Object source, TaskFlow taskFlow, String status) {
        super(source);
        this.taskFlow = taskFlow;
        this.status = status;
    }

    private TaskFlow taskFlow;
    private String status;

    public TaskFlow getTaskFlow() {
        return taskFlow;
    }

    public String getStatus() {
        return status;
    }
}
