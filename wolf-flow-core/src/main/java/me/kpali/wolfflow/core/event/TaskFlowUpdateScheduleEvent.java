package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.model.TaskFlow;
import org.springframework.context.ApplicationEvent;

/**
 * 任务流更新调度事件
 *
 * @author kpali
 */
public class TaskFlowUpdateScheduleEvent extends ApplicationEvent {
    public TaskFlowUpdateScheduleEvent(Object source, TaskFlow taskFlow) {
        super(source);
        this.taskFlow = taskFlow;
    }

    private TaskFlow taskFlow;

    public TaskFlow getTaskFlow() {
        return taskFlow;
    }
}
