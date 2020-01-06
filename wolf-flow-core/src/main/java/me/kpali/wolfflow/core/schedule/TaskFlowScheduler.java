package me.kpali.wolfflow.core.schedule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.event.*;
import me.kpali.wolfflow.core.exception.InvalidCronExpressionException;
import me.kpali.wolfflow.core.exception.SchedulerNotStartedException;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.model.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.quartz.MyDynamicScheduler;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Date;
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
                             Integer execCorePoolSize, Integer execMaximumPoolSize) {
        this.scanInterval = scanInterval;
        this.execCorePoolSize = execCorePoolSize;
        this.execMaximumPoolSize = execMaximumPoolSize;
    }

    private static final Logger log = LoggerFactory.getLogger(TaskFlowScheduler.class);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private boolean started = false;

    private ITaskFlowQuerier taskFlowQuerier;

    private ITaskFlowScaner taskFlowScaner;
    private Integer scanInterval;

    private final Object triggerLock = new Object();

    private ITaskFlowExecutor taskFlowExecutor;
    private Integer execCorePoolSize;
    private Integer execMaximumPoolSize;
    private ExecutorService execThreadPoolExecutor;

    private ITaskFlowLogger taskFlowLogger;

    public boolean isStarted() {
        return started;
    }

    public ITaskFlowQuerier getTaskFlowQuerier() {
        return taskFlowQuerier;
    }

    public ITaskFlowScaner getTaskFlowScaner() {
        return taskFlowScaner;
    }

    public Integer getScanInterval() {
        return scanInterval;
    }

    public ITaskFlowExecutor getTaskFlowExecutor() {
        return taskFlowExecutor;
    }

    public Integer getExecCorePoolSize() {
        return execCorePoolSize;
    }

    public Integer getExecMaximumPoolSize() {
        return execMaximumPoolSize;
    }

    public ExecutorService getExecThreadPoolExecutor() {
        return execThreadPoolExecutor;
    }

    public ITaskFlowLogger getTaskFlowLogger() {
        return taskFlowLogger;
    }

    /**
     * 启动任务流调度器
     *
     * @param taskFlowQuerier
     * @param taskFlowScaner
     * @param taskFlowExecutor
     * @param taskFlowLogger
     */
    public void startup(ITaskFlowQuerier taskFlowQuerier,
                        ITaskFlowScaner taskFlowScaner,
                        ITaskFlowExecutor taskFlowExecutor,
                        ITaskFlowLogger taskFlowLogger) {
        if (this.started) {
            return;
        }
        log.info("任务流调度器启动，扫描间隔：{}秒，执行核心线程数：{}，执行最大线程数：{}",
                this.scanInterval, this.execCorePoolSize, this.execMaximumPoolSize);
        this.started = true;
        this.taskFlowQuerier = taskFlowQuerier;
        this.taskFlowScaner = taskFlowScaner;
        this.taskFlowExecutor = taskFlowExecutor;
        this.taskFlowLogger = taskFlowLogger;
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

                        // 任务流扫描前置处理
                        this.taskFlowScaner.beforeScanning();

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

                        // 任务流扫描后置处理
                        this.taskFlowScaner.afterScanning();
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
     * @return taskFlowLogId 任务日志ID
     */
    public Long trigger(Long taskFlowId) {
        if (!this.started) {
            throw new SchedulerNotStartedException("请先启动调度器！");
        }
        if (this.execThreadPoolExecutor == null) {
            synchronized (this.triggerLock) {
                if (this.execThreadPoolExecutor == null) {
                    // 初始化线程池
                    ThreadFactory execThreadFactory = new ThreadFactoryBuilder().setNameFormat("taskFlowExecutor-pool-%d").build();
                    this.execThreadPoolExecutor = new ThreadPoolExecutor(this.execCorePoolSize, this.execMaximumPoolSize, 60, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(1024), execThreadFactory, new ThreadPoolExecutor.AbortPolicy());
                }
            }
        }

        // 获取任务流
        TaskFlow taskFlow = this.taskFlowQuerier.getTaskFlow(taskFlowId);

        // 新增任务流日志
        TaskFlowLog taskFlowLog = new TaskFlowLog();
        taskFlowLog.setTaskFlowId(taskFlowId);
        taskFlowLog.setStatus(TaskFlowStatusEnum.WAIT_FOR_EXECUTE.getCode());
        taskFlowLog.setContext(null);
        taskFlowLog.setDescription(null);
        Date now = new Date();
        taskFlowLog.setCreationTime(now);
        taskFlowLog.setUpdateTime(now);
        Long taskFlowLogId = this.taskFlowLogger.insert(taskFlowLog);
        // 任务流等待执行
        TaskFlowWaitForExecuteEvent taskFlowWaitForExecuteEvent = new TaskFlowWaitForExecuteEvent(this, taskFlow);
        this.eventPublisher.publishEvent(taskFlowWaitForExecuteEvent);

        // 任务流执行
        this.execThreadPoolExecutor.execute(() -> {
            try {
                // 更新任务流日志状态为执行中
                TaskFlowLog log = this.taskFlowLogger.select(taskFlowLogId);
                log.setStatus(TaskFlowStatusEnum.EXECUTING.getCode());
                log.setUpdateTime(new Date());
                this.taskFlowLogger.update(log);
                // 任务流执行中
                TaskFlowExecutingEvent taskFlowExecutingEvent = new TaskFlowExecutingEvent(this, taskFlow);
                this.eventPublisher.publishEvent(taskFlowExecutingEvent);
                // 开始执行
                TaskFlowContext context = this.taskFlowExecutor.initContext(taskFlow);
                this.taskFlowExecutor.beforeExecute(taskFlow, context);
                this.taskFlowExecutor.execute(taskFlow, context);
                this.taskFlowExecutor.afterExecute(taskFlow, context);
                // 任务流执行成功
                TaskFlowExecuteSuccessEvent taskFlowExecuteSuccessEvent = new TaskFlowExecuteSuccessEvent(this, taskFlow);
                this.eventPublisher.publishEvent(taskFlowExecuteSuccessEvent);
            } catch (Exception e) {
                log.error("任务流执行失败！任务流ID：" + taskFlowId + " 异常信息：" + e.getMessage(), e);
                try {
                    TaskFlowLog log = this.taskFlowLogger.select(taskFlowLogId);
                    log.setStatus(TaskFlowStatusEnum.EXECUTE_FAIL.getCode());
                    log.setDescription(e.getMessage());
                    log.setUpdateTime(new Date());
                    this.taskFlowLogger.update(log);
                } catch (Exception e1) {
                    log.error("任务流执行失败后更新日志失败！任务流ID：" + taskFlowId + " 异常信息：" + e1.getMessage(), e1);
                }
                // 任务流执行失败
                TaskFlowExecuteFailEvent taskFlowExecuteFailEvent = new TaskFlowExecuteFailEvent(this, taskFlow);
                this.eventPublisher.publishEvent(taskFlowExecuteFailEvent);
            }
        });

        return taskFlowLogId;
    }

}
