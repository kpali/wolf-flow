package me.kpali.wolfflow.core.querier;

import me.kpali.wolfflow.core.exception.TaskFlowQueryException;
import me.kpali.wolfflow.core.model.TaskFlow;

import java.util.List;

/**
 * 任务流查询器
 *
 * @author kpali
 */
public interface ITaskFlowQuerier {

    /**
     * 根据任务流ID获取任务流
     *
     * @param taskFlowId
     * @return
     * @throws TaskFlowQueryException
     */
    TaskFlow getTaskFlow(Long taskFlowId) throws TaskFlowQueryException;

    /**
     * 获取定时任务流列表
     *
     * @return
     * @throws TaskFlowQueryException
     */
    List<TaskFlow> listCronTaskFlow() throws TaskFlowQueryException;

}
