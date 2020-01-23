package me.kpali.wolfflow.core.scheduler;

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
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.querier.ITaskFlowQuerier;
import me.kpali.wolfflow.core.recorder.ITaskFlowStatusRecorder;
import me.kpali.wolfflow.core.recorder.ITaskStatusRecorder;
import me.kpali.wolfflow.core.scheduler.quartz.MyDynamicScheduler;
import me.kpali.wolfflow.core.util.TaskFlowUtils;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;
import java.util.concurrent.*;

/**
 * 任务流调度器
 *
 * @author kpali
 */
public class TaskFlowScheduler {
    public TaskFlowScheduler() {
    }

    public TaskFlowScheduler(SchedulerConfig schedulerConfig) {
        this.schedulerConfig = schedulerConfig;
    }

    private static final Logger log = LoggerFactory.getLogger(TaskFlowScheduler.class);

    @Autowired
    SchedulerConfig schedulerConfig;

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
    private ITaskFlowStatusRecorder taskFlowStatusRecorder;
    @Autowired
    private ITaskStatusRecorder taskStatusRecorder;

    @Autowired
    private IClusterController clusterController;

    /**
     * 启动任务流调度器
     */
    public void startup() {
        if (this.started) {
            return;
        }
        log.info("任务流调度器启动，任务流执行请求扫描间隔：{}秒，定时任务流扫描间隔：{}秒，核心线程数：{}，最大线程数：{}",
                this.schedulerConfig.getCronTaskFlowScanInterval(), this.schedulerConfig.getExecRequestScanInterval(),
                this.schedulerConfig.getCorePoolSize(), this.schedulerConfig.getMaximumPoolSize());
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
                    TaskFlowExecRequest request = this.clusterController.poll();
                    if (request != null) {
                        log.info("扫描到新的任务流执行请求，任务流ID：{}", request.getTaskFlow().getId());
                        this.consumeRequest(request);
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
                    Thread.sleep(this.schedulerConfig.getCronTaskFlowScanInterval() * 1000);

                    // 定时任务流扫描前尝试获取锁，只有拥有锁的节点才能调度并触发定时任务流，避免重复触发
                    boolean res = this.clusterController.tryLock("CronTaskFlowScanLock");
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
                                    TaskFlowScheduleStatusChangeEvent taskFlowJoinScheduleEvent = new TaskFlowScheduleStatusChangeEvent(this, TaskFlowScheduleStatusEnum.JOIN.getCode());
                                    this.eventPublisher.publishEvent(taskFlowJoinScheduleEvent);
                                } else {
                                    MyDynamicScheduler.updateJobCron(name, jobGroup, cronExpression);
                                    // 任务流更新调度
                                    TaskFlowScheduleStatusChangeEvent taskFlowUpdateScheduleEvent = new TaskFlowScheduleStatusChangeEvent(this, TaskFlowScheduleStatusEnum.UPDATE.getCode());
                                    this.eventPublisher.publishEvent(taskFlowUpdateScheduleEvent);
                                }
                            } catch (Exception e) {
                                log.error("定时任务流调度失败，任务流ID：" + taskFlow.getId() + "，失败原因：" + e.getMessage());
                                // 任务流调度失败
                                TaskFlowScheduleStatusChangeEvent taskFlowScheduleFailEvent = new TaskFlowScheduleStatusChangeEvent(this, TaskFlowScheduleStatusEnum.FAIL.getCode());
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

    /**
     * 触发任务流
     *
     * @param taskFlowId
     * @param params
     * @return taskFlowExecId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    public String trigger(Long taskFlowId, Map<String, String> params) throws InvalidTaskFlowException,  TaskFlowTriggerException {
        return this.trigger(taskFlowId, null, null, params);
    }

    /**
     * 触发任务流，从指定任务开始
     *
     * @param taskFlowId
     * @param fromTaskId
     * @param params
     * @return taskFlowExecId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    public String triggerFrom(Long taskFlowId, Long fromTaskId, Map<String, String> params) throws InvalidTaskFlowException,  TaskFlowTriggerException {
        return this.trigger(taskFlowId, fromTaskId, null, params);
    }

    /**
     * 触发任务流，到指定任务结束
     *
     * @param taskFlowId
     * @param toTaskId
     * @param params
     * @return taskFlowExecId
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    public String triggerTo(Long taskFlowId, Long toTaskId, Map<String, String> params) throws InvalidTaskFlowException,  TaskFlowTriggerException {
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
    private String trigger(Long taskFlowId, Long fromTaskId, Long toTaskId, Map<String, String> params) throws InvalidTaskFlowException, TaskFlowTriggerException {
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

        String taskFlowExecId;
        boolean isPartialExecute = (fromTaskId != null || toTaskId != null);
        if (isPartialExecute && !this.taskStatusRecorder.listByTaskFlowId(finalTaskFlow.getId()).isEmpty()) {
            // 任务流存在执行记录或任务流部分继续执行，则使用之前的任务流执行ID
            TaskFlowContext taskFlowContext = this.taskFlowStatusRecorder.get(finalTaskFlow.getId()).getTaskFlowContext();
            taskFlowExecId = taskFlowContext.get(ContextKey.TASK_FLOW_EXEC_ID);
        } else {
            // 任务流从未执行过或任务流全部重新执行，则生成新的任务流执行ID
            taskFlowExecId = UUID.randomUUID().toString();
        }
        String taskFlowExecIdString = taskFlowExecId;

        // 初始化任务流上下文
        TaskFlowContext taskFlowContext = new TaskFlowContext();
        if (params != null) {
            taskFlowContext.setParams(params);
        }
        taskFlowContext.put(ContextKey.FROM_TASK_ID, String.valueOf(fromTaskId));
        taskFlowContext.put(ContextKey.TO_TASK_ID, String.valueOf(toTaskId));
        taskFlowContext.put(ContextKey.TASK_FLOW_EXEC_ID, taskFlowExecIdString);

        TaskFlowStatus taskFlowWaitForExecute = new TaskFlowStatus();
        taskFlowWaitForExecute.setTaskFlow(finalTaskFlow);
        taskFlowWaitForExecute.setTaskFlowContext(taskFlowContext);
        taskFlowWaitForExecute.setStatus(TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode());
        taskFlowWaitForExecute.setMessage(null);
        if (!this.schedulerConfig.getAllowParallel()) {
            if (this.taskFlowStatusRecorder.putIfNotInProgress(taskFlowWaitForExecute) == null) {
                throw new TaskFlowTriggerException("不允许同时多次执行！");
            }
        } else {
            this.taskFlowStatusRecorder.put(taskFlowWaitForExecute);
        }
        // 任务流等待执行
        this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, false);

        // 插入执行请求队列
        boolean success = this.clusterController.offer(new TaskFlowExecRequest(finalTaskFlow, taskFlowContext));
        if (!success) {
            this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), "插入执行请求队列失败", true);
        }

        return taskFlowExecId;
    }

    /**
     * 消费任务流请求
     *
     * @return
     * @throws InvalidTaskFlowException
     * @throws TaskFlowTriggerException
     */
    private void consumeRequest(TaskFlowExecRequest request) throws InvalidTaskFlowException, TaskFlowTriggerException {
        TaskFlow finalTaskFlow = request.getTaskFlow();
        TaskFlowContext taskFlowContext = request.getTaskFlowContext();
        // 任务流执行
        if (this.threadPool == null) {
            synchronized (this.lock) {
                if (this.threadPool == null) {
                    // 初始化线程池
                    ThreadFactory triggerThreadFactory = new ThreadFactoryBuilder().setNameFormat("triggerExecutor-pool-%d").build();
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
            } catch (TaskFlowExecuteException | TaskInterruptedException e) {
                log.error("任务流执行失败！任务流ID：" + finalTaskFlow.getId() + " 异常信息：" + e.getMessage(), e);
                // 任务流执行失败
                this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), e.getMessage(), true);
            }
        });
    }

    /**
     * 停止任务流
     *
     * @param taskFlowId
     * @throws TaskFlowStopException
     */
    public void stop(Long taskFlowId) throws TaskFlowStopException {
        TaskFlowStatus taskFlowStatus = this.taskFlowStatusRecorder.toStoppingIfInProgress(taskFlowId);
        if (taskFlowStatus != null) {
            this.publishTaskFlowStatusChangeEvent(taskFlowStatus.getTaskFlow(), taskFlowStatus.getTaskFlowContext(), TaskFlowStatusEnum.STOPPING.getCode(), null, false);
            this.taskFlowExecutor.stop(taskFlowId);
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
        List<TaskStatus> taskStatusList = taskStatusRecorder.listByTaskFlowId(taskFlow.getId());
        if (taskStatusList == null || taskStatusList.isEmpty()) {
            return taskFlow;
        }

        for (TaskStatus taskStatus : taskStatusList) {
            if (TaskStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskStatus.getStatus())) {
                successTaskIdList.add(taskStatus.getTask().getId());
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
            taskFlowStatusRecorder.put(taskFlowStatus);
        }
        TaskFlowStatusChangeEvent taskFlowStatusChangeEvent = new TaskFlowStatusChangeEvent(this, taskFlowStatus);
        this.eventPublisher.publishEvent(taskFlowStatusChangeEvent);
    }
}
