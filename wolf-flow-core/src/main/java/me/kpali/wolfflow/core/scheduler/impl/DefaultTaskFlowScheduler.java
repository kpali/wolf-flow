package me.kpali.wolfflow.core.scheduler.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.SchedulerConfig;
import me.kpali.wolfflow.core.enums.TaskFlowScheduleStatusEnum;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.enums.TaskStatusEnum;
import me.kpali.wolfflow.core.event.ScheduleStatusChangeEvent;
import me.kpali.wolfflow.core.event.ScheduleStatusEventPublisher;
import me.kpali.wolfflow.core.event.TaskFlowStatusEventPublisher;
import me.kpali.wolfflow.core.event.TaskStatusEventPublisher;
import me.kpali.wolfflow.core.exception.InvalidCronExpressionException;
import me.kpali.wolfflow.core.exception.TaskFlowStopException;
import me.kpali.wolfflow.core.exception.TaskFlowTriggerException;
import me.kpali.wolfflow.core.exception.TryLockException;
import me.kpali.wolfflow.core.executor.ITaskFlowExecutor;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.querier.ITaskFlowQuerier;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import me.kpali.wolfflow.core.scheduler.impl.quartz.MyDynamicScheduler;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * 任务流调度器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskFlowScheduler implements ITaskFlowScheduler {
    private static final Logger log = LoggerFactory.getLogger(DefaultTaskFlowScheduler.class);

    @Autowired
    private SchedulerConfig schedulerConfig;

    @Autowired
    private TaskFlowStatusEventPublisher taskFlowStatusEventPublisher;

    @Autowired
    private TaskStatusEventPublisher taskStatusEventPublisher;

    @Autowired
    private ScheduleStatusEventPublisher scheduleStatusEventPublisher;

    private boolean started = false;
    private final Object lock = new Object();

    @Autowired
    private ITaskFlowQuerier taskFlowQuerier;

    private ExecutorService threadPool;

    @Autowired
    private ITaskFlowExecutor taskFlowExecutor;

    @Autowired
    private ITaskFlowLogger taskFlowLogger;

    @Autowired
    private ITaskLogger taskLogger;

    @Autowired
    private IClusterController clusterController;

    @Autowired
    private SystemTimeUtils systemTimeUtils;

    @Override
    public void startup() {
        if (this.started) {
            return;
        }
        log.info("任务流调度器启动，任务流执行请求扫描间隔：{}秒，" +
                        "定时任务流扫描间隔：{}秒，定时任务流扫描加锁等待时间：{}秒，定时任务流扫描自动解锁时间：{}秒，" +
                        "核心线程数：{}，最大线程数：{}",
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

        log.info("任务流执行请求扫描线程启动");
        scanerThreadPool.execute(() -> {
            while (true) {
                try {
                    Thread.sleep(this.schedulerConfig.getExecRequestScanInterval() * 1000);
                    TaskFlowExecRequest request = this.clusterController.execRequestPoll();
                    if (request != null) {
                        Long taskFlowId = request.getTaskFlowId();
                        TaskFlow taskFlow = this.taskFlowQuerier.getTaskFlow(taskFlowId);
                        Map<String, Object> context = request.getContext();
                        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
                        Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
                        Boolean isRollback = taskFlowContextWrapper.getValue(ContextKey.IS_ROLLBACK, Boolean.class);
                        log.info("扫描到新的任务流执行请求，任务流ID：{}，任务流日志ID：{}，是否回滚：{}, 当前节点ID：{}",
                                taskFlowId, taskFlowLogId, isRollback, this.clusterController.getNodeId());
                        // 任务流上下文写入当前节点ID
                        taskFlowContextWrapper.put(ContextKey.EXECUTED_BY_NODE, this.clusterController.getNodeId());
                        // 任务流执行
                        if (this.threadPool == null) {
                            synchronized (this.lock) {
                                if (this.threadPool == null) {
                                    // 初始化线程池
                                    ThreadFactory triggerThreadFactory = new ThreadFactoryBuilder().setNameFormat("schedulerExecutor-pool-%d").build();
                                    this.threadPool = new ThreadPoolExecutor(this.schedulerConfig.getCorePoolSize(), this.schedulerConfig.getMaximumPoolSize(), 60, TimeUnit.SECONDS,
                                            new LinkedBlockingQueue<Runnable>(1024), triggerThreadFactory, new ThreadPoolExecutor.AbortPolicy());
                                }
                            }
                        }
                        if (!isRollback) {
                            // 任务流执行
                            this.threadPool.execute(() -> {
                                try {
                                    this.taskFlowExecutor.beforeExecute(taskFlow, context);
                                    // 任务流执行中
                                    this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.EXECUTING.getCode(), null, true);
                                    // 开始执行
                                    this.taskFlowExecutor.execute(taskFlow, context);
                                    this.taskFlowExecutor.afterExecute(taskFlow, context);
                                    // 任务流执行成功
                                    this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), null, true);
                                } catch (Exception e) {
                                    log.error("任务流执行失败！任务流ID：" + taskFlow.getId() + " 异常信息：" + e.getMessage(), e);
                                    try {
                                        this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), e.getMessage(), true);
                                    } catch (Exception e1) {
                                        log.error("发布任务流状态变更事件失败！" + e.getMessage(), e);
                                    }
                                }
                            });
                        } else {
                            // 任务流回滚
                            this.threadPool.execute(() -> {
                                try {
                                    this.taskFlowExecutor.beforeRollback(taskFlow, context);
                                    // 任务流回滚中
                                    this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.ROLLING_BACK.getCode(), null, true);
                                    // 开始回滚
                                    this.taskFlowExecutor.rollback(taskFlow, context);
                                    this.taskFlowExecutor.afterRollback(taskFlow, context);
                                    // 任务流回滚成功
                                    this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode(), null, true);
                                } catch (Exception e) {
                                    log.error("任务流回滚失败！任务流ID：" + taskFlow.getId() + " 异常信息：" + e.getMessage(), e);
                                    try {
                                        this.taskFlowStatusEventPublisher.publishEvent(taskFlow, context, TaskFlowStatusEnum.ROLLBACK_FAILURE.getCode(), e.getMessage(), true);
                                    } catch (Exception e1) {
                                        log.error("发布任务流状态变更事件失败！" + e.getMessage(), e);
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    log.error("任务流执行请求扫描异常！" + e.getMessage(), e);
                }
            }
        });

        log.info("定时任务流扫描线程启动");
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
                        log.info("定时任务流扫描线程获取锁成功");
                        String jobGroup = "DefaultJobGroup";
                        // 定时任务流扫描
                        List<TaskFlow> scannedCronTaskFlowList = this.taskFlowQuerier.listCronTaskFlow();
                        List<TaskFlow> cronTaskFlowList = (scannedCronTaskFlowList == null ? new ArrayList<>() : scannedCronTaskFlowList);
                        log.info("共扫描到{}个定时任务流", cronTaskFlowList.size());
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
                                    throw new InvalidCronExpressionException("cron表达式不能为空");
                                }
                                Map<String, Object> jobDataMap = new HashMap<>();
                                if (taskFlow.getFromTaskId() != null) {
                                    jobDataMap.put(ContextKey.FROM_TASK_ID, taskFlow.getFromTaskId());
                                }
                                if (taskFlow.getToTaskId() != null) {
                                    jobDataMap.put(ContextKey.TO_TASK_ID, taskFlow.getToTaskId());
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
                                log.error("定时任务流调度失败，任务流ID：" + taskFlow.getId() + "，失败原因：" + e.getMessage());
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
                        log.info("定时任务流扫描线程获取锁失败");
                        MyDynamicScheduler.clear();
                    }
                } catch (Exception e) {
                    log.error("定时任务流扫描异常！" + e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public long execute(Long taskFlowId, Map<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, false, null, null, params);
    }

    @Override
    public long execute(Long taskFlowId, Long taskId, Map<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, false, taskId, taskId, params);
    }

    @Override
    public long executeFrom(Long taskFlowId, Long fromTaskId, Map<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, false, fromTaskId, null, params);
    }

    @Override
    public long executeTo(Long taskFlowId, Long toTaskId, Map<String, Object> params) throws TaskFlowTriggerException {
        return this.trigger(taskFlowId, false, null, toTaskId, params);
    }

    @Override
    public long rollback(Long taskFlowId, Map<String, Object> params) throws TaskFlowTriggerException {
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
    private long trigger(Long taskFlowId, Boolean isRollback, Long fromTaskId, Long toTaskId, Map<String, Object> params) throws TaskFlowTriggerException {
        try {
            // 获取任务流
            TaskFlow taskFlow = this.taskFlowQuerier.getTaskFlow(taskFlowId);
            long taskFlowLogId = systemTimeUtils.getUniqueTimeStamp();
            String waitForStatus = isRollback ? TaskFlowStatusEnum.WAIT_FOR_ROLLBACK.getCode() : TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode();
            String failureStatus = isRollback ? TaskFlowStatusEnum.ROLLBACK_FAILURE.getCode() : TaskFlowStatusEnum.EXECUTE_FAILURE.getCode();

            // 初始化任务流上下文
            TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper();
            taskFlowContextWrapper.put(ContextKey.TASK_FLOW_ID, taskFlowId);
            taskFlowContextWrapper.put(ContextKey.IS_ROLLBACK, isRollback);
            if (fromTaskId != null) {
                taskFlowContextWrapper.put(ContextKey.FROM_TASK_ID, fromTaskId);
            }
            if (toTaskId != null) {
                taskFlowContextWrapper.put(ContextKey.TO_TASK_ID, toTaskId);
            }
            if (params != null) {
                taskFlowContextWrapper.setParams(params);
            }
            taskFlowContextWrapper.put(ContextKey.LOG_ID, taskFlowLogId);

            // 新增任务流日志
            boolean locked = false;
            try {
                locked = this.clusterController.tryLock(
                        ClusterConstants.TASK_FLOW_LOG_LOCK,
                        ClusterConstants.LOG_LOCK_WAIT_TIME,
                        ClusterConstants.LOG_LOCK_LEASE_TIME,
                        TimeUnit.SECONDS);
                if (!locked) {
                    throw new TryLockException("获取任务流日志记录锁失败！");
                }

                TaskFlowLog lastTaskFlowLog = this.taskFlowLogger.last(taskFlow.getId());
                if (lastTaskFlowLog != null) {
                    if (this.taskFlowLogger.isInProgress(lastTaskFlowLog)) {
                        throw new TaskFlowTriggerException("不允许同时多次执行！");
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
                this.taskFlowLogger.add(taskFlowLog);
            } finally {
                if (locked) {
                    this.clusterController.unlock(ClusterConstants.TASK_FLOW_LOG_LOCK);
                }
            }

            // 任务流等待执行
            this.taskFlowStatusEventPublisher.publishEvent(taskFlow, taskFlowContextWrapper.getContext(), waitForStatus, null, false);

            // 插入执行请求队列
            boolean success = this.clusterController.execRequestOffer(new TaskFlowExecRequest(taskFlow.getId(), taskFlowContextWrapper.getContext()));
            if (!success) {
                this.taskFlowStatusEventPublisher.publishEvent(taskFlow, taskFlowContextWrapper.getContext(), failureStatus, "插入执行请求队列失败", true);
            }

            return taskFlowLogId;
        } catch (Exception e) {
            throw new TaskFlowTriggerException(e);
        }
    }

    @Override
    public void stop(Long taskFlowLogId) throws TaskFlowStopException {
        try {
            boolean taskFlowLogLocked = false;
            try {
                taskFlowLogLocked = this.clusterController.tryLock(
                        ClusterConstants.TASK_FLOW_LOG_LOCK,
                        ClusterConstants.LOG_LOCK_WAIT_TIME,
                        ClusterConstants.LOG_LOCK_LEASE_TIME,
                        TimeUnit.SECONDS);
                if (!taskFlowLogLocked) {
                    throw new TryLockException("获取任务流日志记录锁失败！");
                }
                TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
                if (taskFlowLog != null && this.taskFlowLogger.isInProgress(taskFlowLog)) {
                    // 检查任务流所在节点是否存活
                    TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(taskFlowLog.getContext());
                    String nodeId = taskFlowContextWrapper.getValue(ContextKey.EXECUTED_BY_NODE, String.class);
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
                        // 修改进行中的任务状态为执行失败
                        boolean taskLogLocked = false;
                        try {
                            taskLogLocked = this.clusterController.tryLock(
                                    ClusterConstants.TASK_LOG_LOCK,
                                    ClusterConstants.LOG_LOCK_WAIT_TIME,
                                    ClusterConstants.LOG_LOCK_LEASE_TIME,
                                    TimeUnit.SECONDS);
                            if (!taskLogLocked) {
                                throw new TryLockException("获取任务日志记录锁失败！");
                            }
                            List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
                            if (taskLogList != null) {
                                for (TaskLog taskLog : taskLogList) {
                                    if (this.taskLogger.isInProgress(taskLog)) {
                                        taskLog.setStatus(TaskStatusEnum.EXECUTE_FAILURE.getCode());
                                        taskLog.setMessage("任务被终止执行");
                                        this.taskLogger.update(taskLog);
                                        this.taskStatusEventPublisher.publishEvent(taskLog.getTask(), taskLog.getTaskFlowId(), taskLog.getContext(), taskLog.getStatus(), taskLog.getMessage(), false);
                                    }
                                }
                            }
                        } finally {
                            if (taskLogLocked) {
                                this.clusterController.unlock(ClusterConstants.TASK_LOG_LOCK);
                            }
                        }
                        // 修改任务流状态为执行失败
                        taskFlowLog.setStatus(TaskFlowStatusEnum.EXECUTE_FAILURE.getCode());
                        taskFlowLog.setMessage("任务流被终止执行");
                    }
                    this.taskFlowLogger.update(taskFlowLog);
                    this.taskFlowStatusEventPublisher.publishEvent(taskFlowLog.getTaskFlow(), taskFlowLog.getContext(), taskFlowLog.getStatus(), taskFlowLog.getMessage(), false);
                }
            } finally {
                if (taskFlowLogLocked) {
                    this.clusterController.unlock(ClusterConstants.TASK_FLOW_LOG_LOCK);
                }
            }
        } catch (Exception e) {
            throw new TaskFlowStopException(e);
        }
    }
}
