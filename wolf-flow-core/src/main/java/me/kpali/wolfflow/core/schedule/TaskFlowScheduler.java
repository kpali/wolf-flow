package me.kpali.wolfflow.core.schedule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.event.*;
import me.kpali.wolfflow.core.exception.InvalidCronExpressionException;
import me.kpali.wolfflow.core.exception.InvalidTaskFlowException;
import me.kpali.wolfflow.core.exception.SchedulerNotStartedException;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.quartz.MyDynamicScheduler;
import me.kpali.wolfflow.core.util.SystemTime;
import me.kpali.wolfflow.core.util.TaskFlowUtils;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 任务流调度器
 *
 * @author kpali
 */
public class TaskFlowScheduler {
    public TaskFlowScheduler(Integer scanInterval,
                             Integer triggerCorePoolSize, Integer triggerMaximumPoolSize,
                             Integer taskFlowExecutorCorePoolSize, Integer taskFlowExecutorMaximumPoolSize) {
        this.scanInterval = scanInterval;
        this.triggerCorePoolSize = triggerCorePoolSize;
        this.triggerMaximumPoolSize = triggerMaximumPoolSize;
        this.taskFlowExecutorCorePoolSize = taskFlowExecutorCorePoolSize;
        this.taskFlowExecutorMaximumPoolSize = taskFlowExecutorMaximumPoolSize;
    }

    private static final Logger log = LoggerFactory.getLogger(TaskFlowScheduler.class);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private boolean started = false;

    @Autowired
    private ITaskFlowQuerier taskFlowQuerier;

    @Autowired
    private ITaskFlowScaner taskFlowScaner;
    private Integer scanInterval;

    private final Object triggerLock = new Object();
    private ExecutorService triggerThreadPool;
    private Integer triggerCorePoolSize;
    private Integer triggerMaximumPoolSize;

    @Autowired
    private ITaskFlowExecutor taskFlowExecutor;
    private Integer taskFlowExecutorCorePoolSize;
    private Integer taskFlowExecutorMaximumPoolSize;

    @Autowired
    private ITaskFlowStatusRecorder taskFlowStatusRecorder;
    @Autowired
    private ITaskStatusRecorder taskStatusRecorder;

    /**
     * 启动任务流调度器
     */
    public void startup() {
        if (this.started) {
            return;
        }
        log.info("任务流调度器启动，扫描间隔：{}秒，触发器核心线程数：{}，触发器最大线程数：{}，执行器核心线程数：{}，执行器最大线程数：{}",
                this.scanInterval,
                this.triggerCorePoolSize, this.triggerMaximumPoolSize,
                this.taskFlowExecutorCorePoolSize, this.taskFlowExecutorMaximumPoolSize);
        this.started = true;
        this.startScaner();
    }

    /**
     * 启动任务流扫描器
     */
    private void startScaner() {
        ThreadFactory scanerThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("taskFlowScaner-pool-%d").build();
        ExecutorService scanerThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), scanerThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        log.info("任务流扫描线程启动");
        scanerThreadPool.execute(() -> {
            while (true) {
                try {
                    Thread.sleep(this.scanInterval * 1000);

                    // 任务流扫描前尝试获取锁
                    boolean res = this.taskFlowScaner.tryLock();
                    if (res) {
                        log.info("任务流扫描线程获取锁成功");
                        // 获取锁成功
                        TryLockSuccessEvent tryLockSuccessEvent = new TryLockSuccessEvent(this);
                        this.eventPublisher.publishEvent(tryLockSuccessEvent);

                        String jobGroup = "DefaultJobGroup";

                        // 任务流扫描前
                        BeforeScaningEvent beforeScaningEvent = new BeforeScaningEvent(this);
                        this.eventPublisher.publishEvent(beforeScaningEvent);

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
                                    TaskFlowJoinScheduleEvent taskFlowJoinScheduleEvent = new TaskFlowJoinScheduleEvent(this, taskFlow);
                                    this.eventPublisher.publishEvent(taskFlowJoinScheduleEvent);
                                } else {
                                    MyDynamicScheduler.updateJobCron(name, jobGroup, cronExpression);
                                    // 任务流更新调度
                                    TaskFlowUpdateScheduleEvent taskFlowUpdateScheduleEvent = new TaskFlowUpdateScheduleEvent(this, taskFlow);
                                    this.eventPublisher.publishEvent(taskFlowUpdateScheduleEvent);
                                }
                            } catch (Exception e) {
                                log.error("任务流调度失败，任务流ID：" + taskFlow.getId() + "，失败原因：" + e.getMessage());
                                // 任务流调度失败
                                TaskFlowScheduleFailEvent taskFlowScheduleFailEvent = new TaskFlowScheduleFailEvent(this, taskFlow);
                                this.eventPublisher.publishEvent(taskFlowScheduleFailEvent);
                            }
                        }

                        // 任务流扫描后
                        AfterScaningEvent afterScaningEvent = new AfterScaningEvent(this);
                        this.eventPublisher.publishEvent(afterScaningEvent);
                    } else {
                        log.info("任务流调度线程获取锁失败");
                        MyDynamicScheduler.clear();
                        // 获取锁失败
                        TryLockFailEvent tryLockFailEvent = new TryLockFailEvent(this);
                        this.eventPublisher.publishEvent(tryLockFailEvent);
                    }
                } catch (Exception e) {
                    log.error("任务流调度异常！" + e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 触发任务流
     *
     * @param taskFlowId
     * @param params
     */
    public void trigger(Long taskFlowId, Map<String, String> params) {
        this.trigger(taskFlowId, null, null, params);
    }

    /**
     * 触发任务流，从指定任务开始
     *
     * @param taskFlowId
     * @param fromTaskId
     * @param params
     */
    public void triggerFrom(Long taskFlowId, Long fromTaskId, Map<String, String> params) {
        this.trigger(taskFlowId, fromTaskId, null, params);
    }

    /**
     * 触发任务流，到指定任务结束
     *
     * @param taskFlowId
     * @param toTaskId
     * @param params
     */
    public void triggerTo(Long taskFlowId, Long toTaskId, Map<String, String> params) {
        this.trigger(taskFlowId, null, toTaskId, params);
    }

    /**
     * 触发任务流
     *
     * @param taskFlowId
     * @param fromTaskId
     * @param toTaskId
     * @param params
     */
    private void trigger(Long taskFlowId, Long fromTaskId, Long toTaskId, Map<String, String> params) {
        if (!this.started) {
            throw new SchedulerNotStartedException("请先启动调度器！");
        }
        if (this.triggerThreadPool == null) {
            synchronized (this.triggerLock) {
                if (this.triggerThreadPool == null) {
                    // 初始化线程池
                    ThreadFactory triggerThreadFactory = new ThreadFactoryBuilder().setNameFormat("triggerExecutor-pool-%d").build();
                    this.triggerThreadPool = new ThreadPoolExecutor(this.triggerCorePoolSize, this.triggerMaximumPoolSize, 60, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(1024), triggerThreadFactory, new ThreadPoolExecutor.AbortPolicy());
                }
            }
        }

        // 获取任务流
        TaskFlow taskFlow = this.taskFlowQuerier.getTaskFlow(taskFlowId);

        // 检查任务流是否是一个有向无环图
        List<Task> sortedTaskList = TaskFlowUtils.topologicalSort(taskFlow);
        if (sortedTaskList == null) {
            throw new InvalidTaskFlowException("任务流不是一个有向无环图，请检查是否存在回路！");
        }

        // 根据从指定任务开始或到指定任务结束，对任务流进行剪裁
        TaskFlow prunedTaskFlow = TaskFlowUtils.prune(taskFlow, fromTaskId, toTaskId);
        if (prunedTaskFlow.getTaskList().size() == 0) {
            return;
        }

        // 在到指定任务结束的情况下，已经执行成功的任务无需再执行，因此移除掉
        TaskFlow unsuccessTaskFlow = null;
        if (fromTaskId == null && toTaskId != null) {
            unsuccessTaskFlow = this.removeSuccessTask(prunedTaskFlow);
        }

        TaskFlow finalTaskFlow = (unsuccessTaskFlow == null ? prunedTaskFlow : unsuccessTaskFlow);

        Long taskFlowExecId = null;
        boolean isPartialExecute = (fromTaskId != null || toTaskId != null);
        if (isPartialExecute && !this.taskStatusRecorder.listByTaskFlowId(finalTaskFlow.getId()).isEmpty()) {
            // 任务流存在执行记录或任务流部分继续执行，则使用之前的任务流执行ID
            TaskFlowContext taskFlowContext = this.taskFlowStatusRecorder.get(finalTaskFlow.getId()).getTaskFlowContext();
            taskFlowExecId = Long.parseLong(taskFlowContext.get(ContextKey.TASK_FLOW_EXEC_ID));
        } else {
            // 任务流从未执行过或任务流全部重新执行，则生成新的任务流执行ID
            taskFlowExecId = SystemTime.getUniqueTime();
        }
        String taskFlowExecIdString = String.valueOf(taskFlowExecId);

        // 任务流执行
        this.triggerThreadPool.execute(() -> {
            TaskFlowContext taskFlowContext = new TaskFlowContext();
            if (params != null) {
                taskFlowContext.setParams(params);
            }
            taskFlowContext.put(ContextKey.FROM_TASK_ID, String.valueOf(fromTaskId));
            taskFlowContext.put(ContextKey.TO_TASK_ID, String.valueOf(toTaskId));
            taskFlowContext.put(ContextKey.TASK_FLOW_EXEC_ID, taskFlowExecIdString);
            try {
                // 任务流等待执行
                this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode(), null);
                this.taskFlowExecutor.beforeExecute(finalTaskFlow, taskFlowContext);
                // 任务流执行中
                this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTING.getCode(), null);
                // 开始执行
                this.taskFlowExecutor.execute(finalTaskFlow, taskFlowContext, this.taskFlowExecutorCorePoolSize, this.taskFlowExecutorMaximumPoolSize);
                this.taskFlowExecutor.afterExecute(finalTaskFlow, taskFlowContext);
                // 任务流执行成功
                this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), null);
            } catch (Exception e) {
                log.error("任务流执行失败！任务流ID：" + finalTaskFlow.getId() + " 异常信息：" + e.getMessage(), e);
                // 任务流执行失败
                this.publishTaskFlowStatusChangeEvent(finalTaskFlow, taskFlowContext, TaskFlowStatusEnum.EXECUTE_FAILURE.getCode(), e.getMessage());
            }
        });
    }

    /**
     * 移除已经执行成功的任务
     *
     * @param taskFlow
     * @return
     */
    private TaskFlow removeSuccessTask(TaskFlow taskFlow) {
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
     */
    private void publishTaskFlowStatusChangeEvent(TaskFlow taskFlow, TaskFlowContext taskFlowContext, String status, String message) {
        TaskFlowStatus taskFlowStatus = new TaskFlowStatus();
        taskFlowStatus.setTaskFlow(taskFlow);
        taskFlowStatus.setTaskFlowContext(taskFlowContext);
        taskFlowStatus.setStatus(status);
        taskFlowStatus.setMessage(message);
        TaskFlowStatusChangeEvent taskFlowStatusChangeEvent = new TaskFlowStatusChangeEvent(this, taskFlowStatus);
        this.eventPublisher.publishEvent(taskFlowStatusChangeEvent);
    }

    /**
     * 停止任务流
     *
     * @param taskFlowId
     * @throws Exception
     */
    public void stop(Long taskFlowId) throws Exception {
        this.taskFlowExecutor.stop(taskFlowId);
    }

}
