package me.kpali.wolfflow.core.querier.impl;

import me.kpali.wolfflow.core.querier.ITaskFlowQuerier;
import me.kpali.wolfflow.core.exception.TaskFlowQueryException;
import me.kpali.wolfflow.core.model.TaskFlow;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务流查询器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskFlowQuerier implements ITaskFlowQuerier {
    @Override
    public TaskFlow getTaskFlow(Long taskFlowId) throws TaskFlowQueryException {
        return null;
    }

    @Override
    public List<TaskFlow> listCronTaskFlow() throws TaskFlowQueryException {
        return null;
    }
}
