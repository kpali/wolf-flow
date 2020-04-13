package me.kpali.wolfflow.core.scheduler.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.SchedulerConfig;
import me.kpali.wolfflow.core.enums.TaskFlowScheduleStatusEnum;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.event.TaskFlowScheduleStatusChangeEvent;
import me.kpali.wolfflow.core.event.TaskFlowStatusChangeEvent;
import me.kpali.wolfflow.core.exception.*;
import me.kpali.wolfflow.core.executor.ITaskFlowExecutor;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.querier.ITaskFlowQuerier;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import me.kpali.wolfflow.core.scheduler.impl.quartz.MyDynamicScheduler;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private ApplicationEventPublisher eventPublisher;

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
                        TaskFlow executeTaskFlow = request.getTaskFlow();
                        Map<String, Object> taskFlowContext = request.getTaskFlowContext();
                        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(taskFlowContext);
                        Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
                        log.info("扫描到新的任务流执行请求，任务流ID：{}，任务流日志ID：{}，当前节点ID：{}",
                                request.getTaskFlow().getId(), taskFlowLogId, this.clusterController.getNodeId());
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
                        this.threadPool.execute(() -> {
                            try {
                                this.taskFlowExecutor.beforeExecute(executeTaskFlow, taskFlowContext);
                                // 任务流执行中
                                this.publishTaskFlowStatusChangeEvent(executeTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTING.getCode(), null, true);
                                // 开始执行
                                this.taskFlowExecutor.execute(executeTaskFlow, taskFlowContext);
                                this.taskFlowExecutor.afterExecute(executeTaskFlow, taskFlowContext);
                                // 任务流执行成功
                                this.publishTaskFlowStatusChangeEvent(executeTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), null, true);
                            } catch (TaskFlowExecuteException | TaskFlowInterruptedException e) {
                                log.error("任务流执行失败！任务流ID：" + executeTaskFlow.getId() + " 异常信息：" + e.getMessage(), e);
                                // 任务流执行失败
                                this.publishTaskFlowStatusChangeEvent(executeTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), e.getMessage(), true);
                            }
                        });
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
                                if (!MyDynamicScheduler.checkExists(name, jobGroup)) {
                                    MyDynamicScheduler.addJob(name, jobGroup, cronExpression);
                                    // 任务流加入调度
                                    TaskFlowScheduleStatusChangeEvent taskFlowJoinScheduleEvent = new TaskFlowScheduleStatusChangeEvent(
                                            this,
                                            taskFlow.getId(),
                                            taskFlow.getCron(),
                                            TaskFlowScheduleStatusEnum.JOIN.getCode());
                                    this.eventPublisher.publishEvent(taskFlowJoinScheduleEvent);
                                } else {
                                    MyDynamicScheduler.updateJobCron(name, jobGroup, cronExpression);
                                    // 任务流更新调度
                                    TaskFlowScheduleStatusChangeEvent taskFlowUpdateScheduleEvent = new TaskFlowScheduleStatusChangeEvent(
                                            this,
                                            taskFlow.getId(),
                                            taskFlow.getCron(),
                                            TaskFlowScheduleStatusEnum.UPDATE.getCode());
                                    this.eventPublisher.publishEvent(taskFlowUpdateScheduleEvent);
                                }
                            } catch (Exception e) {
                                log.error("定时任务流调度失败，任务流ID：" + taskFlow.getId() + "，失败原因：" + e.getMessage());
                                // 任务流调度失败
                                TaskFlowScheduleStatusChangeEvent taskFlowScheduleFailEvent = new TaskFlowScheduleStatusChangeEvent(
                                        this,
                                        taskFlow.getId(),
                                        taskFlow.getCron(),
                                        TaskFlowScheduleStatusEnum.FAIL.getCode());
                                this.eventPublisher.publishEvent(taskFlowScheduleFailEvent);
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
    public long trigger(Long taskFlowId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
        return this.trigger(taskFlowId, null, null, params);
    }

    @Override
    public long trigger(Long taskFlowId, Long taskId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
        return this.trigger(taskFlowId, taskId, taskId, params);
    }

    @Override
    public long triggerFrom(Long taskFlowId, Long fromTaskId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
        return this.trigger(taskFlowId, fromTaskId, null, params);
    }

    @Override
    public long triggerTo(Long taskFlowId, Long toTaskId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
        return this.trigger(taskFlowId, null, toTaskId, params);
    }

    /**
     * 触发任务流
     *
     * @param taskFlowId
     * @param fromTaskId
     * @param toTaskId
     * @param params
     * @return
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    private long trigger(Long taskFlowId, Long fromTaskId, Long toTaskId, Map<String, Object> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
        // 获取任务流
        TaskFlow taskFlow = this.taskFlowQuerier.getTaskFlow(taskFlowId);

        long taskFlowLogId = systemTimeUtils.getUniqueTimeStamp();

        // 初始化任务流上下文
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper();
        if (params != null) {
            taskFlowContextWrapper.setParams(params);
        }
        if (fromTaskId != null) {
            taskFlowContextWrapper.put(ContextKey.FROM_TASK_ID, fromTaskId);
        }
        if (toTaskId != null) {
            taskFlowContextWrapper.put(ContextKey.TO_TASK_ID, toTaskId);
        }
        taskFlowContextWrapper.put(ContextKey.LOG_ID, taskFlowLogId);

        TaskFlowStatus taskFlowWaitForExecute = new TaskFlowStatus();
        taskFlowWaitForExecute.setTaskFlow(taskFlow);
        taskFlowWaitForExecute.setTaskFlowContext(taskFlowContextWrapper.getContext());
        taskFlowWaitForExecute.setStatus(TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode());
        taskFlowWaitForExecute.setMessage(null);

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
            boolean isInProgress = lastTaskFlowLog != null && this.taskFlowLogger.isInProgress(lastTaskFlowLog);
            if (isInProgress) {
                throw new TaskFlowTriggerException("不允许同时多次执行！");
            }

            TaskFlowLog taskFlowLog = new TaskFlowLog();
            taskFlowLog.setLogId(taskFlowLogId);
            taskFlowLog.setTaskFlowId(taskFlow.getId());
            taskFlowLog.setTaskFlow(taskFlowWaitForExecute.getTaskFlow());
            taskFlowLog.setTaskFlowContext(taskFlowWaitForExecute.getTaskFlowContext());
            taskFlowLog.setStatus(taskFlowWaitForExecute.getStatus());
            taskFlowLog.setMessage(taskFlowWaitForExecute.getMessage());
            this.taskFlowLogger.add(taskFlowLog);
        } finally {
            if (locked) {
                this.clusterController.unlock(ClusterConstants.TASK_FLOW_LOG_LOCK);
            }
        }

        // 任务流等待执行
        this.publishTaskFlowStatusChangeEvent(taskFlow, taskFlowContextWrapper.getContext(), TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, false);

        // 插入执行请求队列
        boolean success = this.clusterController.execRequestOffer(new TaskFlowExecRequest(taskFlow, taskFlowContextWrapper.getContext()));
        if (!success) {
            this.publishTaskFlowStatusChangeEvent(taskFlow, taskFlowContextWrapper.getContext(), TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), "插入执行请求队列失败", true);
        }

        return taskFlowLogId;
    }

    @Override
    public void stop(Long taskFlowLogId) throws TaskFlowStopException {
        TaskFlowLog taskFlowLog = null;
        boolean stopping = false;
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
            taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
            // 如果任务流正在处理中，则更新任务流状态为停止中
            if (taskFlowLog != null && this.taskFlowLogger.isInProgress(taskFlowLog)) {
                stopping = true;
                if (!this.clusterController.stopRequestContains(taskFlowLogId)) {
                    this.clusterController.stopRequestAdd(taskFlowLogId);
                }
                taskFlowLog.setStatus(TaskFlowStatusEnum.STOPPING.getCode());
                this.taskFlowLogger.update(taskFlowLog);
            }
        } finally {
            if (locked) {
                this.clusterController.unlock(ClusterConstants.TASK_FLOW_LOG_LOCK);
            }
        }
        if (stopping) {
            TaskFlowStatus taskFlowStatus = new TaskFlowStatus();
            taskFlowStatus.setTaskFlow(taskFlowLog.getTaskFlow());
            taskFlowStatus.setTaskFlowContext(taskFlowLog.getTaskFlowContext());
            taskFlowStatus.setStatus(taskFlowLog.getStatus());
            taskFlowStatus.setMessage(taskFlowLog.getMessage());
            this.publishTaskFlowStatusChangeEvent(taskFlowStatus.getTaskFlow(), taskFlowStatus.getTaskFlowContext(), TaskFlowStatusEnum.STOPPING.getCode(), null, false);
        }
    }

    /**
     * 发布任务流状态变更事件
     *
     * @param taskFlow
     * @param taskFlowContext
     * @param status
     * @param message
     * @param record
     */
    private void publishTaskFlowStatusChangeEvent(TaskFlow taskFlow, Map<String, Object> taskFlowContext, String status, String message, boolean record) {
        TaskFlowStatus taskFlowStatus = new TaskFlowStatus();
        taskFlowStatus.setTaskFlow(taskFlow);
        taskFlowStatus.setTaskFlowContext(taskFlowContext);
        taskFlowStatus.setStatus(status);
        taskFlowStatus.setMessage(message);
        if (record) {
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
                TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(taskFlowContext);
                Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
                TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
                boolean isNewLog = false;
                if (taskFlowLog == null) {
                    isNewLog = true;
                    taskFlowLog = new TaskFlowLog();
                    taskFlowLog.setLogId(taskFlowLogId);
                    taskFlowLog.setTaskFlowId(taskFlow.getId());
                }
                taskFlowLog.setTaskFlow(taskFlow);
                taskFlowLog.setTaskFlowContext(taskFlowContext);
                taskFlowLog.setStatus(status);
                taskFlowLog.setMessage(message);
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
