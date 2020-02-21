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
     * 根据任务流日志ID获取任务日志列表
     *
     * @param taskFlowLogId
     * @return
     * @throws TaskLogException
     */
    List<TaskLog> list(Long taskFlowLogId) throws TaskLogException;

    /**
     * 根据任务流日志ID和任务ID获取任务日志
     *
     * @param taskFlowLogId
     * @param taskId
     * @return
     * @throws TaskLogException
     */
    TaskLog get(Long taskFlowLogId, Long taskId) throws TaskLogException;

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
     * 根据任务流日志ID删除任务日志
     *
     * @param taskFlowLogId
     * @throws TaskLogException
     */
    void deleteByTaskFlowLogId(Long taskFlowLogId) throws TaskLogException;

    /**
     * 记录日志内容，如果日志内容不存在则新增，存在则追加
     *
     * @param logFileId
     * @param logContent
     * @param end
     * @return 当前日志总行数
     * @throws TaskLogException
     */
    int log(String logFileId, String logContent, Boolean end) throws TaskLogException;

    /**
     * 查询日志内容
     *
     * @param logFileId
     * @param fromLineNum
     * @return
     * @throws TaskLogException
     */
    TaskLogResult query(String logFileId, Integer fromLineNum) throws TaskLogException;

    /**
     * 新增或更新任务状态
     *
     * @param taskStatus
     * @throws TaskLogException
     */
    void putTaskStatus(TaskLog taskStatus) throws TaskLogException;

    /**
     * 根据任务ID获取任务状态
     *
     * @param taskId
     * @throws TaskLogException
     */
    TaskLog getTaskStatus(Long taskId) throws TaskLogException;

    /**
     * 根据任务流ID获取任务状态列表
     *
     * @param taskFlowId
     * @return
     * @throws TaskLogException
     */
    List<TaskLog> listTaskStatus(Long taskFlowId) throws TaskLogException;

    /**
     * 根据任务ID删除任务状态
     *
     * @param taskId
     * @throws TaskLogException
     */
    void deleteTaskStatus(Long taskId) throws TaskLogException;
}
