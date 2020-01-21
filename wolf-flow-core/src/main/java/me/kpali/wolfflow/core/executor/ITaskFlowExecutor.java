package me.kpali.wolfflow.core.executor;

import me.kpali.wolfflow.core.exception.TaskFlowExecuteException;
import me.kpali.wolfflow.core.exception.TaskFlowInterruptedException;
import me.kpali.wolfflow.core.exception.TaskFlowStopException;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;

/**
 * 任务流执行器接口
 *
 * @author kpali
 */
public interface ITaskFlowExecutor {

    /**
     * 任务流执行前置处理
     *
     * @param taskFlow
     * @param taskFlowContext
     * @throws TaskFlowExecuteException
     */
    void beforeExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) throws TaskFlowExecuteException;

    /**
     * 任务流执行
     *
     * @param taskFlow
     * @param taskFlowContext
     * @param taskFlowExecutorCorePoolSize
     * @param taskFlowExecutorMaximumPoolSize
     * @throws TaskFlowExecuteException
     * @throws TaskFlowInterruptedException
     */
    void execute(TaskFlow taskFlow, TaskFlowContext taskFlowContext,
                 Integer taskFlowExecutorCorePoolSize, Integer taskFlowExecutorMaximumPoolSize) throws TaskFlowExecuteException, TaskFlowInterruptedException;

    /**
     * 任务流后置处理
     *
     * @param taskFlow
     * @param taskFlowContext
     * @throws TaskFlowExecuteException
     */
    void afterExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) throws TaskFlowExecuteException;

    /**
     * 停止任务流
     *
     * @param taskFlowId
     * @throws TaskFlowStopException
     */
    void stop(Long taskFlowId) throws TaskFlowStopException;

}
