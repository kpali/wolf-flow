package me.kpali.wolfflow.admin.taskflow;

import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.schedule.ITaskFlowMonitor;

import java.util.ArrayList;
import java.util.List;

public class TaskFlowMonitor implements ITaskFlowMonitor {
    @Override
    public List<TaskFlowLog> listMonitoringTaskFlowLog() {
        return new ArrayList<>();
    }

    @Override
    public void beforeMonitoring(TaskFlowLog taskFlowLog) {

    }

    @Override
    public TaskFlowLog monitoring(TaskFlowLog taskFlowLog) {
        return null;
    }

    @Override
    public void updateTaskFlowLog(TaskFlowLog taskFlowLog) {

    }

    @Override
    public void whenWaitForTrigger(TaskFlowLog taskFlowLog) {

    }

    @Override
    public void whenTriggerFail(TaskFlowLog taskFlowLog) {

    }

    @Override
    public void whenExecuting(TaskFlowLog taskFlowLog) {

    }

    @Override
    public void whenExecuteSuccess(TaskFlowLog taskFlowLog) {

    }

    @Override
    public void whenExecuteFail(TaskFlowLog taskFlowLog) {

    }

    @Override
    public void whenInOtherStatus(TaskFlowLog taskFlowLog) {

    }

    @Override
    public void afterMonitoring(TaskFlowLog taskFlowLog) {

    }
}
