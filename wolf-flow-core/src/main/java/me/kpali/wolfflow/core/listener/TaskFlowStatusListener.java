package me.kpali.wolfflow.core.listener;

import me.kpali.wolfflow.core.event.TaskFlowStatusChangeEvent;
import me.kpali.wolfflow.core.schedule.ITaskFlowStatusRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 任务流状态监听器
 *
 * @author kpali
 */
@Component
public class TaskFlowStatusListener {
    @Autowired
    ITaskFlowStatusRecorder taskFlowStatusRecorder;

    @EventListener
    public void taskFlowStatusChange(TaskFlowStatusChangeEvent event) throws Exception {
        taskFlowStatusRecorder.put(event.getTaskFlowStatus());
    }
}
