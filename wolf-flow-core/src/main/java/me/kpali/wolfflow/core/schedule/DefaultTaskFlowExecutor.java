package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;

/**
 * 任务流执行器的默认实现
 *
 * @author kpali
 */
public class DefaultTaskFlowExecutor implements ITaskFlowExecutor {
    @Override
    public TaskFlow getTaskFlow(Long taskFlowId) {
        return null;
    }

    @Override
    public TaskFlowContext initContext(TaskFlow taskFlow) {
        return null;
    }

    @Override
    public void beforeExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
        // 不做任何操作
    }

    @Override
    public void execute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
        taskFlow.execute();
    }

    @Override
    public void afterExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
        // 不做任何操作
    }
}
