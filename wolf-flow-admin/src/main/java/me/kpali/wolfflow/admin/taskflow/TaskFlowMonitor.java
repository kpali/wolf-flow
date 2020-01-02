package me.kpali.wolfflow.admin.taskflow;

import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.model.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.schedule.DefaultTaskFlowMonitor;

public class TaskFlowMonitor extends DefaultTaskFlowMonitor {
    @Override
    public TaskFlowLog monitoring(TaskFlowLog taskFlowLog) {
        taskFlowLog.setStatus(TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());
        return taskFlowLog;
    }
}
