package me.kpali.wolfflow.core.executor;

import me.kpali.wolfflow.core.exception.TaskFlowExecuteException;
import me.kpali.wolfflow.core.exception.TaskFlowInterruptedException;
import me.kpali.wolfflow.core.model.TaskFlow;

import java.util.Map;

/**
 * 任务流执行器接口
 *
 * @author kpali
 */
public interface ITaskFlowExecutor {
    /**
     * 获取当前执行器ID
     *
     * @return
     */
    String getExecutorId();

    /**
     * 任务流执行前置处理
     *
     * @param taskFlow
     * @param taskFlowContext
     * @throws TaskFlowExecuteException
     */
    void beforeExecute(TaskFlow taskFlow, Map<String, Object> taskFlowContext) throws TaskFlowExecuteException;

    /**
     * 任务流执行
     *
     * @param taskFlow
     * @param taskFlowContext
     * @throws TaskFlowExecuteException
     * @throws TaskFlowInterruptedException
     */
    void execute(TaskFlow taskFlow, Map<String, Object> taskFlowContext) throws TaskFlowExecuteException, TaskFlowInterruptedException;

    /**
     * 任务流后置处理
     *
     * @param taskFlow
     * @param taskFlowContext
     * @throws TaskFlowExecuteException
     */
    void afterExecute(TaskFlow taskFlow, Map<String, Object> taskFlowContext) throws TaskFlowExecuteException;
}
