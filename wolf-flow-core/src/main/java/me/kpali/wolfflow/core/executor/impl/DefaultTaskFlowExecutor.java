package me.kpali.wolfflow.core.executor.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.ExecutorConfig;
import me.kpali.wolfflow.core.enums.TaskStatusEnum;
import me.kpali.wolfflow.core.event.TaskStatusChangeEvent;
import me.kpali.wolfflow.core.exception.*;
import me.kpali.wolfflow.core.executor.ITaskFlowExecutor;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.scheduler.impl.SystemTimeUtils;
import me.kpali.wolfflow.core.util.TaskFlowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * 任务流执行器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskFlowExecutor implements ITaskFlowExecutor {
    private static final Logger log = LoggerFactory.getLogger(DefaultTaskFlowExecutor.class);

    @Autowired
    private ExecutorConfig executorConfig;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private IClusterController clusterController;

    @Autowired
    private ITaskLogger taskLogger;

    @Autowired
    private SystemTimeUtils systemTimeUtils;

    @Override
    public void beforeExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) throws TaskFlowExecuteException {
        Long taskFlowLogId = taskFlowContext.getValue(ContextKey.LOG_ID, Long.class);
        // 检查任务流是否是一个有向无环图
        List<Task> sortedTaskList = TaskFlowUtils.topologicalSort(taskFlow);
        if (sortedTaskList == null) {
            throw new TaskFlowExecuteException("任务流不是一个有向无环图，请检查是否存在回路！");
        }
        if (taskFlow.getTaskList().size() == 0) {
            return;
        }
        if (taskFlowContext.getTaskContexts() == null) {
            taskFlowContext.setTaskContexts(new ConcurrentHashMap<>());
        }

        // 根据从指定任务开始或到指定任务结束，对任务流进行剪裁
        Long fromTaskId = taskFlowContext.getValue(ContextKey.FROM_TASK_ID, Long.class);
        Long toTaskId = taskFlowContext.getValue(ContextKey.TO_TASK_ID, Long.class);
        TaskFlow prunedTaskFlow = TaskFlowUtils.prune(taskFlow, fromTaskId, toTaskId);
        // 在到指定任务结束的情况下，已经执行成功的任务无需再执行，因此只保留未执行成功的任务
        TaskFlow unsuccessfulTaskFlow = null;
        if (fromTaskId == null && toTaskId != null) {
            unsuccessfulTaskFlow = this.excludeSuccessfulTasks(prunedTaskFlow);
        }
        TaskFlow executeTaskFlow = (unsuccessfulTaskFlow == null ? prunedTaskFlow : unsuccessfulTaskFlow);
        taskFlowContext.put(ContextKey.EXECUTE_TASK_FLOW, executeTaskFlow);

        boolean locked = false;
        try {
            locked = this.clusterController.tryLock(
                    ClusterConstants.TASK_LOG_LOCK,
                    ClusterConstants.LOG_LOCK_WAIT_TIME,
                    ClusterConstants.LOG_LOCK_LEASE_TIME,
                    TimeUnit.SECONDS);
            if (!locked) {
                throw new TryLockException("获取任务日志记录锁失败！");
            }
            for (Task task : taskFlow.getTaskList()) {
                boolean isExecute = false;
                for (Task executeTask : executeTaskFlow.getTaskList()) {
                    if (task.getId().equals(executeTask.getId())) {
                        isExecute = true;
                        break;
                    }
                }
                if (isExecute) {
                    // 本次执行的任务，清除任务状态
                    this.taskLogger.deleteTaskStatus(task.getId());
                    // 初始化上下文
                    TaskContext taskContext = new TaskContext();
                    List<Long> parentTaskIdList = new ArrayList<>();
                    taskFlow.getLinkList().forEach(link -> {
                        if (link.getTarget().equals(task.getId())) {
                            parentTaskIdList.add(link.getSource());
                        }
                    });
                    taskContext.put(ContextKey.PARENT_TASK_ID_LIST, parentTaskIdList);
                    taskFlowContext.getTaskContexts().put(task.getId(), taskContext);
                } else {
                    // 对于本次不执行的任务，如果已经执行过，则复制一份任务状态（日志），并导入任务上下文到本次任务流上下文
                    TaskLog taskLog = this.taskLogger.getTaskStatus(task.getId());
                    if (taskLog == null) {
                        continue;
                    }
                    // 任务状态（日志）
                    Long taskLogId = systemTimeUtils.getUniqueTimeStamp();
                    taskLog.setLogId(taskLogId);
                    taskLog.setTaskFlowLogId(taskFlowLogId);
                    this.taskLogger.add(taskLog);
                    // 任务上下文
                    TaskFlowContext lastTaskFlowContext = taskLog.getTaskFlowContext();
                    if (lastTaskFlowContext == null) {
                        continue;
                    }
                    if (lastTaskFlowContext.getTaskContexts() == null) {
                        continue;
                    }
                    TaskContext lastTaskContext = lastTaskFlowContext.getTaskContexts().get(task.getId());
                    if (lastTaskContext == null) {
                        continue;
                    }
                    taskFlowContext.getTaskContexts().put(task.getId(), lastTaskContext);
                }
            }
        } finally {
            if (locked) {
                this.clusterController.unlock(ClusterConstants.TASK_LOG_LOCK);
            }
        }
    }

    @Override
    public void execute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) throws TaskFlowExecuteException, TaskFlowInterruptedException {
        Long taskFlowLogId = taskFlowContext.getValue(ContextKey.LOG_ID, Long.class);
        TaskFlow executeTaskFlow = taskFlowContext.getValue(ContextKey.EXECUTE_TASK_FLOW, TaskFlow.class);
        try {
            // 初始化线程池
            ThreadFactory executorThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("taskFlowExecutor-pool-%d").build();
            ExecutorService executorThreadPool = new ThreadPoolExecutor(this.executorConfig.getCorePoolSize(), this.executorConfig.getMaximumPoolSize(),
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(1024), executorThreadFactory, new ThreadPoolExecutor.AbortPolicy());
            Map<Long, Task> idToTaskMap = new HashMap<>();
            taskFlow.getTaskList().forEach(task -> {
                idToTaskMap.put(task.getId(), task);
            });
            // 计算节点入度
            Map<Long, Integer> taskIdToInDegreeMap = new HashMap<>();
            executeTaskFlow.getTaskList().forEach(task -> {
                int inDegree = 0;
                for (Link link : executeTaskFlow.getLinkList()) {
                    if (link.getTarget().equals(task.getId())) {
                        inDegree++;
                    }
                }
                taskIdToInDegreeMap.put(task.getId(), inDegree);
            });
            // 从入度为0的节点开始执行
            Map<Long, String> taskIdToStatusMap = new ConcurrentHashMap<>();
            for (Long taskId : taskIdToInDegreeMap.keySet()) {
                int inDegree = taskIdToInDegreeMap.get(taskId);
                if (inDegree == 0) {
                    taskIdToStatusMap.put(taskId, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode());
                    Task task = idToTaskMap.get(taskId);
                    this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, true);
                }
            }
            boolean isSuccess = true;
            boolean requireToStop = false;
            while (!taskIdToStatusMap.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn(e.getMessage(), e);
                }
                requireToStop = this.clusterController.stopRequestContains(taskFlowLogId);
                for (Long taskId : taskIdToStatusMap.keySet()) {
                    String taskStatus = taskIdToStatusMap.get(taskId);
                    if (TaskStatusEnum.WAIT_FOR_EXECUTE.getCode().equals(taskStatus)) {
                        // 等待执行，将节点状态改为执行中，并将任务加入线程池
                        Task task = idToTaskMap.get(taskId);
                        taskIdToStatusMap.put(task.getId(), TaskStatusEnum.EXECUTING.getCode());
                        executorThreadPool.execute(() -> {
                            try {
                                task.beforeExecute(taskFlowContext);
                                this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.EXECUTING.getCode(), null, true);
                                task.execute(taskFlowContext);
                                task.afterExecute(taskFlowContext);
                                taskIdToStatusMap.put(task.getId(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
                                this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.EXECUTE_SUCCESS.getCode(), null, true);
                            } catch (TaskExecuteException | TaskInterruptedException e) {
                                log.error("任务执行失败！任务ID：" + task.getId() + " 异常信息：" + e.getMessage(), e);
                                taskIdToStatusMap.put(task.getId(), TaskStatusEnum.EXECUTE_FAILURE.getCode());
                                this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.EXECUTE_FAILURE.getCode(), e.getMessage(), true);
                            }
                        });
                    } else if (requireToStop && TaskStatusEnum.EXECUTING.getCode().equals(taskStatus)) {
                        Task task = idToTaskMap.get(taskId);
                        taskIdToStatusMap.put(task.getId(), TaskStatusEnum.STOPPING.getCode());
                        try {
                            this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.STOPPING.getCode(), null, true);
                            task.stop(taskFlowContext);
                        } catch (TaskStopException e) {
                            log.error("任务终止失败！任务ID：" + task.getId() + " 异常信息：" + e.getMessage(), e);
                        }
                    } else if (TaskStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskStatus)) {
                        // 执行成功，将子节点的入度减1，如果子节点入度为0，则将子节点状态设置为等待执行并加入状态检查，最后将此节点移除状态检查
                        for (Link link : executeTaskFlow.getLinkList()) {
                            if (link.getSource().equals(taskId)) {
                                Long childTaskId = link.getTarget();
                                int childTaskInDegree = taskIdToInDegreeMap.get(childTaskId);
                                childTaskInDegree--;
                                taskIdToInDegreeMap.put(childTaskId, childTaskInDegree);
                                if (childTaskInDegree == 0) {
                                    taskIdToStatusMap.put(childTaskId, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode());
                                    Task childTask = idToTaskMap.get(childTaskId);
                                    this.publishTaskStatusChangeEvent(childTask, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, true);
                                }
                            }
                        }
                        taskIdToStatusMap.remove(taskId);
                    } else if (TaskStatusEnum.EXECUTE_FAILURE.getCode().equals(taskStatus) || TaskStatusEnum.SKIPPED.getCode().equals(taskStatus)) {
                        if (TaskStatusEnum.EXECUTE_FAILURE.getCode().equals(taskStatus)) {
                            isSuccess = false;
                        }
                        // 执行失败 或者 跳过，将子节点状态设置为跳过，并将此节点移除状态检查
                        for (Link link : executeTaskFlow.getLinkList()) {
                            if (link.getSource().equals(taskId)) {
                                Long childTaskId = link.getTarget();
                                taskIdToStatusMap.put(childTaskId, TaskStatusEnum.SKIPPED.getCode());
                                Task childTask = idToTaskMap.get(childTaskId);
                                this.publishTaskStatusChangeEvent(childTask, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.SKIPPED.getCode(), null, true);
                            }
                        }
                        taskIdToStatusMap.remove(taskId);
                    }
                }
            }
            if (requireToStop) {
                throw new TaskFlowInterruptedException("任务流被终止执行");
            } else if (!isSuccess) {
                throw new TaskFlowExecuteException("至少有一个任务执行失败");
            }
        } finally {
            this.clusterController.stopRequestRemove(taskFlowLogId);
        }
    }

    @Override
    public void afterExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) throws TaskFlowExecuteException {
        // 不做任何操作
    }

    /**
     * 排除已执行成功的任务
     *
     * @param taskFlow
     * @return
     */
    private TaskFlow excludeSuccessfulTasks(TaskFlow taskFlow) {
        List<Long> successTaskIdList = new ArrayList<>();
        List<TaskLog> taskStatusList = null;
        boolean locked = false;
        try {
            locked = this.clusterController.tryLock(
                    ClusterConstants.TASK_LOG_LOCK,
                    ClusterConstants.LOG_LOCK_WAIT_TIME,
                    ClusterConstants.LOG_LOCK_LEASE_TIME,
                    TimeUnit.SECONDS);
            if (!locked) {
                throw new TryLockException("获取任务日志记录锁失败！");
            }
            taskStatusList = this.taskLogger.listTaskStatus(taskFlow.getId());
        } finally {
            if (locked) {
                this.clusterController.unlock(ClusterConstants.TASK_LOG_LOCK);
            }
        }
        if (taskStatusList == null || taskStatusList.isEmpty()) {
            return taskFlow;
        }

        for (TaskLog taskStatus : taskStatusList) {
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
     * 发布任务状态变更事件
     *
     * @param task
     * @param taskFlowId
     * @param taskFlowContext
     * @param status
     * @param message
     * @param record
     */
    private void publishTaskStatusChangeEvent(Task task, Long taskFlowId, TaskFlowContext taskFlowContext, String status, String message, boolean record) {
        TaskStatus taskStatus = new TaskStatus();
        taskStatus.setTask(task);
        taskStatus.setTaskFlowId(taskFlowId);
        taskStatus.setTaskFlowContext(taskFlowContext);
        taskStatus.setStatus(status);
        taskStatus.setMessage(message);
        if (record) {
            boolean locked = false;
            try {
                locked = this.clusterController.tryLock(
                        ClusterConstants.TASK_LOG_LOCK,
                        ClusterConstants.LOG_LOCK_WAIT_TIME,
                        ClusterConstants.LOG_LOCK_LEASE_TIME,
                        TimeUnit.SECONDS);
                if (!locked) {
                    throw new TryLockException("获取任务日志记录锁失败！");
                }
                Long taskFlowLogId = taskFlowContext.getValue(ContextKey.LOG_ID, Long.class);
                TaskLog taskLog = this.taskLogger.get(taskFlowLogId, task.getId());
                boolean isNewLog = false;
                if (taskLog == null) {
                    isNewLog = true;
                    Long taskLogId = systemTimeUtils.getUniqueTimeStamp();
                    String logFileId = UUID.randomUUID().toString();
                    TaskContext taskContext = taskFlowContext.getTaskContexts().get(task.getId());
                    taskContext.put(ContextKey.LOG_ID, taskLogId);
                    taskContext.put(ContextKey.LOG_FILE_ID, logFileId);
                    taskLog = new TaskLog();
                    taskLog.setLogId(taskLogId);
                    taskLog.setTaskFlowLogId(taskFlowLogId);
                    taskLog.setTaskId(task.getId());
                    taskLog.setLogFileId(logFileId);
                }
                taskLog.setTask(task);
                taskLog.setTaskFlowId(taskFlowId);
                taskLog.setTaskFlowContext(taskFlowContext);
                taskLog.setStatus(status);
                taskLog.setMessage(message);
                if (isNewLog) {
                    this.taskLogger.add(taskLog);
                } else {
                    this.taskLogger.update(taskLog);
                }
            } finally {
                if (locked) {
                    this.clusterController.unlock(ClusterConstants.TASK_LOG_LOCK);
                }
            }
        }
        TaskStatusChangeEvent taskStatusChangeEvent = new TaskStatusChangeEvent(this, taskStatus);
        this.eventPublisher.publishEvent(taskStatusChangeEvent);
    }
}
