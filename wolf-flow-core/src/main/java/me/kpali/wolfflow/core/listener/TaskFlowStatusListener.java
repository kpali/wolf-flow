package me.kpali.wolfflow.core.listener;

import me.kpali.wolfflow.core.event.TaskFlowStatusChangeEvent;
import me.kpali.wolfflow.core.model.Task;
import me.kpali.wolfflow.core.model.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.schedule.ITaskStatusRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

/**
 * 任务流状态监听器
 *
 * @author kpali
 */
public class TaskFlowStatusListener {
    @Autowired
    ITaskStatusRecorder taskStatusRecorder;

    @EventListener
    public void taskFlowStatusChange(TaskFlowStatusChangeEvent event) {
        if (TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode().equals(event.getTaskFlowStatus().getStatus())) {
            for (Task task : event.getTaskFlowStatus().getTaskFlow().getTaskList()) {
                this.taskStatusRecorder.remove(task.getId());
            }
        }
    }
}
