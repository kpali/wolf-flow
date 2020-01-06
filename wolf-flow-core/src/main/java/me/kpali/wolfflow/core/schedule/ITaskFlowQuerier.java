package me.kpali.wolfflow.core.schedule;

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
     */
    TaskFlow getTaskFlow(Long taskFlowId);

    /**
     * 获取定时任务流列表
     *
     * @return
     */
    List<TaskFlow> listCronTaskFlow();

}
