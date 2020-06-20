package me.kpali.wolfflow.core.event;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.ClusterConfig;
import me.kpali.wolfflow.core.exception.TaskFlowLogException;
import me.kpali.wolfflow.core.exception.TryLockException;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.util.context.TaskFlowContextWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 任务流状态事件发布器
 *
 * @author kpali
 */
@Component
public class TaskFlowStatusEventPublisher {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ClusterConfig clusterConfig;

    @Autowired
    private IClusterController clusterController;

    @Autowired
    private ITaskFlowLogger taskFlowLogger;

    /**
     * 发布任务流状态变更事件
     *
     * @param taskFlow
     * @param context
     * @param status
     * @param message
     * @param record
     */
    public void publishEvent(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context, String status, String message, boolean record) throws TryLockException, TaskFlowLogException {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);

        TaskFlowStatus taskFlowStatus = new TaskFlowStatus();
        taskFlowStatus.setTaskFlow(taskFlow);
        taskFlowStatus.setContext(taskFlowContextWrapper.getTaskFlowContext());
        taskFlowStatus.setStatus(status);
        taskFlowStatus.setMessage(message);
        if (record) {
            boolean locked = false;
            try {
                locked = this.clusterController.tryLock(
                        ClusterConstants.TASK_FLOW_LOG_LOCK,
                        clusterConfig.getTaskFlowLogLockWaitTime(),
                        clusterConfig.getTaskFlowLogLockLeaseTime(),
                        TimeUnit.SECONDS);
                if (!locked) {
                    throw new TryLockException("获取任务流日志记录锁失败！");
                }
                Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
                boolean isRollback = taskFlowContextWrapper.getValue(ContextKey.IS_ROLLBACK, Boolean.class);
                TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
                boolean isNewLog = false;
                Date now = new Date();
                if (taskFlowLog == null) {
                    isNewLog = true;
                    taskFlowLog = new TaskFlowLog();
                    taskFlowLog.setCreationTime(now);
                }
                taskFlowLog.setLogId(taskFlowLogId);
                taskFlowLog.setTaskFlowId(taskFlow.getId());
                taskFlowLog.setTaskFlow(taskFlow);
                taskFlowLog.setContext(taskFlowContextWrapper.getTaskFlowContext());
                taskFlowLog.setStatus(status);
                taskFlowLog.setMessage(message);
                taskFlowLog.setRollback(isRollback);
                taskFlowLog.setUpdateTime(now);
                if (isNewLog) {
                    this.taskFlowLogger.add(taskFlowLog);
                } else {
                    this.taskFlowLogger.update(taskFlowLog);
                }
            } finally {
                if (locked) {
                    this.clusterController.unlock(ClusterConstants.TASK_FLOW_LOG_LOCK);
                }
            }
        }
        TaskFlowStatusChangeEvent taskFlowStatusChangeEvent = new TaskFlowStatusChangeEvent(this, taskFlowStatus);
        this.eventPublisher.publishEvent(taskFlowStatusChangeEvent);
    }
}
