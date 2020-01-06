package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlow;

import java.util.List;

/**
 * 任务流查询器的默认实现
 *
 * @author kpali
 */
public class DefaultTaskFlowQuerier implements ITaskFlowQuerier {
    @Override
    public TaskFlow getTaskFlow(Long taskFlowId) {
        return null;
    }

    @Override
    public List<TaskFlow> listCronTaskFlow() {
        return null;
    }
}
