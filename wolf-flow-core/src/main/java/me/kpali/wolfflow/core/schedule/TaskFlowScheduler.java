package me.kpali.wolfflow.core.schedule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.event.*;
import me.kpali.wolfflow.core.exception.InvalidCronExpressionException;
import me.kpali.wolfflow.core.exception.SchedulerNotStartedException;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.core.model.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.quartz.MyDynamicScheduler;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
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

    private ITaskFlowQuerier taskFlowQuerier;

    private ITaskFlowScaner taskFlowScaner;
    private Integer scanInterval;

    private final Object triggerLock = new Object();
    private ExecutorService triggerThreadPool;
    private Integer triggerCorePoolSize;
    private Integer triggerMaximumPoolSize;

    private ITaskFlowExecutor taskFlowExecutor;
    private Integer taskFlowExecutorCorePoolSize;
    private Integer taskFlowExecutorMaximumPoolSize;

    /**
     * 启动任务流调度器
     *
     * @param taskFlowQuerier
     * @param taskFlowScaner
     * @param taskFlowExecutor
     */
    public void startup(ITaskFlowQuerier taskFlowQuerier,
                        ITaskFlowScaner taskFlowScaner,
                        ITaskFlowExecutor taskFlowExecutor) {
        if (this.started) {
            return;
        }
        log.info("任务流调度器启动，扫描间隔：{}秒，执行核心线程数：{}，执行最大线程数：{}",
                this.scanInterval, this.triggerCorePoolSize, this.triggerMaximumPoolSize);
        this.started = true;
        this.taskFlowQuerier = taskFlowQuerier;
        this.taskFlowScaner = taskFlowScaner;
        this.taskFlowExecutor = taskFlowExecutor;
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
     */
    public void trigger(Long taskFlowId) {
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

        // 任务流等待执行
        TaskFlowStatusChangeEvent taskFlowWaitForExecuteEvent = new TaskFlowStatusChangeEvent(this, taskFlow, TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode());
        this.eventPublisher.publishEvent(taskFlowWaitForExecuteEvent);

        // 任务流执行
        this.triggerThreadPool.execute(() -> {
            try {
                // 任务流执行中
                TaskFlowStatusChangeEvent taskFlowExecutingEvent = new TaskFlowStatusChangeEvent(this, taskFlow, TaskFlowStatusEnum.EXECUTING.getCode());
                this.eventPublisher.publishEvent(taskFlowExecutingEvent);
                // 开始执行
                TaskFlowContext context = this.taskFlowExecutor.initContext(taskFlow);
                this.taskFlowExecutor.beforeExecute(taskFlow, context);
                this.taskFlowExecutor.execute(taskFlow, context, this.taskFlowExecutorCorePoolSize, this.taskFlowExecutorMaximumPoolSize);
                this.taskFlowExecutor.afterExecute(taskFlow, context);
                // 任务流执行成功
                TaskFlowStatusChangeEvent taskFlowExecuteSuccessEvent = new TaskFlowStatusChangeEvent(this, taskFlow, TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());
                this.eventPublisher.publishEvent(taskFlowExecuteSuccessEvent);
            } catch (Exception e) {
                log.error("任务流执行失败！任务流ID：" + taskFlowId + " 异常信息：" + e.getMessage(), e);
                // 任务流执行失败
                TaskFlowStatusChangeEvent taskFlowExecuteFailEvent = new TaskFlowStatusChangeEvent(this, taskFlow, TaskFlowStatusEnum.EXECUTE_FAIL.getCode());
                this.eventPublisher.publishEvent(taskFlowExecuteFailEvent);
            }
        });
    }

}
