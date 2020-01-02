package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlowContext;

/**
 * 任务流执行器的默认实现
 *
 * @author kpali
 */
public class DefaultTaskFlowExecutor implements ITaskFlowExecutor {

    @Override
    public TaskFlowContext createContext(Long taskFlowId) {
        return null;
    }

    @Override
    public void beforeExecute(TaskFlowContext taskFlowContext) {
        // 不做任何操作
    }

    @Override
    public void execute(TaskFlowContext taskFlowContext) {
        // 不做任何操作
    }

    @Override
    public void afterExecute(TaskFlowContext taskFlowContext) {
        // 不做任何操作
    }
}
