package me.kpali.wolfflow.core.scheduler;

import me.kpali.wolfflow.core.exception.TaskFlowStopException;
import me.kpali.wolfflow.core.exception.TaskFlowTriggerException;

import java.util.concurrent.ConcurrentHashMap;

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
     * @throws TaskFlowTriggerException
     */
    long execute(Long taskFlowId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException;

    /**
     * 执行任务流，执行指定任务
     *
     * @param taskFlowId
     * @param taskId
     * @param params
     * @return taskFlowLogId
     * @throws TaskFlowTriggerException
     */
    long execute(Long taskFlowId, Long taskId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException;

    /**
     * 执行任务流，从指定任务开始
     *
     * @param taskFlowId
     * @param fromTaskId
     * @param params
     * @return taskFlowLogId
     * @throws TaskFlowTriggerException
     */
    long executeFrom(Long taskFlowId, Long fromTaskId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException;

    /**
     * 执行任务流，到指定任务结束
     *
     * @param taskFlowId
     * @param toTaskId
     * @param params
     * @return taskFlowLogId
     * @throws TaskFlowTriggerException
     */
    long executeTo(Long taskFlowId, Long toTaskId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException;

    /**
     * 回滚任务流
     *
     * @param taskFlowId
     * @param params
     * @return taskFlowLogId
     * @throws TaskFlowTriggerException
     */
    long rollback(Long taskFlowId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException;

    /**
     * 停止任务流
     *
     * @param taskFlowLogId
     * @throws TaskFlowStopException
     */
    void stop(Long taskFlowLogId) throws TaskFlowStopException;
}
