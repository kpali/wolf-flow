package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.Task;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;

/**
 * 任务流执行器的默认实现
 *
 * @author kpali
 */
public class DefaultTaskFlowExecutor implements ITaskFlowExecutor {
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
        if (taskFlow == null) {
            throw new NullPointerException("任务流不能为空");
        }
        for (Task task : taskFlow.getTaskList()) {
            task.execute();
        }
    }

    @Override
    public void afterExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
        // 不做任何操作
    }
}
