package me.kpali.wolfflow.core.scheduler.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.ClusterConfig;
import me.kpali.wolfflow.core.config.SchedulerConfig;
import me.kpali.wolfflow.core.enums.TaskFlowScheduleStatusEnum;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.enums.TaskStatusEnum;
import me.kpali.wolfflow.core.event.ScheduleStatusChangeEvent;
import me.kpali.wolfflow.core.event.ScheduleStatusEventPublisher;
import me.kpali.wolfflow.core.event.TaskFlowStatusEventPublisher;
import me.kpali.wolfflow.core.event.TaskStatusEventPublisher;
import me.kpali.wolfflow.core.exception.InvalidCronExpressionException;
import me.kpali.wolfflow.core.exception.TaskFlowInterruptedException;
import me.kpali.wolfflow.core.exception.TaskFlowStopException;
import me.kpali.wolfflow.core.exception.TaskFlowTriggerException;
import me.kpali.wolfflow.core.exception.TryLockException;
import me.kpali.wolfflow.core.executor.ITaskFlowExecutor;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.ClusterConstants;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContextKey;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;
import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.model.TaskFlowStatus;
import me.kpali.wolfflow.core.model.TaskLog;
import me.kpali.wolfflow.core.querier.ITaskFlowQuerier;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import me.kpali.wolfflow.core.scheduler.impl.quartz.MyDynamicScheduler;
import me.kpali.wolfflow.core.util.IdGenerator;
import me.kpali.wolfflow.core.util.context.TaskFlowContextWrapper;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务流调度器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskFlowScheduler implements ITaskFlowScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskFlowScheduler.class);

    @Autowired
    private SchedulerConfig schedulerConfig;

    @Autowired
    private ClusterConfig clusterConfig;

    @Autowired
    private TaskFlowStatusEventPublisher taskFlowStatusEventPublisher;

    @Autowired
    private TaskStatusEventPublisher taskStatusEventPublisher;

    @Autowired
    private ScheduleStatusEventPublisher scheduleStatusEventPublisher;

    private boolean started = false;

    @Autowired
    private ITaskFlowQuerier taskFlowQuerier;

    private final ThreadFactory schedulerThreadFactory = new ThreadFactoryBuilder().setNameFormat("task-flow-scheduler-pool-%d").build();
    private ExecutorService schedulerThreadPool;

    @Autowired
    private ITaskFlowExecutor taskFlowExecutor;

    @Autowired
    private ITaskFlowLogger taskFlowLogger;

    @Autowired
    private ITaskLogger taskLogger;

    @Autowired
    private IClusterController clusterController;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    public void startup() {
        if (this.started) {
            return;
        }
        logger.info("Starting task flow scheduler, execRequestScanInterval: {}s, " +
                        "cronScanInterval: {}s, cronScanLockWaitTime: {}s, cronScanLockLeaseTime: {}s, " +
                        "corePoolSize: {}, maximumPoolSize: {}",
                this.schedulerConfig.getExecRequestScanInterval(),
                this.schedulerConfig.getCronScanInterval(), this.schedulerConfig.getCronScanLockWaitTime(), this.schedulerConfig.getCronScanLockLeaseTime(),
                this.schedulerConfig.getCorePoolSize(),
                this.schedulerConfig.getMaximumPoolSize());
        this.started = true;
        this.startTaskFlowScaner();
    }

    /**
     * 启动任务流扫描器
     */
    private void startTaskFlowScaner() {
        ThreadFactory scanerThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("taskFlowScaner-pool-%d").build();
        ExecutorService scanerThreadPool = new ThreadPoolExecutor(2, 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), scanerThreadFactory, new ThreadPoolExecutor.AbortPolicy());

        logger.info("Start scanning task flow execute request...");
        scanerThreadPool.execute(() -> {
            while (true) {
                try {
                    Thread.sleep(this.schedulerConfig.getExecRequestScanInterval() * 1000);
                    // 判断当前执行的线程数是否已达到最大线程数，是则不接收新的执行请求
                    ThreadPoolExecutor threadPoolExecutor = (this.schedulerThreadPool != null) ? (ThreadPoolExecutor) this.schedulerThreadPool : null;
                    if (threadPoolExecutor != null && threadPoolExecutor.getActiveCount() >= threadPoolExecutor.getMaximumPoolSize()) {
                        logger.debug("Task flow scheduler at full load, stop receiving new requests until there are idle threads.");
                        continue;
                    }
                    // 接收执行请求
                    TaskFlowExecRequest request = this.clusterController.execRequestPoll();
                    if (request != null) {
                        Long taskFlowId = request.getTaskFlowId();
                        TaskFlow taskFlow = this.taskFlowQuerier.getTaskFlow(taskFlowId);
                        ConcurrentHashMap<String, Object> context = request.getContext();
                        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
                        Long taskFlowLogId = taskFlowContextWrapper.getValue(TaskFlowContextKey.LOG_ID, Long.class);
                        Boolean isRollback = taskFlowContextWrapper.getValue(TaskFlowContextKey.IS_ROLLBACK, Boolean.class);
                        logger.debug("New task flow execute request was scanned, id: {}, log id: {}, rollback: {}",
                                taskFlowId, taskFlowLogId, isRollback);
                        // 任务流上下文写入当前节点ID
                        taskFlowContextWrapper.put(TaskFlowContextKey.EXECUTED_BY_NODE, this.clusterController.getNodeId());
                        // 任务流执行
                        if (this.schedulerThreadPool == null) {
                            synchronized (this.schedulerThreadFactory) {
                                if (this.schedulerThreadPool == null) {
                                    // 初始化线程池
                                    this.schedulerThreadPool = new ThreadPoolExecutor(
                                            this.schedulerConfig.getCorePoolSize(),
                                            this.schedulerConfig.getMaximumPoolSize(),
                                            60, TimeUnit.SECONDS,
                                            new LinkedBlockingQueue<Runnable>(1024),
                                            this.schedulerThreadFactory,
                                            new ThreadPoolExecutor.AbortPolicy());
                                }
                            }
                        }
                        if (!isRollback) {
                            // 任务流执行
                            this.schedulerThreadPool.execute(() -> {
                                try {
                                    // 任务流执行中
                                    this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.EXECUTING.getCode(), null, true);
                                    this.taskFlowExecutor.beforeExecute(taskFlow, context);
                                    this.taskFlowExecutor.execute(taskFlow, context);
                                    this.taskFlowExecutor.afterExecute(taskFlow, context);
                                    // 任务流执行成功
                                    this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), null, true);
                                } catch (TaskFlowInterruptedException e) {
                                    String msg = e.getMessage();
                                    if (msg == null) {
                                        msg = e.toString();
                                    }
                                    logger.error("Task flow [" + taskFlow.getId() + "] execution terminated! cause: " + e.getMessage(), e);
                                    try {
                                        this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.EXECUTE_STOPPED.getCode(), msg, true);
                                    } catch (Exception e1) {
                                        logger.error("Failed to publish task flow status event! " + e1.getMessage(), e1);
                                    }
                                } catch (Exception e) {
                                    String msg = e.getMessage();
                                    if (msg == null) {
                                        msg = e.toString();
                                    }
                                    logger.error("Task flow [" + taskFlow.getId() + "] execution failed! cause: " + e.getMessage(), e);
                                    try {
                                        this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), msg, true);
                                    } catch (Exception e1) {
                                        logger.error("Failed to publish task flow status event! " + e1.getMessage(), e1);
                                    }
                                }
                            });
                        } else {
                            // 任务流回滚
                            this.schedulerThreadPool.execute(() -> {
                                try {
                                    // 任务流回滚中
                                    this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.ROLLING_BACK.getCode(), null, true);
                                    this.taskFlowExecutor.beforeRollback(taskFlow, context);
                                    this.taskFlowExecutor.rollback(taskFlow, context);
                                    this.taskFlowExecutor.afterRollback(taskFlow, context);
                                    // 任务流回滚成功
                                    this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode(), null, true);
                                } catch (TaskFlowInterruptedException e) {
                                    String msg = e.getMessage();
                                    if (msg == null) {
                                        msg = e.toString();
                                    }
                                    logger.error("Task flow [" + taskFlow.getId() + "] rollback terminated! cause: " + msg, e);
                                    try {
                                        this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.ROLLBACK_STOPPED.getCode(), msg, true);
                                    } catch (Exception e1) {
                                        logger.error("Failed to publish task flow status event! " + e1.getMessage(), e1);
                                    }
                                } catch (Exception e) {
                                    String msg = e.getMessage();
                                    if (msg == null) {
                                        msg = e.toString();
                                    }
                                    logger.error("Task flow [" + taskFlow.getId() + "] rollback failed! cause: " + msg, e);
                                    try {
                                        this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.ROLLBACK_FAILURE.getCode(), msg, true);
                                    } catch (Exception e1) {
                                        logger.error("Failed to publish task flow status event! " + e1.getMessage(), e1);
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to scan task flow execute request! " + e.getMessage(), e);
                }
            }
        });

        logger.info("Start scanning cron task flow...");
        scanerThreadPool.execute(() -> {
            while (true) {
                try {
                    Thread.sleep(this.schedulerConfig.getCronScanInterval() * 1000);

                    // 定时任务流扫描前尝试获取锁，只有拥有锁的节点才能调度并触发定时任务流，避免重复触发
                    boolean res = this.clusterController.tryLock(
                            ClusterConstants.CRON_TASK_FLOW_SCAN_LOCK,
                            this.schedulerConfig.getCronScanLockWaitTime(),
                            this.schedulerConfig.getCronScanLockLeaseTime(),
                            TimeUnit.SECONDS);
                    if (res) {
                        // 获取锁成功
                        logger.debug("Acquire cron task flow scan lock success");
                        String jobGroup = "DefaultJobGroup";
                        // 定时任务流扫描
                        List<TaskFlow> scannedCronTaskFlowList = this.taskFlowQuerier.listCronTaskFlow();
                        List<TaskFlow> cronTaskFlowList = (scannedCronTaskFlowList == null ? new ArrayList<>() : scannedCronTaskFlowList);
                        logger.debug("{} cron task flows were scanned", cronTaskFlowList.size());
                        // 删除无需调度的任务流
                        List<JobKey> removedJobKeyList = new ArrayList<>();
                        Set<JobKey> jobKeySet = MyDynamicScheduler.getJobKeysGroupEquals(jobGroup);
                        for (JobKey jobKey : jobKeySet) {
                            boolean isFound = false;
                            for (TaskFlow taskFlow : cronTaskFlowList) {
                                String name = String.valueOf(taskFlow.getId());
                                if (name.equals(jobKey.getName())) {
                                    isFound = true;
                                    break;
                                }
                            }
                            if (!isFound) {
                                removedJobKeyList.add(jobKey);
                            }
                        }
                        for (JobKey jobKey : removedJobKeyList) {
                            MyDynamicScheduler.removeJob(jobKey.getName(), jobKey.getGroup());
                        }
                        // 新增或更新任务流调度
                        for (TaskFlow taskFlow : cronTaskFlowList) {
                            try {
                                String name = String.valueOf(taskFlow.getId());
                                String cronExpression = taskFlow.getCron();
                                if (cronExpression == null || cronExpression.length() == 0) {
                                    throw new InvalidCronExpressionException("Cron expression cannot be null or empty");
                                }
                                Map<String, Object> jobDataMap = new HashMap<>();
                                if (taskFlow.getFromTaskId() != null) {
                                    jobDataMap.put(TaskFlowContextKey.FROM_TASK_ID, taskFlow.getFromTaskId());
                                }
                                if (taskFlow.getToTaskId() != null) {
                                    jobDataMap.put(TaskFlowContextKey.TO_TASK_ID, taskFlow.getToTaskId());
                                }
                                if (!MyDynamicScheduler.checkExists(name, jobGroup)) {
                                    MyDynamicScheduler.addJob(name, jobGroup, cronExpression, jobDataMap);
                                    // 任务流加入调度
                                    ScheduleStatusChangeEvent joinScheduleEvent = new ScheduleStatusChangeEvent(
                                            this,
                                            taskFlow.getId(),
                                            taskFlow.getCron(),
                                            TaskFlowScheduleStatusEnum.JOIN.getCode());
                                    this.scheduleStatusEventPublisher.publishEvent(joinScheduleEvent);
                                } else {
                                    MyDynamicScheduler.updateJobCron(name, jobGroup, cronExpression, jobDataMap);
                                    // 任务流更新调度
                                    ScheduleStatusChangeEvent updateScheduleEvent = new ScheduleStatusChangeEvent(
                                            this,
                                            taskFlow.getId(),
                                            taskFlow.getCron(),
                                            TaskFlowScheduleStatusEnum.UPDATE.getCode());
                                    this.scheduleStatusEventPublisher.publishEvent(updateScheduleEvent);
                                }
                            } catch (Exception e) {
                                logger.error("Failed to scheduling task flow [" + taskFlow.getId() + "], cause: " + e.getMessage());
                                // 任务流调度失败
                                ScheduleStatusChangeEvent scheduleFailEvent = new ScheduleStatusChangeEvent(
                                        this,
                                        taskFlow.getId(),
                                        taskFlow.getCron(),
                                        TaskFlowScheduleStatusEnum.FAIL.getCode());
                                this.scheduleStatusEventPublisher.publishEvent(scheduleFailEvent);
                            }
                        }
                    } else {
                        // 获取锁失败，清理定时调度列表
                        logger.info("Acquire cron task flow scan lock failed");
                        MyDynamicScheduler.clear();
                    }
                } catch (Exception e) {
                    logger.error("Scan cron task flow failed, cause: " + e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public long execute(Long taskFlowId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, false, null, null, params);
    }

    @Override
    public long execute(Long taskFlowId, Long taskId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, false, taskId, taskId, params);
    }

    @Override
    public long executeFrom(Long taskFlowId, Long fromTaskId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, false, fromTaskId, null, params);
    }

    @Override
    public long executeTo(Long taskFlowId, Long toTaskId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, false, null, toTaskId, params);
    }

    @Override
    public long rollback(Long taskFlowId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, true, null, null, params);
    }

    /**
     * 触发任务流
     *
     * @param taskFlowId
     * @param isRollback
     * @param fromTaskId
     * @param toTaskId
     * @param params
     * @return
     * @throws TaskFlowTriggerException
     */
    private long trigger(Long taskFlowId, Boolean isRollback, Long fromTaskId, Long toTaskId, ConcurrentHashMap<String, Object> params) throws TaskFlowTriggerException {
        try {
            // 获取任务流
            TaskFlow taskFlow = this.taskFlowQuerier.getTaskFlow(taskFlowId);
            long taskFlowLogId = idGenerator.nextId();
            String waitForStatus = isRollback ? TaskFlowStatusEnum.WAIT_FOR_ROLLBACK.getCode() : TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode();
            String failureStatus = isRollback ? TaskFlowStatusEnum.ROLLBACK_FAILURE.getCode() : TaskFlowStatusEnum.EXECUTE_FAILURE.getCode();

            // 初始化任务流上下文
            TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper();
            taskFlowContextWrapper.put(TaskFlowContextKey.TASK_FLOW_ID, taskFlowId);
            taskFlowContextWrapper.put(TaskFlowContextKey.IS_ROLLBACK, isRollback);
            if (fromTaskId != null) {
                taskFlowContextWrapper.put(TaskFlowContextKey.FROM_TASK_ID, fromTaskId);
            }
            if (toTaskId != null) {
                taskFlowContextWrapper.put(TaskFlowContextKey.TO_TASK_ID, toTaskId);
            }
            if (params != null) {
                taskFlowContextWrapper.setParams(params);
            }
            taskFlowContextWrapper.put(TaskFlowContextKey.LOG_ID, taskFlowLogId);

            // 新增任务流日志
            String taskFlowLogLock = ClusterConstants.getTaskFlowLogLock(taskFlowId);
            boolean locked = false;
            try {
                locked = this.clusterController.tryLock(
                        taskFlowLogLock,
                        clusterConfig.getTaskFlowLogLockWaitTime(),
                        clusterConfig.getTaskFlowLogLockLeaseTime(),
                        TimeUnit.SECONDS);
                if (!locked) {
                    throw new TryLockException("Acquire the task flow log lock failed!");
                }

                TaskFlowLog lastTaskFlowLog = this.taskFlowLogger.last(taskFlow.getId());
                if (lastTaskFlowLog != null) {
                    if (this.taskFlowLogger.isInProgress(lastTaskFlowLog)) {
                        throw new TaskFlowTriggerException("Task flow is running!");
                    }
                    // 分段执行时，导入上一次的传递上下文
                    if ((fromTaskId != null || toTaskId != null) && isRollback.equals(lastTaskFlowLog.getRollback())) {
                        if (lastTaskFlowLog.getContext() != null && lastTaskFlowLog.getContext().containsKey(TaskFlowContextKey.DELIVERY_CONTEXT)) {
                            taskFlowContextWrapper.put(TaskFlowContextKey.DELIVERY_CONTEXT, lastTaskFlowLog.getContext().get(TaskFlowContextKey.DELIVERY_CONTEXT));
                        }
                    }
                }

                TaskFlowStatus taskFlowWaitForExecute = new TaskFlowStatus();
                taskFlowWaitForExecute.setTaskFlow(taskFlow);
                taskFlowWaitForExecute.setContext(taskFlowContextWrapper.getTaskFlowContext());
                taskFlowWaitForExecute.setStatus(waitForStatus);
                taskFlowWaitForExecute.setMessage(null);

                TaskFlowLog taskFlowLog = new TaskFlowLog();
                taskFlowLog.setLogId(taskFlowLogId);
                taskFlowLog.setTaskFlowId(taskFlow.getId());
                taskFlowLog.setTaskFlow(taskFlowWaitForExecute.getTaskFlow());
                taskFlowLog.setContext(taskFlowWaitForExecute.getContext());
                taskFlowLog.setStatus(taskFlowWaitForExecute.getStatus());
                taskFlowLog.setMessage(taskFlowWaitForExecute.getMessage());
                taskFlowLog.setRollback(isRollback);
                Date now = new Date();
                taskFlowLog.setCreationTime(now);
                taskFlowLog.setUpdateTime(now);
                this.taskFlowLogger.add(taskFlowLog);
            } finally {
                if (locked) {
                    this.clusterController.unlock(taskFlowLogLock);
                }
            }

            // 任务流等待执行
            this.taskFlowStatusEventPublisher.publishEvent(taskFlow, taskFlowContextWrapper.getContext(), waitForStatus, null, false);

            // 插入执行请求队列
            boolean success = this.clusterController.execRequestOffer(new TaskFlowExecRequest(taskFlow.getId(), taskFlowContextWrapper.getContext()));
            if (!success) {
                this.taskFlowStatusEventPublisher.publishEvent(taskFlow, taskFlowContextWrapper.getContext(), failureStatus,
                        "Failed to insert into task flow execution request queue", true);
            }

            return taskFlowLogId;
        } catch (Exception e) {
            throw new TaskFlowTriggerException(e);
        }
    }

    @Override
    public void stop(Long taskFlowLogId) throws TaskFlowStopException {
        try {
            TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
            if (taskFlowLog == null) {
                return;
            }
            String taskFlowLogLock = ClusterConstants.getTaskFlowLogLock(taskFlowLog.getTaskFlowId());
            boolean taskFlowLogLocked = false;
            try {
                taskFlowLogLocked = this.clusterController.tryLock(
                        taskFlowLogLock,
                        clusterConfig.getTaskFlowLogLockWaitTime(),
                        clusterConfig.getTaskFlowLogLockLeaseTime(),
                        TimeUnit.SECONDS);
                if (!taskFlowLogLocked) {
                    throw new TryLockException("Acquire the task flow log lock failed!");
                }
                if (this.taskFlowLogger.isInProgress(taskFlowLog)) {
                    // 检查任务流所在节点是否存活
                    TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(taskFlowLog.getContext());
                    Long nodeId = taskFlowContextWrapper.getValue(TaskFlowContextKey.EXECUTED_BY_NODE, Long.class);
                    if (this.clusterController.isNodeAlive(nodeId)) {
                        // 任务流所在节点存活，则发送停止请求
                        if (!this.clusterController.stopRequestContains(taskFlowLogId)) {
                            this.clusterController.stopRequestAdd(taskFlowLogId);
                        }
                        // 修改任务流状态为停止中
                        taskFlowLog.setStatus(TaskFlowStatusEnum.STOPPING.getCode());
                        taskFlowLog.setMessage(null);
                    } else {
                        // 任务流所在节点已消亡，则强制停止任务流
                        // 修改进行中的任务状态为 执行中止 或 回滚中止
                        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
                        if (taskLogList != null) {
                            for (TaskLog taskLog : taskLogList) {
                                if (this.taskLogger.isInProgress(taskLog)) {
                                    if (!taskFlowLog.getRollback()) {
                                        taskLog.setStatus(TaskStatusEnum.EXECUTE_STOPPED.getCode());
                                        taskLog.setMessage("Task execution is terminated");
                                    } else {
                                        taskLog.setStatus(TaskStatusEnum.ROLLBACK_STOPPED.getCode());
                                        taskLog.setMessage("Task rollback is terminated");
                                    }
                                    this.taskLogger.update(taskLog);
                                    this.taskStatusEventPublisher.publishEvent(taskLog.getTask(), taskLog.getTaskFlowId(), taskLog.getContext(), taskLog.getStatus(), taskLog.getMessage(), false);
                                }
                            }
                        }
                        // 修改任务流状态为 执行中止 或 回滚中止
                        if (!taskFlowLog.getRollback()) {
                            taskFlowLog.setStatus(TaskFlowStatusEnum.EXECUTE_STOPPED.getCode());
                            taskFlowLog.setMessage("Task flow execution is terminated");
                        } else {
                            taskFlowLog.setStatus(TaskFlowStatusEnum.ROLLBACK_STOPPED.getCode());
                            taskFlowLog.setMessage("Task flow rollback is terminated");
                        }
                    }
                    this.taskFlowLogger.update(taskFlowLog);
                    this.taskFlowStatusEventPublisher.publishEvent(taskFlowLog.getTaskFlow(), taskFlowLog.getContext(), taskFlowLog.getStatus(), taskFlowLog.getMessage(), false);
                }
            } finally {
                if (taskFlowLogLocked) {
                    this.clusterController.unlock(taskFlowLogLock);
                }
            }
        } catch (Exception e) {
            throw new TaskFlowStopException(e);
        }
    }
}
