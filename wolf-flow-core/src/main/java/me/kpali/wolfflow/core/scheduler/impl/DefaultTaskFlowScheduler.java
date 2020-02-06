package me.kpali.wolfflow.core.scheduler.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.SchedulerConfig;
import me.kpali.wolfflow.core.enums.TaskFlowScheduleStatusEnum;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.enums.TaskStatusEnum;
import me.kpali.wolfflow.core.event.TaskFlowScheduleStatusChangeEvent;
import me.kpali.wolfflow.core.event.TaskFlowStatusChangeEvent;
import me.kpali.wolfflow.core.exception.*;
import me.kpali.wolfflow.core.executor.ITaskFlowExecutor;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.querier.ITaskFlowQuerier;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import me.kpali.wolfflow.core.scheduler.impl.quartz.MyDynamicScheduler;
import me.kpali.wolfflow.core.util.TaskFlowUtils;
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
                        log.info("扫描到新的任务流执行请求，任务流ID：{}", request.getTaskFlow().getId());
                        TaskFlow finalTaskFlow = request.getTaskFlow();
                        TaskFlowContext taskFlowContext = request.getTaskFlowContext();
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
                                this.taskFlowExecutor.beforeExecute(finalTaskFlow, taskFlowContext);
                                // 任务流执行中
                                this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTING.getCode(), null, true);
                                // 开始执行
                                this.taskFlowExecutor.execute(finalTaskFlow, taskFlowContext);
                                this.taskFlowExecutor.afterExecute(finalTaskFlow, taskFlowContext);
                                // 任务流执行成功
                                this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), null, true);
                            } catch (TaskFlowExecuteException | TaskFlowInterruptedException e) {
                                log.error("任务流执行失败！任务流ID：" + finalTaskFlow.getId() + " 异常信息：" + e.getMessage(), e);
                                // 任务流执行失败
                                this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), e.getMessage(), true);
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
    public long trigger(Long taskFlowId, Map<String, String> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
        return this.trigger(taskFlowId, null, null, params);
    }

    @Override
    public long triggerFrom(Long taskFlowId, Long fromTaskId, Map<String, String> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
        return this.trigger(taskFlowId, fromTaskId, null, params);
    }

    @Override
    public long triggerTo(Long taskFlowId, Long toTaskId, Map<String, String> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
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
    private long trigger(Long taskFlowId, Long fromTaskId, Long toTaskId, Map<String, String> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
        // 获取任务流
        TaskFlow taskFlow = this.taskFlowQuerier.getTaskFlow(taskFlowId);

        // 检查任务流是否是一个有向无环图
        List<Task> sortedTaskList = TaskFlowUtils.topologicalSort(taskFlow);
        if (sortedTaskList == null) {
            throw new InvalidTaskFlowException("任务流不是一个有向无环图，请检查是否存在回路！");
        }

        // 根据从指定任务开始或到指定任务结束，对任务流进行剪裁
        TaskFlow prunedTaskFlow = TaskFlowUtils.prune(taskFlow, fromTaskId, toTaskId);

        // 在到指定任务结束的情况下，已经执行成功的任务无需再执行，因此只保留未执行成功的任务
        TaskFlow unsuccessfulTaskFlow = null;
        if (fromTaskId == null && toTaskId != null) {
            unsuccessfulTaskFlow = this.pickOutUnsuccessfulTasks(prunedTaskFlow);
        }

        TaskFlow finalTaskFlow = (unsuccessfulTaskFlow == null ? prunedTaskFlow : unsuccessfulTaskFlow);
        if (finalTaskFlow.getTaskList().size() == 0) {
            throw new InvalidTaskFlowException("没有需要执行的任务！");
        }

        long logId;
        boolean isPartialExecute = (fromTaskId != null || toTaskId != null);
        List<TaskLog> taskLogList = null;
        boolean locked = false;
        try {
            locked = this.clusterController.tryLock(
                    ClusterConstants.TASK_LOG_LOCK,
                    ClusterConstants.LOG_LOCK_WAIT_TIME,
                    ClusterConstants.LOG_LOCK_LEASE_TIME,
                    TimeUnit.SECONDS);
            if (!locked) {
                throw new TryLockException("获取任务状态记录锁失败！");
            }
            taskLogList = this.taskLogger.lastByTaskFlowId(finalTaskFlow.getId());
        } finally {
            if (locked) {
                this.clusterController.unlock(ClusterConstants.TASK_LOG_LOCK);
            }
        }
        boolean isNewLog= !(isPartialExecute && taskLogList != null && !taskLogList.isEmpty());
        if (isNewLog) {
            // 任务流从未执行过或任务流全部重新执行，则生成新的日志ID
            logId = systemTimeUtils.getUniqueTimeStamp();
        } else {
            // 任务流存在执行记录或任务流部分继续执行，则使用之前的日志ID
            locked = false;
            try {
                locked = this.clusterController.tryLock(
                        ClusterConstants.TASK_FLOW_LOG_LOCK,
                        ClusterConstants.LOG_LOCK_WAIT_TIME,
                        ClusterConstants.LOG_LOCK_LEASE_TIME,
                        TimeUnit.SECONDS);
                if (!locked) {
                    throw new TryLockException("获取任务流状态记录锁失败！");
                }
                TaskFlowContext taskFlowContext = this.taskFlowLogger.last(finalTaskFlow.getId()).getTaskFlowContext();
                logId = Long.parseLong(taskFlowContext.get(ContextKey.LOG_ID));
            } finally {
                if (locked) {
                    this.clusterController.unlock(ClusterConstants.TASK_FLOW_LOG_LOCK);
                }
            }
        }
        String logIdStr = String.valueOf(logId);

        // 初始化任务流上下文
        TaskFlowContext taskFlowContext = new TaskFlowContext();
        if (params != null) {
            taskFlowContext.setParams(params);
        }
        taskFlowContext.put(ContextKey.FROM_TASK_ID, String.valueOf(fromTaskId));
        taskFlowContext.put(ContextKey.TO_TASK_ID, String.valueOf(toTaskId));
        taskFlowContext.put(ContextKey.LOG_ID, logIdStr);

        TaskFlowStatus taskFlowWaitForExecute = new TaskFlowStatus();
        taskFlowWaitForExecute.setTaskFlow(finalTaskFlow);
        taskFlowWaitForExecute.setTaskFlowContext(taskFlowContext);
        taskFlowWaitForExecute.setStatus(TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode());
        taskFlowWaitForExecute.setMessage(null);
        locked = false;
        try {
            locked = this.clusterController.tryLock(
                    ClusterConstants.TASK_FLOW_LOG_LOCK,
                    ClusterConstants.LOG_LOCK_WAIT_TIME,
                    ClusterConstants.LOG_LOCK_LEASE_TIME,
                    TimeUnit.SECONDS);
            if (!locked) {
                throw new TryLockException("获取任务流状态记录锁失败！");
            }
            if (!this.schedulerConfig.getAllowParallel()) {
                TaskFlowLog taskFlowLog = this.taskFlowLogger.last(finalTaskFlow.getId());
                boolean isInProgress = taskFlowLog != null && this.taskFlowLogger.isInProgress(taskFlowLog);
                if (isInProgress) {
                    throw new TaskFlowTriggerException("不允许同时多次执行！");
                }
            }
            TaskFlowLog taskFlowLog = new TaskFlowLog();
            taskFlowLog.setLogId(logId);
            taskFlowLog.setTaskFlowId(taskFlow.getId());
            taskFlowLog.setCompleteTaskFlow(taskFlow);
            taskFlowLog.setTaskFlow(taskFlowWaitForExecute.getTaskFlow());
            taskFlowLog.setTaskFlowContext(taskFlowWaitForExecute.getTaskFlowContext());
            taskFlowLog.setStatus(taskFlowWaitForExecute.getStatus());
            taskFlowLog.setMessage(taskFlowWaitForExecute.getMessage());
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
        // 任务流等待执行
        this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, false);

        // 插入执行请求队列
        boolean success = this.clusterController.execRequestOffer(new TaskFlowExecRequest(finalTaskFlow, taskFlowContext));
        if (!success) {
            this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), "插入执行请求队列失败", true);
        }

        return logId;
    }

    @Override
    public void stop(Long logId) throws TaskFlowStopException {
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
                throw new TryLockException("获取任务流状态记录锁失败！");
            }
            taskFlowLog = this.taskFlowLogger.get(logId);
            // 如果任务流正在处理中，则更新任务流状态为停止中
            if (taskFlowLog != null && this.taskFlowLogger.isInProgress(taskFlowLog)) {
                stopping = true;
                if (!this.clusterController.stopRequestContains(logId)) {
                    this.clusterController.stopRequestAdd(logId);
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
            taskFlowStatus.setTaskFlow(taskFlowLog.getCompleteTaskFlow());
            taskFlowStatus.setTaskFlowContext(taskFlowLog.getTaskFlowContext());
            taskFlowStatus.setStatus(taskFlowLog.getStatus());
            taskFlowStatus.setMessage(taskFlowLog.getMessage());
            this.publishTaskFlowStatusChangeEvent(taskFlowStatus.getTaskFlow(), taskFlowStatus.getTaskFlowContext(), TaskFlowStatusEnum.STOPPING.getCode(), null, false);
        }
    }

    /**
     * 挑出未执行成功的任务
     *
     * @param taskFlow
     * @return
     */
    private TaskFlow pickOutUnsuccessfulTasks(TaskFlow taskFlow) {
        List<Long> successTaskIdList = new ArrayList<>();
        List<TaskLog> taskLogList = null;
        boolean locked = false;
        try {
            locked = this.clusterController.tryLock(
                    ClusterConstants.TASK_LOG_LOCK,
                    ClusterConstants.LOG_LOCK_WAIT_TIME,
                    ClusterConstants.LOG_LOCK_LEASE_TIME,
                    TimeUnit.SECONDS);
            if (!locked) {
                throw new TryLockException("获取任务状态记录锁失败！");
            }
            taskLogList = this.taskLogger.lastByTaskFlowId(taskFlow.getId());
        } finally {
            if (locked) {
                this.clusterController.unlock(ClusterConstants.TASK_LOG_LOCK);
            }
        }
        if (taskLogList == null || taskLogList.isEmpty()) {
            return taskFlow;
        }

        for (TaskLog taskLog : taskLogList) {
            if (TaskStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskLog.getStatus())) {
                successTaskIdList.add(taskLog.getTask().getId());
            }
        }
        TaskFlow unsuccessTaskFlow = new TaskFlow();
        unsuccessTaskFlow.setId(taskFlow.getId());
        unsuccessTaskFlow.setCron(taskFlow.getCron());
        unsuccessTaskFlow.setTaskList(new ArrayList<>());
        unsuccessTaskFlow.setLinkList(new ArrayList<>());
        for (Task task : taskFlow.getTaskList()) {
            if (!successTaskIdList.contains(task.getId())) {
                unsuccessTaskFlow.getTaskList().add(task);
            }
        }
        for (Link link : taskFlow.getLinkList()) {
            if (!successTaskIdList.contains(link.getSource()) && !successTaskIdList.contains(link.getTarget())) {
                unsuccessTaskFlow.getLinkList().add(link);
            }
        }
        return unsuccessTaskFlow;
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
    private void publishTaskFlowStatusChangeEvent(TaskFlow taskFlow, TaskFlowContext taskFlowContext, String status, String message, boolean record) {
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
                    throw new TryLockException("获取任务流状态记录锁失败！");
                }
                Long logId = Long.parseLong(taskFlowContext.get(ContextKey.LOG_ID));
                TaskFlowLog taskFlowLog = this.taskFlowLogger.get(logId);
                boolean isNewLog = false;
                if (taskFlowLog == null) {
                    isNewLog = true;
                    taskFlowLog = new TaskFlowLog();
                    taskFlowLog.setLogId(logId);
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
