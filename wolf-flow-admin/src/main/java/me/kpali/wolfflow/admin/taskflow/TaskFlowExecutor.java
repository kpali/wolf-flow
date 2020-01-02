package me.kpali.wolfflow.admin.taskflow;

import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.schedule.ITaskFlowExecutor;

public class TaskFlowExecutor implements ITaskFlowExecutor {
    @Override
    public TaskFlowContext createContext(Long taskFlowId) {
        return null;
    }

    @Override
    public Long insertLog(TaskFlowLog taskFlowLog) {
        return null;
    }

    @Override
    public void beforeExecute(TaskFlowContext taskFlowContext) {

    }

    @Override
    public void execute(TaskFlowContext taskFlowContext) {

    }

    @Override
    public void afterExecute(TaskFlowContext taskFlowContext) {

    }
}
