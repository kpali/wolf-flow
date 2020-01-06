package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.model.TaskFlow;
import org.springframework.context.ApplicationEvent;

/**
 * 任务流执行失败事件
 *
 * @author kpali
 */
public class TaskFlowExecuteFailEvent extends ApplicationEvent {
    public TaskFlowExecuteFailEvent(Object source, TaskFlow taskFlow) {
        super(source);
        this.taskFlow = taskFlow;
    }

    private TaskFlow taskFlow;

    public TaskFlow getTaskFlow() {
        return taskFlow;
    }
}
