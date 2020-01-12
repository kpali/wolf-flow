package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.model.Task;
import org.springframework.context.ApplicationEvent;

/**
 * 任务状态变更事件
 *
 * @author kpali
 */
public class TaskStatusChangeEvent extends ApplicationEvent {
    public TaskStatusChangeEvent(Object source, Task task, String status, String message) {
        super(source);
        this.task = task;
        this.status = status;
        this.message = message;
    }

    private Task task;
    private String status;
    private String message;

    public Task getTask() {
        return task;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
