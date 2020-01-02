package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlowLog;

import java.util.List;

/**
 * 任务流日志器
 *
 * @author kpali
 */
public interface ITaskFlowLogger {

    /**
     * 新增任务流日志
     *
     * @param taskFlowLog
     * @return taskFlowLogId 任务流日志ID
     */
    Long insert(TaskFlowLog taskFlowLog);

    /**
     * 更新任务流日志
     *
     * @param taskFlowLog
     */
    void update(TaskFlowLog taskFlowLog);

    /**
     * 获取未完成的任务流日志列表
     *
     * @return
     */
    List<TaskFlowLog> listUnfinishedLog();

    /**
     * 删除任务流日志
     *
     * @param taskFlowLogId
     */
    void delete(Long taskFlowLogId);

}
