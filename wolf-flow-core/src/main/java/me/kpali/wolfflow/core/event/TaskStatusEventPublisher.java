package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.exception.TryLockException;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 任务状态事件发布器
 *
 * @author kpali
 */
@Component
public class TaskStatusEventPublisher {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private IClusterController clusterController;

    @Autowired
    private ITaskLogger taskLogger;

    /**
     * 发布任务状态变更事件
     *
     * @param task
     * @param taskFlowId
     * @param context
     * @param status
     * @param message
     * @param record
     */
    public void publishEvent(Task task, Long taskFlowId, Map<String, Object> context, String status, String message, boolean record) {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
        TaskContextWrapper taskContextWrapper = taskFlowContextWrapper.getTaskContextWrapper(task.getId().toString());

        TaskStatus taskStatus = new TaskStatus();
        taskStatus.setTask(task);
        taskStatus.setTaskFlowId(taskFlowId);
        taskStatus.setContext(taskContextWrapper.getContext());
        taskStatus.setStatus(status);
        taskStatus.setMessage(message);
        if (record) {
            boolean locked = false;
            try {
                locked = this.clusterController.tryLock(
                        ClusterConstants.TASK_LOG_LOCK,
                        ClusterConstants.LOG_LOCK_WAIT_TIME,
                        ClusterConstants.LOG_LOCK_LEASE_TIME,
                        TimeUnit.SECONDS);
                if (!locked) {
                    throw new TryLockException("获取任务日志记录锁失败！");
                }
                Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
                boolean isRollback = taskFlowContextWrapper.getValue(ContextKey.IS_ROLLBACK, Boolean.class);
                Long taskLogId = taskContextWrapper.getValue(ContextKey.TASK_LOG_ID, Long.class);
                String logFileId = taskContextWrapper.getValue(ContextKey.TASK_LOG_FILE_ID, String.class);
                TaskLog taskLog = this.taskLogger.get(taskFlowLogId, task.getId());
                boolean isNewLog = false;
                if (taskLog == null) {
                    isNewLog = true;
                    taskLog = new TaskLog();
                }
                taskLog.setLogId(taskLogId);
                taskLog.setTaskId(task.getId());
                taskLog.setTask(task);
                taskLog.setTaskFlowLogId(taskFlowLogId);
                taskLog.setTaskFlowId(taskFlowId);
                taskLog.setContext(taskContextWrapper.getContext());
                taskLog.setStatus(status);
                taskLog.setMessage(message);
                taskLog.setLogFileId(logFileId);
                taskLog.setRollback(isRollback);
                if (isNewLog) {
                    this.taskLogger.add(taskLog);
                } else {
                    this.taskLogger.update(taskLog);
                }
            } finally {
                if (locked) {
                    this.clusterController.unlock(ClusterConstants.TASK_LOG_LOCK);
                }
            }
        }
        TaskStatusChangeEvent taskStatusChangeEvent = new TaskStatusChangeEvent(this, taskStatus);
        this.eventPublisher.publishEvent(taskStatusChangeEvent);
    }
}
