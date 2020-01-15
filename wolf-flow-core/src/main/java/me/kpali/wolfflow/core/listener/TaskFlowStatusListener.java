package me.kpali.wolfflow.core.listener;

import me.kpali.wolfflow.core.event.TaskFlowStatusChangeEvent;
import org.springframework.context.event.EventListener;

/**
 * 任务流状态监听器
 *
 * @author kpali
 */
public class TaskFlowStatusListener {
    @EventListener
    public void taskFlowStatusChange(TaskFlowStatusChangeEvent event) {
    }
}
