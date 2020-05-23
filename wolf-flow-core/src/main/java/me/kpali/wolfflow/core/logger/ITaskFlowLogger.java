package me.kpali.wolfflow.core.logger;

import me.kpali.wolfflow.core.exception.TaskFlowLogException;
import me.kpali.wolfflow.core.model.TaskFlowLog;

import java.util.List;

/**
 * 任务流日志器
 *
 * @author kpali
 */
public interface ITaskFlowLogger {
    /**
     * 根据任务流ID获取日志列表
     *
     * @param taskFlowId
     * @return
     * @throws TaskFlowLogException
     */
    List<TaskFlowLog> list(Long taskFlowId) throws TaskFlowLogException;

    /**
     * 根据任务流ID获取最后的任务流日志
     *
     * @param taskFlowId
     * @return
     * @throws TaskFlowLogException
     */
    TaskFlowLog last(Long taskFlowId) throws TaskFlowLogException;

    /**
     * 根据日志ID获取任务流日志
     *
     * @param taskFlowLogId
     * @return
     * @throws TaskFlowLogException
     */
    TaskFlowLog get(Long taskFlowLogId) throws TaskFlowLogException;

    /**
     * 新增任务流日志
     *
     * @param taskFlowLog
     * @throws TaskFlowLogException
     */
    void add(TaskFlowLog taskFlowLog) throws TaskFlowLogException;

    /**
     * 更新任务流日志
     *
     * @param taskFlowLog
     * @throws TaskFlowLogException
     */
    void update(TaskFlowLog taskFlowLog) throws TaskFlowLogException;

    /**
     * 根据日志ID删除任务流日志
     *
     * @param taskFlowLogId
     * @throws TaskFlowLogException
     */
    void delete(Long taskFlowLogId) throws TaskFlowLogException;

    /**
     * 根据任务流ID删除所有任务流日志
     *
     * @param taskFlowId
     * @throws TaskFlowLogException
     */
    void deleteByTaskFlowId(Long taskFlowId) throws TaskFlowLogException;

    /**
     * 查询是否正在处理中
     *
     * @param taskFlowLogId
     * @return
     * @throws TaskFlowLogException
     */
    boolean isInProgress(Long taskFlowLogId) throws TaskFlowLogException;

    /**
     * 查询是否正在处理中
     *
     * @param taskFlowLog
     * @return
     * @throws TaskFlowLogException
     */
    boolean isInProgress(TaskFlowLog taskFlowLog) throws TaskFlowLogException;
}
