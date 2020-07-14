package me.kpali.wolfflow.core.executor;

import me.kpali.wolfflow.core.exception.TaskFlowExecuteException;
import me.kpali.wolfflow.core.exception.TaskFlowInterruptedException;
import me.kpali.wolfflow.core.exception.TaskFlowRollbackException;
import me.kpali.wolfflow.core.model.TaskFlow;

import java.util.concurrent.ConcurrentHashMap;

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
     * @param context
     * @throws TaskFlowExecuteException
     */
    void beforeExecute(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowExecuteException;

    /**
     * 任务流执行
     *
     * @param taskFlow
     * @param context
     * @throws TaskFlowExecuteException
     * @throws TaskFlowInterruptedException
     */
    void execute(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowExecuteException, TaskFlowInterruptedException;

    /**
     * 任务流执行后置处理
     *
     * @param taskFlow
     * @param context
     * @throws TaskFlowExecuteException
     */
    void afterExecute(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowExecuteException;

    /**
     * 任务流回滚前置处理
     *
     * @param taskFlow
     * @param context
     * @throws TaskFlowRollbackException
     */
    void beforeRollback(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowRollbackException;

    /**
     * 任务流回滚
     *
     * @param taskFlow
     * @param context
     * @throws TaskFlowRollbackException
     * @throws TaskFlowInterruptedException
     */
    void rollback(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowRollbackException, TaskFlowInterruptedException;

    /**
     * 任务流执行后置处理
     *
     * @param taskFlow
     * @param context
     * @throws TaskFlowRollbackException
     */
    void afterRollback(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowRollbackException;
}
