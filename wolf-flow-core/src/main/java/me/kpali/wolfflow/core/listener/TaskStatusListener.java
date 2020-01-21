package me.kpali.wolfflow.core.listener;

import me.kpali.wolfflow.core.event.TaskStatusChangeEvent;
import me.kpali.wolfflow.core.recorder.ITaskStatusRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 任务状态监听器
 *
 * @author kpali
 */
@Component
public class TaskStatusListener {
    @Autowired
    ITaskStatusRecorder taskStatusRecorder;

    @EventListener
    public void taskStatusChange(TaskStatusChangeEvent event) {
        this.taskStatusRecorder.put(event.getTaskStatus());
    }
}
