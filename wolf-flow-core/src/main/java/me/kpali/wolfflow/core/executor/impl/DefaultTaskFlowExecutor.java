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
    public void beforeExecute(TaskFlow taskFlow, Map<String, Object> taskFlowContext) throws TaskFlowExecuteException {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(taskFlowContext);
        Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
        // 检查任务流是否是一个有向无环图
        List<Task> sortedTaskList = TaskFlowUtils.topologicalSort(taskFlow);
        if (sortedTaskList == null) {
            throw new TaskFlowExecuteException("任务流不是一个有向无环图，请检查是否存在回路！");
        }
        if (taskFlow.getTaskList().size() == 0) {
            return;
        }

        // 根据参数“从指定任务开始”和“到指定任务结束”，分析要执行的任务和受影响的任务
        Long fromTaskId = taskFlowContextWrapper.getValue(ContextKey.FROM_TASK_ID, Long.class);
        Long toTaskId = taskFlowContextWrapper.getValue(ContextKey.TO_TASK_ID, Long.class);
        TaskFlow executeTaskFlow;
        TaskFlow affectedTaskFlow;
        if (fromTaskId == null && toTaskId == null) {
            // 执行所有任务，执行任务 == 受影响任务
            executeTaskFlow = TaskFlowUtils.prune(taskFlow, null, null);
            affectedTaskFlow = executeTaskFlow;
        } else if (fromTaskId != null && fromTaskId.equals(toTaskId)) {
            // 执行指定任务，执行任务 != 受影响任务，受影响任务 = 指定任务的所有子孙节点
            executeTaskFlow = TaskFlowUtils.prune(taskFlow, fromTaskId, toTaskId);
            affectedTaskFlow = TaskFlowUtils.prune(taskFlow, fromTaskId, null);
        } else if (fromTaskId != null) {
            // 从指定任务开始，执行任务 == 受影响任务
            executeTaskFlow = TaskFlowUtils.prune(taskFlow, fromTaskId, toTaskId);
            affectedTaskFlow = executeTaskFlow;
        } else {
            // 到指定任务结束，执行任务 == 受影响任务，但需要排除已经执行成功的任务
            TaskFlow prunedTaskFlow = TaskFlowUtils.prune(taskFlow, fromTaskId, toTaskId);
            TaskFlow unsuccessfulTaskFlow = this.excludeSuccessfulTasks(prunedTaskFlow);
            executeTaskFlow = (unsuccessfulTaskFlow == null ? prunedTaskFlow : unsuccessfulTaskFlow);
            affectedTaskFlow = executeTaskFlow;
        }
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
                boolean isAffected = false;
                for (Task affectedTask : affectedTaskFlow.getTaskList()) {
                    if (task.getId().equals(affectedTask.getId())) {
                        isAffected = true;
                        break;
                    }
                }
                if (isAffected) {
                    // 对于本次执行受影响的任务，清除任务状态
                    this.taskLogger.deleteTaskStatus(task.getId());
                    // 初始化上下文
                    TaskContextWrapper taskContextWrapper = new TaskContextWrapper();
                    List<Long> parentTaskIdList = new ArrayList<>();
                    taskFlow.getLinkList().forEach(link -> {
                        if (link.getTarget().equals(task.getId())) {
                            parentTaskIdList.add(link.getSource());
                        }
                    });
                    taskContextWrapper.put(ContextKey.PARENT_TASK_ID_LIST, parentTaskIdList);
                    taskFlowContextWrapper.putTaskContext(task.getId().toString(), taskContextWrapper.getContext());
                } else {
                    // 对于本次执行不受影响的任务，如果已经执行过，则复制一份任务状态（日志），并导入任务上下文到本次任务流上下文
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
                    Map<String, Object> lastTaskFlowContext = taskLog.getTaskFlowContext();
                    if (lastTaskFlowContext == null) {
                        continue;
                    }
                    TaskFlowContextWrapper lastTaskFlowContextWrapper = new TaskFlowContextWrapper(lastTaskFlowContext);
                    if (lastTaskFlowContextWrapper.getTaskContexts() == null) {
                        continue;
                    }
                    Map<String, Object> lastTaskContext = lastTaskFlowContextWrapper.getTaskContext(task.getId().toString());
                    if (lastTaskContext == null) {
                        continue;
                    }
                    taskFlowContextWrapper.putTaskContext(task.getId().toString(), lastTaskContext);
                }
            }
        } finally {
            if (locked) {
                this.clusterController.unlock(ClusterConstants.TASK_LOG_LOCK);
            }
        }
    }

    @Override
    public void execute(TaskFlow taskFlow, Map<String, Object> taskFlowContext) throws TaskFlowExecuteException, TaskFlowInterruptedException {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(taskFlowContext);
        Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
        TaskFlow executeTaskFlow = taskFlowContextWrapper.getValue(ContextKey.EXECUTE_TASK_FLOW, TaskFlow.class);
        try {
            // 初始化线程池
            ThreadFactory executorThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("taskFlowExecutor-pool-%d").build();
            ExecutorService executorThreadPool = new ThreadPoolExecutor(this.executorConfig.getCorePoolSize(), this.executorConfig.getMaximumPoolSize(),
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(1024), executorThreadFactory, new ThreadPoolExecutor.AbortPolicy());
            // 任务表、父任务表和子任务表
            Map<Long, Task> idToTaskMap = new HashMap<>();
            Map<Long, List<Long>> idToParentTaskIdsMap = new HashMap<>();
            taskFlow.getTaskList().forEach(task -> {
                idToTaskMap.put(task.getId(), task);
                List<Long> parentTaskIds = new ArrayList<>();
                for (Link link : taskFlow.getLinkList()) {
                    if (link.getTarget().equals(task.getId())) {
                        parentTaskIds.add(link.getSource());
                    }
                }
                idToParentTaskIdsMap.put(task.getId(), parentTaskIds);
            });
            // 参与执行的父任务表和子任务表
            Map<Long, List<Long>> idToParentTaskIds4ExecMap = new HashMap<>();
            Map<Long, List<Long>> idToChildTaskIds4ExecMap = new HashMap<>();
            executeTaskFlow.getTaskList().forEach(task -> {
                List<Long> parentTaskIds4Exec = new ArrayList<>();
                List<Long> childTaskIds4Exec = new ArrayList<>();
                for (Link link : executeTaskFlow.getLinkList()) {
                    if (link.getTarget().equals(task.getId())) {
                        parentTaskIds4Exec.add(link.getSource());
                    }
                    if (link.getSource().equals(task.getId())) {
                        childTaskIds4Exec.add(link.getTarget());
                    }
                }
                idToParentTaskIds4ExecMap.put(task.getId(), parentTaskIds4Exec);
                idToChildTaskIds4ExecMap.put(task.getId(), childTaskIds4Exec);
            });
            // 任务入度表
            Map<Long, Integer> idToInDegree4ExecMap = new HashMap<>();
            executeTaskFlow.getTaskList().forEach(task -> {
                int inDegree = idToParentTaskIds4ExecMap.get(task.getId()).size();
                idToInDegree4ExecMap.put(task.getId(), inDegree);
            });
            // 从入度为0的任务开始执行
            Map<Long, String> idToTaskStatusMap = new ConcurrentHashMap<>();
            for (Long taskId : idToInDegree4ExecMap.keySet()) {
                int inDegree = idToInDegree4ExecMap.get(taskId);
                if (inDegree == 0) {
                    idToTaskStatusMap.put(taskId, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode());
                    Task task = idToTaskMap.get(taskId);
                    this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, true);
                }
            }
            boolean isSuccess = true;
            boolean requireToStop = false;
            while (!idToTaskStatusMap.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn(e.getMessage(), e);
                }
                requireToStop = this.clusterController.stopRequestContains(taskFlowLogId);
                for (Long taskId : idToTaskStatusMap.keySet()) {
                    String taskStatus = idToTaskStatusMap.get(taskId);
                    if (TaskStatusEnum.WAIT_FOR_EXECUTE.getCode().equals(taskStatus)) {
                        // 等待执行，将任务状态改为执行中，并将任务加入线程池
                        Task task = idToTaskMap.get(taskId);
                        idToTaskStatusMap.put(task.getId(), TaskStatusEnum.EXECUTING.getCode());
                        executorThreadPool.execute(() -> {
                            try {
                                // 检查父任务是否已经执行成功
                                List<Long> parentTaskIds = idToParentTaskIdsMap.get(task.getId());
                                for (Long parentTaskId : parentTaskIds) {
                                    TaskLog parentTaskStatus  = this.taskLogger.getTaskStatus(parentTaskId);
                                    if (parentTaskStatus == null || !TaskStatusEnum.EXECUTE_SUCCESS.getCode().equals(parentTaskStatus.getStatus())) {
                                        throw new TaskExecuteException("父任务必须先执行成功");
                                    }
                                }
                                task.beforeExecute(taskFlowContext);
                                this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.EXECUTING.getCode(), null, true);
                                task.execute(taskFlowContext);
                                task.afterExecute(taskFlowContext);
                                idToTaskStatusMap.put(task.getId(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
                                this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.EXECUTE_SUCCESS.getCode(), null, true);
                            } catch (TaskExecuteException | TaskInterruptedException e) {
                                log.error("任务执行失败！任务ID：" + task.getId() + " 异常信息：" + e.getMessage(), e);
                                idToTaskStatusMap.put(task.getId(), TaskStatusEnum.EXECUTE_FAILURE.getCode());
                                this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.EXECUTE_FAILURE.getCode(), e.getMessage(), true);
                            }
                        });
                    } else if (requireToStop && TaskStatusEnum.EXECUTING.getCode().equals(taskStatus)) {
                        Task task = idToTaskMap.get(taskId);
                        idToTaskStatusMap.put(task.getId(), TaskStatusEnum.STOPPING.getCode());
                        try {
                            this.publishTaskStatusChangeEvent(task, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.STOPPING.getCode(), null, true);
                            task.stop(taskFlowContext);
                        } catch (TaskStopException e) {
                            log.error("任务终止失败！任务ID：" + task.getId() + " 异常信息：" + e.getMessage(), e);
                        }
                    } else if (TaskStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskStatus)) {
                        // 执行成功，将子任务的入度减1，如果子任务入度为0，则将子任务状态设置为等待执行并加入状态检查，最后将此任务移除状态检查
                        List<Long> childTaskIds = idToChildTaskIds4ExecMap.get(taskId);
                        for (Long childTaskId : childTaskIds) {
                            int childTaskInDegree = idToInDegree4ExecMap.get(childTaskId);
                            childTaskInDegree--;
                            idToInDegree4ExecMap.put(childTaskId, childTaskInDegree);
                            if (childTaskInDegree == 0) {
                                idToTaskStatusMap.put(childTaskId, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode());
                                Task childTask = idToTaskMap.get(childTaskId);
                                this.publishTaskStatusChangeEvent(childTask, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, true);
                            }
                        }
                        idToTaskStatusMap.remove(taskId);
                    } else if (TaskStatusEnum.EXECUTE_FAILURE.getCode().equals(taskStatus) || TaskStatusEnum.SKIPPED.getCode().equals(taskStatus)) {
                        if (TaskStatusEnum.EXECUTE_FAILURE.getCode().equals(taskStatus)) {
                            isSuccess = false;
                        }
                        // 执行失败 或者 跳过，将子任务状态设置为跳过，并将此任务移除状态检查
                        List<Long> childTaskIds = idToChildTaskIds4ExecMap.get(taskId);
                        for (Long childTaskId : childTaskIds) {
                            idToTaskStatusMap.put(childTaskId, TaskStatusEnum.SKIPPED.getCode());
                            Task childTask = idToTaskMap.get(childTaskId);
                            this.publishTaskStatusChangeEvent(childTask, executeTaskFlow.getId(), taskFlowContext, TaskStatusEnum.SKIPPED.getCode(), null, true);
                        }
                        idToTaskStatusMap.remove(taskId);
                    }
                }
            }
            if (requireToStop) {
                throw new TaskFlowInterruptedException("任务流被终止执行");
            } else if (!isSuccess) {
                throw new TaskFlowExecuteException("一个或多个任务执行失败");
            }
        } finally {
            this.clusterController.stopRequestRemove(taskFlowLogId);
        }
    }

    @Override
    public void afterExecute(TaskFlow taskFlow, Map<String, Object> taskFlowContext) throws TaskFlowExecuteException {
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
    private void publishTaskStatusChangeEvent(Task task, Long taskFlowId, Map<String, Object> taskFlowContext, String status, String message, boolean record) {
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
                TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(taskFlowContext);
                Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
                TaskLog taskLog = this.taskLogger.get(taskFlowLogId, task.getId());
                boolean isNewLog = false;
                if (taskLog == null) {
                    isNewLog = true;
                    Long taskLogId = systemTimeUtils.getUniqueTimeStamp();
                    String logFileId = UUID.randomUUID().toString();
                    Map<String, Object> taskContext = taskFlowContextWrapper.getTaskContext(task.getId().toString());
                    taskContext.put(ContextKey.TASK_LOG_ID, taskLogId);
                    taskContext.put(ContextKey.TASK_LOG_FILE_ID, logFileId);
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
