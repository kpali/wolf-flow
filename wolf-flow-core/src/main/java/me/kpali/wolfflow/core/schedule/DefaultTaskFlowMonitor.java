package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlowLog;

/**
 * 任务流监视器的默认实现
 *
 * @author kpali
 */
public class DefaultTaskFlowMonitor implements ITaskFlowMonitor {
    @Override
    public void beforeMonitoring(TaskFlowLog taskFlowLog) {
        // 不做任何操作
    }

    @Override
    public TaskFlowLog monitoring(TaskFlowLog taskFlowLog) {
        return null;
    }

    @Override
    public void whenWaitForTrigger(TaskFlowLog taskFlowLog) {
        // 不做任何操作
    }

    @Override
    public void whenTriggerFail(TaskFlowLog taskFlowLog) {
        // 不做任何操作
    }

    @Override
    public void whenExecuting(TaskFlowLog taskFlowLog) {
        // 不做任何操作
    }

    @Override
    public void whenExecuteSuccess(TaskFlowLog taskFlowLog) {
        // 不做任何操作
    }

    @Override
    public void whenExecuteFail(TaskFlowLog taskFlowLog) {
        // 不做任何操作
    }

    @Override
    public void whenInOtherStatus(TaskFlowLog taskFlowLog) {
        // 不做任何操作
    }

    @Override
    public void afterMonitoring(TaskFlowLog taskFlowLog) {
        // 不做任何操作
    }
}
