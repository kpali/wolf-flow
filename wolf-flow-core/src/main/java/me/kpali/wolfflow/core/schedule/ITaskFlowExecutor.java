package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;

/**
 * 任务流执行器接口
 *
 * @author kpali
 */
public interface ITaskFlowExecutor {

    /**
     * 初始化任务流上下文
     *
     * @param taskFlow
     * @return
     * @throws Exception
     */
    TaskFlowContext initContext(TaskFlow taskFlow) throws Exception;

    /**
     * 任务流执行前置处理
     *
     * @param taskFlow
     * @param taskFlowContext
     * @throws Exception
     */
    void beforeExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) throws Exception;

    /**
     * 任务流执行
     *
     * @param taskFlow
     * @param taskFlowContext
     * @param fromTaskId
     * @param toTaskId
     * @param taskFlowExecutorCorePoolSize
     * @param taskFlowExecutorMaximumPoolSize
     * @throws Exception
     */
    void execute(TaskFlow taskFlow, TaskFlowContext taskFlowContext,
                 Long fromTaskId, Long toTaskId,
                 Integer taskFlowExecutorCorePoolSize, Integer taskFlowExecutorMaximumPoolSize) throws Exception;

    /**
     * 任务流后置处理
     *
     * @param taskFlow
     * @param taskFlowContext
     * @throws Exception
     */
    void afterExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) throws Exception;

}
