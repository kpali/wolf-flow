package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.model.TaskFlow;
import org.springframework.context.ApplicationEvent;

/**
 * 任务流等待执行事件
 *
 * @author kpali
 */
public class TaskFlowWaitForExecuteEvent extends ApplicationEvent {
    public TaskFlowWaitForExecuteEvent(Object source, TaskFlow taskFlow) {
        super(source);
        this.taskFlow = taskFlow;
    }

    private TaskFlow taskFlow;

    public TaskFlow getTaskFlow() {
        return taskFlow;
    }
}
