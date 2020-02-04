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
     * 触发任务流
     *
     * @param taskFlowId
     * @param params
     * @return logId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    long trigger(Long taskFlowId, Map<String, String> params) throws InvalidTaskFlowException, TaskFlowTriggerException;

    /**
     * 触发任务流，从指定任务开始
     *
     * @param taskFlowId
     * @param fromTaskId
     * @param params
     * @return logId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    long triggerFrom(Long taskFlowId, Long fromTaskId, Map<String, String> params) throws InvalidTaskFlowException, TaskFlowTriggerException;

    /**
     * 触发任务流，到指定任务结束
     *
     * @param taskFlowId
     * @param toTaskId
     * @param params
     * @return logId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    long triggerTo(Long taskFlowId, Long toTaskId, Map<String, String> params) throws InvalidTaskFlowException, TaskFlowTriggerException;

    /**
     * 停止任务流
     *
     * @param logId
     * @throws TaskFlowStopException
     */
    void stop(Long logId) throws TaskFlowStopException;
}
