package me.kpali.wolfflow.core.logger;

import me.kpali.wolfflow.core.exception.TaskLogException;
import me.kpali.wolfflow.core.model.TaskLog;
import me.kpali.wolfflow.core.model.TaskLogResult;

import java.util.List;

/**
 * 任务日志器
 *
 * @author kpali
 */
public interface ITaskLogger {
    /**
     * 根据日志ID获取任务日志列表
     *
     * @param logId
     * @return
     * @throws TaskLogException
     */
    List<TaskLog> list(Long logId) throws TaskLogException;

    /**
     * 根据任务ID获取最后的任务日志列表
     *
     * @param taskId
     * @return
     * @throws TaskLogException
     */
    TaskLog last(Long taskId) throws TaskLogException;

    /**
     * 根据任务流ID获取最后的任务日志列表
     *
     * @param taskFlowId
     * @return
     * @throws TaskLogException
     */
    List<TaskLog> lastByTaskFlowId(Long taskFlowId) throws TaskLogException;

    /**
     * 根据日志ID和任务ID获取任务日志
     *
     * @param logId
     * @param taskId
     * @return
     * @throws TaskLogException
     */
    TaskLog get(Long logId, Long taskId) throws TaskLogException;

    /**
     * 新增任务日志
     *
     * @param taskLog
     * @throws TaskLogException
     */
    void add(TaskLog taskLog) throws TaskLogException;

    /**
     * 更新任务日志
     *
     * @param taskLog
     * @throws TaskLogException
     */
    void update(TaskLog taskLog) throws TaskLogException;

    /**
     * 根据日志ID删除任务日志
     *
     * @param logId
     * @throws TaskLogException
     */
    void delete(Long logId) throws TaskLogException;

    /**
     * 根据日志ID和任务ID删除任务日志
     *
     * @param logId
     * @param taskId
     * @throws TaskLogException
     */
    void delete(Long logId, Long taskId) throws TaskLogException;

    /**
     * 记录日志内容，如果日志内容不存在则新增，存在则追加
     *
     * @param logId
     * @param taskId
     * @param logContent
     * @param end
     * @return 当前日志总行数
     * @throws TaskLogException
     */
    int log(Long logId, Long taskId, String logContent, Boolean end) throws TaskLogException;

    /**
     * 查询日志内容
     *
     * @param logId
     * @param taskId
     * @param fromLineNum
     * @return
     * @throws TaskLogException
     */
    TaskLogResult query(Long logId, Long taskId, Integer fromLineNum) throws TaskLogException;
}
