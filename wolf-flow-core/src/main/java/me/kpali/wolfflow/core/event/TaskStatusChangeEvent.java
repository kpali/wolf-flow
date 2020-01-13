package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.model.TaskStatus;
import org.springframework.context.ApplicationEvent;

/**
 * 任务状态变更事件
 *
 * @author kpali
 */
public class TaskStatusChangeEvent extends ApplicationEvent {
    public TaskStatusChangeEvent(Object source, TaskStatus taskStatus) {
        super(source);
        this.taskStatus = taskStatus;
    }

    private TaskStatus taskStatus;

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
}
