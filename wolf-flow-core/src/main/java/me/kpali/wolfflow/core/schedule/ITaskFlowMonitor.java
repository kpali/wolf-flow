package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlowLog;

import java.util.List;

/**
 * 任务流监视器接口
 *
 * @author kpali
 */
public interface ITaskFlowMonitor {

    /**
     * 获取监视中的任务流日志列表
     *
     * @return
     */
    List<TaskFlowLog> listMonitoringTaskFlowLog();

    /**
     * 监视前置处理
     *
     * @param taskFlowLog
     */
    void beforeMonitoring(TaskFlowLog taskFlowLog);

    /**
     * 监视任务流变化
     *
     * @param taskFlowLog
     * @return 如果任务流有变化，则返回更新后的任务流日志，否则返回null
     */
    TaskFlowLog monitoring(TaskFlowLog taskFlowLog);

    /**
     * 更新任务流日志
     *
     * @param taskFlowLog
     */
    void updateTaskFlowLog(TaskFlowLog taskFlowLog);

    /**
     * 当任务流处于WAIT_FOR_TRIGGER状态时
     *
     * @param taskFlowLog
     */
    void whenWaitForTrigger(TaskFlowLog taskFlowLog);

    /**
     * 当任务流处于TRIGGER_FAIL状态时
     *
     * @param taskFlowLog
     */
    void whenTriggerFail(TaskFlowLog taskFlowLog);

    /**
     * 当任务流处于EXECUTING状态时
     *
     * @param taskFlowLog
     */
    void whenExecuting(TaskFlowLog taskFlowLog);

    /**
     * 当任务流处于EXECUTE_SUCCESS状态时
     *
     * @param taskFlowLog
     */
    void whenExecuteSuccess(TaskFlowLog taskFlowLog);

    /**
     * 当任务流处于EXECUTE_FAIL状态时
     *
     * @param taskFlowLog
     */
    void whenExecuteFail(TaskFlowLog taskFlowLog);

    /**
     * 当任务流处于其他状态时
     *
     * @param taskFlowLog
     */
    void whenInOtherStatus(TaskFlowLog taskFlowLog);

    /**
     * 监视后置处理
     *
     * @param taskFlowLog
     */
    void afterMonitoring(TaskFlowLog taskFlowLog);
}
