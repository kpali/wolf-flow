package me.kpali.wolfflow.sample.cluster.listener;

import me.kpali.wolfflow.core.event.ScheduleStatusChangeEvent;
import me.kpali.wolfflow.core.event.TaskFlowStatusChangeEvent;
import me.kpali.wolfflow.core.event.TaskStatusChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 任务流事件监听
 * （可选）
 *
 * @author kpali
 */
@Component
public class TaskFlowEventListener {
    @EventListener
    public void taskFlowScheduleStatusChange(ScheduleStatusChangeEvent event) {
        // 任务流调度状态变更监听，主要为定时任务流，状态有[加入调度、更新调度、调度失败]等
    }

    @EventListener
    public void taskFlowStatusChange(TaskFlowStatusChangeEvent event) {
        // 任务流状态变更监听，状态有[等待执行、执行中、执行成功、执行失败、停止中]等
        System.out.println(">>>>>>>>>> Task flow [" + event.getTaskFlowStatus().getTaskFlow().getId() + "] status changed to: " + event.getTaskFlowStatus().getStatus());
    }

    @EventListener
    public void taskStatusChange(TaskStatusChangeEvent event) {
        // 任务状态变更监听，状态有[等待执行、执行中、执行成功、执行失败、停止中、跳过]等
        System.out.println(">>>>>>>>>> Task [" + event.getTaskStatus().getTask().getId() + "] status changed to: " + event.getTaskStatus().getStatus());
    }
}
