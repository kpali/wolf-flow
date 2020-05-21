package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.exception.TryLockException;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.scheduler.impl.SystemTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
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
    private SystemTimeUtils systemTimeUtils;

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
        TaskStatus taskStatus = new TaskStatus();
        taskStatus.setTask(task);
        taskStatus.setTaskFlowId(taskFlowId);
        taskStatus.setContext(context);
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
                TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
                Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
                TaskLog taskLog = this.taskLogger.get(taskFlowLogId, task.getId());
                boolean isNewLog = false;
                if (taskLog == null) {
                    isNewLog = true;
                    Long taskLogId = systemTimeUtils.getUniqueTimeStamp();
                    String logFileId = UUID.randomUUID().toString();
                    Map<String, Object> taskContext = taskFlowContextWrapper.getTaskContext(task.getId().toString());
                    taskContext.put(ContextKey.TASK_LOG_ID, taskLogId);
                    taskContext.put(ContextKey.TASK_LOG_FILE_ID, logFileId);
                    taskLog = new TaskLog();
                    taskLog.setLogId(taskLogId);
                    taskLog.setTaskFlowLogId(taskFlowLogId);
                    taskLog.setTaskId(task.getId());
                    taskLog.setLogFileId(logFileId);
                }
                taskLog.setTask(task);
                taskLog.setTaskFlowId(taskFlowId);
                taskLog.setContext(context);
                taskLog.setStatus(status);
                taskLog.setMessage(message);
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
