package me.kpali.wolfflow.core.scheduler;

import me.kpali.wolfflow.core.exception.InvalidTaskFlowException;
import me.kpali.wolfflow.core.exception.TaskFlowStopException;
import me.kpali.wolfflow.core.exception.TaskFlowTriggerException;

import java.util.Map;

/**
 * 任务流调度器
 *
 * @author kpali
 */
public interface ITaskFlowScheduler {
    /**
     * 启动任务流调度器
     */
    void startup();

    /**
     * 执行任务流
     *
     * @param taskFlowId
     * @param params
     * @return taskFlowLogId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    long execute(Long taskFlowId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException;

    /**
     * 执行任务流，执行指定任务
     *
     * @param taskFlowId
     * @param taskId
     * @param params
     * @return taskFlowLogId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    long execute(Long taskFlowId, Long taskId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException;

    /**
     * 执行任务流，从指定任务开始
     *
     * @param taskFlowId
     * @param fromTaskId
     * @param params
     * @return taskFlowLogId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    long executeFrom(Long taskFlowId, Long fromTaskId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException;

    /**
     * 执行任务流，到指定任务结束
     *
     * @param taskFlowId
     * @param toTaskId
     * @param params
     * @return taskFlowLogId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    long executeTo(Long taskFlowId, Long toTaskId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException;

    /**
     * 回滚任务流
     *
     * @param taskFlowId
     * @param params
     * @return taskFlowLogId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    long rollback(Long taskFlowId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException;

    /**
     * 停止任务流
     *
     * @param taskFlowLogId
     * @throws TaskFlowStopException
     */
    void stop(Long taskFlowLogId) throws TaskFlowStopException;
}
