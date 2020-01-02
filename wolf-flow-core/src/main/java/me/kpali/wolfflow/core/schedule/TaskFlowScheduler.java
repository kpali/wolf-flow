package me.kpali.wolfflow.core.schedule;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
                             Integer execCorePoolSize, Integer execMaximumPoolSize,
                             Integer monitoringInterval) {
        this.scanInterval = scanInterval;
        this.execCorePoolSize = execCorePoolSize;
        this.execMaximumPoolSize = execMaximumPoolSize;
        this.monitoringInterval = monitoringInterval;
    }

    private static final Logger log = LoggerFactory.getLogger(TaskFlowScheduler.class);
    private boolean started = false;

    private ITaskFlowScaner taskFlowScaner;
    private Integer scanInterval;

    private ITaskFlowExecutor taskFlowExecutor;
    private Integer execCorePoolSize;
    private Integer execMaximumPoolSize;
    private ExecutorService execThreadPoolExecutor;
    private Object execLock = new Object();

    private ITaskFlowMonitor taskFlowMonitor;
    private Integer monitoringInterval;

    private ITaskFlowLogger taskFlowLogger;

    public boolean isStarted() {
        return started;
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

    public ITaskFlowMonitor getTaskFlowMonitor() {
        return taskFlowMonitor;
    }

    public Integer getMonitoringInterval() {
        return monitoringInterval;
    }

    public ITaskFlowLogger getTaskFlowLogger() {
        return taskFlowLogger;
    }

    /**
     * 启动任务流调度器
     *
     * @param taskFlowScaner
     * @param taskFlowExecutor
     * @param taskFlowMonitor
     * @param taskFlowLogger
     */
    public void startup(ITaskFlowScaner taskFlowScaner,
                        ITaskFlowExecutor taskFlowExecutor,
                        ITaskFlowMonitor taskFlowMonitor,
                        ITaskFlowLogger taskFlowLogger) {
        if (this.started) {
            return;
        }
        log.info("任务流调度器启动，扫描间隔：{}秒，执行核心线程数：{}，执行最大线程数：{}，监视间隔：{}秒",
                this.scanInterval, this.execCorePoolSize, this.execMaximumPoolSize, this.monitoringInterval);
        this.started = true;
        this.taskFlowScaner = taskFlowScaner;
        this.taskFlowExecutor = taskFlowExecutor;
        this.taskFlowMonitor = taskFlowMonitor;
        this.taskFlowLogger = taskFlowLogger;
        this.startScaner();
        this.startMonitor();
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
                        log.info("任务流调度线程获取锁成功");
                        String jobGroup = "DefaultJobGroup";

                        // 任务流扫描前置处理
                        this.taskFlowScaner.beforeScanning();

                        // 任务流扫描
                        List<TaskFlow> scannedTaskFlowList = this.taskFlowScaner.scan();
                        List<TaskFlow> taskFlowList = (scannedTaskFlowList == null ? new ArrayList<>() : scannedTaskFlowList);
                        log.info("共扫描到{}个任务流", taskFlowList.size());

                        // 删除无需调度的任务流
                        List<JobKey> removedJobKeyList = new ArrayList<>();
                        Set<JobKey> jobKeySet = MyDynamicScheduler.getJobKeysGroupEquals(jobGroup);
                        for (JobKey jobKey : jobKeySet) {
                            boolean isFound = false;
                            for (TaskFlow taskFlow : taskFlowList) {
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
                        for (TaskFlow taskFlow : taskFlowList) {
                            try {
                                String name = String.valueOf(taskFlow.getId());
                                String cronExpression = taskFlow.getCron();
                                if (cronExpression == null || cronExpression.length() == 0) {
                                    throw new InvalidCronExpressionException("cron表达式不能为空");
                                }
                                if (!MyDynamicScheduler.checkExists(name, jobGroup)) {
                                    MyDynamicScheduler.addJob(name, jobGroup, cronExpression);
                                } else {
                                    MyDynamicScheduler.updateJobCron(name, jobGroup, cronExpression);
                                }
                            } catch (Exception e) {
                                log.error("任务流调度失败，任务流ID：" + taskFlow.getId() + "，失败原因：" + e.getMessage());
                            }
                        }

                        // 任务流扫描后置处理
                        this.taskFlowScaner.afterScanning();
                    } else {
                        log.info("任务流调度线程获取锁失败");
                        MyDynamicScheduler.clear();
                    }
                } catch (Exception e) {
                    log.error("任务流调度异常！" + e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 启动任务流监视器
     */
    private void startMonitor() {
        ThreadFactory monitorThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("taskFlowMonitor-pool-%d").build();
        ExecutorService monitorThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), monitorThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        log.info("任务流监视线程启动");
        monitorThreadPool.execute(() -> {
            while (true) {
                try {
                    // 线程休眠
                    Thread.sleep(this.monitoringInterval * 1000);
                    // 获取未完成的任务流日志列表
                    List<TaskFlowLog> unfinishedTaskFlowLogList = this.taskFlowLogger.listUnfinishedLog();
                    List<TaskFlowLog> taskFlowLogList = (unfinishedTaskFlowLogList == null ? new ArrayList<>() : unfinishedTaskFlowLogList);
                    log.info("当前共有{}个未完成的任务流", taskFlowLogList.size());
                    for (TaskFlowLog taskFlowLog : taskFlowLogList) {
                        try {
                            // 监视前置处理
                            this.taskFlowMonitor.beforeMonitoring(taskFlowLog);
                            // 监视任务流变化
                            TaskFlowLog updatedTaskFlowLog = this.taskFlowMonitor.monitoring(taskFlowLog);
                            if (updatedTaskFlowLog != null) {
                                // 更新任务流日志
                                this.taskFlowLogger.update(updatedTaskFlowLog);
                                // 检查任务流状态是否有变化
                                String updatedTaskStatus = updatedTaskFlowLog.getStatus();
                                if (updatedTaskStatus != null && !taskFlowLog.getStatus().equals(updatedTaskStatus)) {
                                    if (TaskFlowStatusEnum.WAIT_FOR_TRIGGER.getCode().equals(updatedTaskStatus)) {
                                        this.taskFlowMonitor.whenWaitForTrigger(updatedTaskFlowLog);
                                    } else if (TaskFlowStatusEnum.TRIGGER_FAIL.getCode().equals(updatedTaskStatus)) {
                                        this.taskFlowMonitor.whenTriggerFail(updatedTaskFlowLog);
                                    } else if (TaskFlowStatusEnum.EXECUTING.getCode().equals(updatedTaskStatus)) {
                                        this.taskFlowMonitor.whenExecuting(updatedTaskFlowLog);
                                    } else if (TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode().equals(updatedTaskStatus)) {
                                        this.taskFlowMonitor.whenExecuteSuccess(updatedTaskFlowLog);
                                    } else if (TaskFlowStatusEnum.EXECUTE_FAIL.getCode().equals(updatedTaskStatus)) {
                                        this.taskFlowMonitor.whenExecuteFail(updatedTaskFlowLog);
                                    } else {
                                        this.taskFlowMonitor.whenInOtherStatus(updatedTaskFlowLog);
                                    }
                                }
                            }
                            // 监视后置处理
                            if (updatedTaskFlowLog != null) {
                                this.taskFlowMonitor.afterMonitoring(updatedTaskFlowLog);
                            } else {
                                this.taskFlowMonitor.afterMonitoring(taskFlowLog);
                            }
                        } catch (Exception e) {
                            log.error("监视任务流时异常！任务流日志ID：" + taskFlowLog.getId() + " 异常消息：" + e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    log.error("任务流监视异常！" + e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 执行任务流
     *
     * @param taskFlowId
     * @return taskFlowLogId 任务日志ID
     */
    public Long execute(Long taskFlowId) {
        if (!this.started) {
            throw new SchedulerNotStartedException("请先启动调度器！");
        }
        if (this.execThreadPoolExecutor == null) {
            synchronized (this.execLock) {
                if (this.execThreadPoolExecutor == null) {
                    // 初始化线程池
                    ThreadFactory execThreadFactory = new ThreadFactoryBuilder().setNameFormat("taskFlowExecutor-pool-%d").build();
                    this.execThreadPoolExecutor = new ThreadPoolExecutor(this.execCorePoolSize, this.execMaximumPoolSize, 60, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(1024), execThreadFactory, new ThreadPoolExecutor.AbortPolicy());
                }
            }
        }

        // 创建任务流上下文
        TaskFlowContext context = this.taskFlowExecutor.createContext(taskFlowId);

        // 新增任务流日志
        TaskFlowLog taskFlowLog = new TaskFlowLog();
        taskFlowLog.setTaskFlowId(taskFlowId);
        taskFlowLog.setStatus(TaskFlowStatusEnum.WAIT_FOR_TRIGGER.getCode());
        taskFlowLog.setContext(JSONObject.toJSONString(context));
        Date now = new Date();
        taskFlowLog.setCreationTime(now);
        taskFlowLog.setUpdateTime(now);
        Long taskFlowLogId = this.taskFlowLogger.insert(taskFlowLog);

        // 任务流执行
        this.execThreadPoolExecutor.execute(() -> {
            this.taskFlowExecutor.beforeExecute(context);
            this.taskFlowExecutor.execute(context);
            this.taskFlowExecutor.afterExecute(context);
        });

        return taskFlowLogId;
    }

}
