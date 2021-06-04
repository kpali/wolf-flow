package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.ClusterConfig;
import me.kpali.wolfflow.core.exception.TaskLogException;
import me.kpali.wolfflow.core.exception.TryLockException;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.util.context.TaskContextWrapper;
import me.kpali.wolfflow.core.util.context.TaskFlowContextWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
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
    private ClusterConfig clusterConfig;

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
    public void publishEvent(Task task, Long taskFlowId, ConcurrentHashMap<String, Object> context, String status, String message, boolean record) throws TryLockException, TaskLogException {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
        TaskContextWrapper taskContextWrapper = taskFlowContextWrapper.getTaskContextWrapper(task.getId().toString());

        TaskStatus taskStatus = new TaskStatus();
        taskStatus.setTask(task);
        taskStatus.setTaskFlowId(taskFlowId);
        taskStatus.setContext(taskContextWrapper.getContext());
        taskStatus.setStatus(status);
        taskStatus.setMessage(message);
        if (record) {
            String taskLogLock = ClusterConstants.getTaskLogLock(task.getId());
            boolean locked = false;
            try {
                locked = this.clusterController.tryLock(
                        taskLogLock,
                        clusterConfig.getTaskLogLockWaitTime(),
                        clusterConfig.getTaskLogLockLeaseTime(),
                        TimeUnit.SECONDS);
                if (!locked) {
                    throw new TryLockException("Acquire the task log lock failed!");
                }
                Long taskFlowLogId = taskFlowContextWrapper.getValue(TaskFlowContextKey.LOG_ID, Long.class);
                boolean isRollback = taskFlowContextWrapper.getValue(TaskFlowContextKey.IS_ROLLBACK, Boolean.class);
                Long taskLogId = taskContextWrapper.getValue(TaskContextKey.TASK_LOG_ID, Long.class);
                String logFileId = taskContextWrapper.getValue(TaskContextKey.TASK_LOG_FILE_ID, String.class);
                TaskLog taskLog = this.taskLogger.get(taskFlowLogId, task.getId());
                boolean isNewLog = false;
                Date now = new Date();
                if (taskLog == null) {
                    isNewLog = true;
                    taskLog = new TaskLog();
                    taskLog.setCreationTime(now);
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
                taskLog.setUpdateTime(now);
                if (isNewLog) {
                    this.taskLogger.add(taskLog);
                } else {
                    this.taskLogger.update(taskLog);
                }
            } finally {
                if (locked) {
                    this.clusterController.unlock(taskLogLock);
                }
            }
        }
        TaskStatusChangeEvent taskStatusChangeEvent = new TaskStatusChangeEvent(this, taskStatus);
        this.eventPublisher.publishEvent(taskStatusChangeEvent);
    }
}
