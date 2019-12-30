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

    private static final Logger log = LoggerFactory.getLogger(TaskFlowScheduler.class);
    private static boolean started = false;


    private static ITaskFlowScaner taskFlowScaner;
    private static Integer scanInterval;

    private static ITaskFlowExecutor taskFlowExecutor;
    private static Integer execCorePoolSize;
    private static Integer execMaximumPoolSize;
    private static ExecutorService execThreadPoolExecutor;
    private static final Object EXEC_LOCK = new Object();

    private static ITaskFlowMonitor taskFlowMonitor;
    private static Integer monitoringInterval;

    /**
     * 启动任务流调度器
     *
     * @param taskFlowScaner
     * @param scanInterval
     * @param taskFlowExecutor
     * @param taskFlowMonitor
     * @param monitoringInterval
     */
    public static void startup(ITaskFlowScaner taskFlowScaner, Integer scanInterval,
                               ITaskFlowExecutor taskFlowExecutor,
                               ITaskFlowMonitor taskFlowMonitor, Integer monitoringInterval) {
        if (started) {
            return;
        }
        started = true;
        taskFlowScaner = taskFlowScaner;
        scanInterval = scanInterval;
        taskFlowExecutor = taskFlowExecutor;
        taskFlowMonitor = taskFlowMonitor;
        monitoringInterval = monitoringInterval;
        startScaner();
        startMonitor();
    }

    /**
     * 启动任务流扫描器
     */
    private static void startScaner() {
        ThreadFactory scanerThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("taskFlowScaner-pool-%d").build();
        ExecutorService scanerThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), scanerThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        log.info("任务流扫描线程启动，扫描间隔:{}秒", scanInterval);
        scanerThreadPool.execute(() -> {
            while (true) {
                try {
                    Thread.sleep(scanInterval * 1000);

                    // 任务流扫描前尝试获取锁
                    boolean res = taskFlowScaner.tryLock();
                    if (res) {
                        log.info("任务流调度线程获取锁成功");
                        String jobGroup = "JobGroup";

                        // 任务流扫描前置处理
                        taskFlowScaner.beforeScanning();

                        // 任务流扫描
                        List<TaskFlow> taskFlowList = taskFlowScaner.scan();

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
                        taskFlowScaner.afterScanning();
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
    private static void startMonitor() {
        ThreadFactory monitorThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("taskFlowMonitor-pool-%d").build();
        ExecutorService monitorThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), monitorThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        log.info("任务流监视线程启动，监视间隔:{}秒", monitoringInterval);
        monitorThreadPool.execute(() -> {
            while (true) {
                try {
                    // 线程休眠
                    Thread.sleep(monitoringInterval * 1000);
                    // 获取监视中的任务流日志列表
                    List<TaskFlowLog> taskFlowLogList = taskFlowMonitor.listMonitoringTaskFlowLog();
                    for (TaskFlowLog taskFlowLog : taskFlowLogList) {
                        try {
                            // 监视前置处理
                            taskFlowMonitor.beforeMonitoring(taskFlowLog);
                            // 监视任务流变化
                            TaskFlowLog updatedTaskFlowLog = taskFlowMonitor.monitoring(taskFlowLog);
                            if (updatedTaskFlowLog != null) {
                                // 更新任务流日志
                                taskFlowMonitor.updateTaskFlowLog(updatedTaskFlowLog);
                                // 检查任务流状态是否有变化
                                String updatedTaskStatus = updatedTaskFlowLog.getStatus();
                                if (updatedTaskStatus != null && !taskFlowLog.getStatus().equals(updatedTaskStatus)) {
                                    if (TaskFlowStatusEnum.WAIT_FOR_TRIGGER.getCode().equals(updatedTaskStatus)) {
                                        taskFlowMonitor.whenWaitForTrigger(updatedTaskFlowLog);
                                    } else if (TaskFlowStatusEnum.TRIGGER_FAIL.getCode().equals(updatedTaskStatus)) {
                                        taskFlowMonitor.whenTriggerFail(updatedTaskFlowLog);
                                    } else if (TaskFlowStatusEnum.EXECUTING.getCode().equals(updatedTaskStatus)) {
                                        taskFlowMonitor.whenExecuting(updatedTaskFlowLog);
                                    } else if (TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode().equals(updatedTaskStatus)) {
                                        taskFlowMonitor.whenExecuteSuccess(updatedTaskFlowLog);
                                    } else if (TaskFlowStatusEnum.EXECUTE_FAIL.getCode().equals(updatedTaskStatus)) {
                                        taskFlowMonitor.whenExecuteFail(updatedTaskFlowLog);
                                    } else {
                                        taskFlowMonitor.whenInOtherStatus(updatedTaskFlowLog);
                                    }
                                }
                            }
                            // 监视后置处理
                            if (updatedTaskFlowLog != null) {
                                taskFlowMonitor.afterMonitoring(updatedTaskFlowLog);
                            } else {
                                taskFlowMonitor.afterMonitoring(taskFlowLog);
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
    public static Long execute(Long taskFlowId) {
        if (!started) {
            throw new SchedulerNotStartedException("请先启动调度器！");
        }
        if (execThreadPoolExecutor == null) {
            synchronized (EXEC_LOCK) {
                if (execThreadPoolExecutor == null) {
                    // 初始化线程池
                    ThreadFactory execThreadFactory = new ThreadFactoryBuilder().setNameFormat("taskFlowExecutor-pool-%d").build();
                    execThreadPoolExecutor = new ThreadPoolExecutor(execCorePoolSize, execMaximumPoolSize, 60, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>(1024), execThreadFactory, new ThreadPoolExecutor.AbortPolicy());
                }
            }
        }

        // 创建任务流上下文
        TaskFlowContext context = taskFlowExecutor.createContext(taskFlowId);

        // 新增任务流日志
        TaskFlowLog taskFlowLog = new TaskFlowLog();
        taskFlowLog.setTaskFlowId(taskFlowId);
        taskFlowLog.setStatus(TaskFlowStatusEnum.WAIT_FOR_TRIGGER.getCode());
        taskFlowLog.setContext(JSONObject.toJSONString(context));
        Date now = new Date();
        taskFlowLog.setCreationTime(now);
        taskFlowLog.setUpdateTime(now);
        Long taskFlowLogId = taskFlowExecutor.insertLog(taskFlowLog);

        // 任务流执行
        execThreadPoolExecutor.execute(() -> {
            taskFlowExecutor.beforeExecute(context);
            taskFlowExecutor.execute(context);
            taskFlowExecutor.afterExecute(context);
        });

        return taskFlowLogId;
    }

}
