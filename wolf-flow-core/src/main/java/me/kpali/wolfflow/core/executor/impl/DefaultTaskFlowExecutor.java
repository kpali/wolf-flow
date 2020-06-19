package me.kpali.wolfflow.core.executor.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.ExecutorConfig;
import me.kpali.wolfflow.core.enums.TaskStatusEnum;
import me.kpali.wolfflow.core.event.TaskStatusEventPublisher;
import me.kpali.wolfflow.core.exception.*;
import me.kpali.wolfflow.core.executor.ITaskFlowExecutor;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.util.IdGenerator;
import me.kpali.wolfflow.core.util.TaskFlowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private TaskStatusEventPublisher taskStatusEventPublisher;

    @Autowired
    private IClusterController clusterController;

    @Autowired
    private ITaskLogger taskLogger;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    public void beforeExecute(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowExecuteException {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
        Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
        try {
            this.checkTaskFlow(taskFlow);

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
            taskFlowContextWrapper.put(ContextKey.EXECUTE_TASK_FLOW, executeTaskFlow);

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
                        Long taskLogId = idGenerator.nextId();
                        taskContextWrapper.put(ContextKey.TASK_LOG_ID, taskLogId);
                        String logFileId = UUID.randomUUID().toString();
                        taskContextWrapper.put(ContextKey.TASK_LOG_FILE_ID, logFileId);
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
                        Long taskLogId = idGenerator.nextId();
                        taskLog.setLogId(taskLogId);
                        taskLog.setTaskFlowLogId(taskFlowLogId);
                        this.taskLogger.add(taskLog);
                        // 任务上下文
                        ConcurrentHashMap<String, Object> lastTaskContext = taskLog.getContext();
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

            // 预检查要执行的任务
            for (Task task : executeTaskFlow.getTaskList()) {
                task.executePreCheck(context);
            }
        } catch (Exception e) {
            throw new TaskFlowExecuteException(e);
        }
    }

    @Override
    public void execute(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowExecuteException, TaskFlowInterruptedException {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
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
                    this.taskStatusEventPublisher.publishEvent(task, executeTaskFlow.getId(), context, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, true);
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
                        // 等待执行，自动任务状态改为执行中，手工任务状态改为手工确认，并将任务加入线程池
                        Task task = idToTaskMap.get(taskId);
                        String statusCode = task.getManual() ? TaskStatusEnum.MANUAL_CONFIRM.getCode() : TaskStatusEnum.EXECUTING.getCode();
                        idToTaskStatusMap.put(task.getId(), statusCode);
                        executorThreadPool.execute(() -> {
                            try {
                                // 检查父任务是否已经执行成功
                                List<Long> parentTaskIds = idToParentTaskIdsMap.get(task.getId());
                                for (Long parentTaskId : parentTaskIds) {
                                    TaskLog parentTaskStatus = this.taskLogger.getTaskStatus(parentTaskId);
                                    if (parentTaskStatus == null || !TaskStatusEnum.EXECUTE_SUCCESS.getCode().equals(parentTaskStatus.getStatus())) {
                                        throw new TaskExecuteException("父任务必须先执行成功");
                                    }
                                }
                                this.taskStatusEventPublisher.publishEvent(task, executeTaskFlow.getId(), context, statusCode, null, true);
                                task.beforeExecute(context);
                                task.execute(context);
                                task.afterExecute(context);
                                idToTaskStatusMap.put(task.getId(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
                                this.taskStatusEventPublisher.publishEvent(task, executeTaskFlow.getId(), context, TaskStatusEnum.EXECUTE_SUCCESS.getCode(), null, true);
                            } catch (Exception e) {
                                log.error("任务执行失败！任务ID：" + task.getId() + " 异常信息：" + e.getMessage(), e);
                                idToTaskStatusMap.put(task.getId(), TaskStatusEnum.EXECUTE_FAILURE.getCode());
                                try {
                                    this.taskStatusEventPublisher.publishEvent(task, executeTaskFlow.getId(), context, TaskStatusEnum.EXECUTE_FAILURE.getCode(), e.getMessage(), true);
                                } catch (Exception e1) {
                                    log.error("发布任务状态变更事件失败！" + e.getMessage(), e);
                                }
                            }
                        });
                    } else if (requireToStop &&
                            (TaskStatusEnum.EXECUTING.getCode().equals(taskStatus) || TaskStatusEnum.MANUAL_CONFIRM.getCode().equals(taskStatus))) {
                        Task task = idToTaskMap.get(taskId);
                        idToTaskStatusMap.put(task.getId(), TaskStatusEnum.STOPPING.getCode());
                        try {
                            this.taskStatusEventPublisher.publishEvent(task, executeTaskFlow.getId(), context, TaskStatusEnum.STOPPING.getCode(), null, true);
                            task.stop(context);
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
                                this.taskStatusEventPublisher.publishEvent(childTask, executeTaskFlow.getId(), context, TaskStatusEnum.WAIT_FOR_EXECUTE.getCode(), null, true);
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
                            this.taskStatusEventPublisher.publishEvent(childTask, executeTaskFlow.getId(), context, TaskStatusEnum.SKIPPED.getCode(), null, true);
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
        } catch (TaskFlowExecuteException | TaskFlowInterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new TaskFlowExecuteException(e);
        } finally {
            this.clusterController.stopRequestRemove(taskFlowLogId);
        }
    }

    @Override
    public void afterExecute(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowExecuteException {
        // 不做任何操作
    }

    @Override
    public void beforeRollback(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowRollbackException {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
        Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
        try {
            this.checkTaskFlow(taskFlow);

            // 要回滚的任务 = 执行过的任务，但需要排除已经回滚的任务
            TaskFlow rollbackTaskFlow = this.selectRollbackTasks(taskFlow);
            // 反转任务流方向
            TaskFlow reversedTaskFlow = this.reverseTaskFlow(rollbackTaskFlow);
            taskFlowContextWrapper.put(ContextKey.ROLLBACK_TASK_FLOW, reversedTaskFlow);

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
                // 复制一份回滚前的任务状态（日志）
                Date now = new Date();
                for (Task task : taskFlow.getTaskList()) {
                    boolean needRollback = false;
                    for (Task rollbackTask : rollbackTaskFlow.getTaskList()) {
                        if (task.getId().equals(rollbackTask.getId())) {
                            needRollback = true;
                            break;
                        }
                    }
                    TaskLog taskLog = this.taskLogger.getTaskStatus(task.getId());
                    if (taskLog == null) {
                        continue;
                    }
                    // 任务状态（日志）
                    Long taskLogId = idGenerator.nextId();
                    taskLog.setLogId(taskLogId);
                    taskLog.setTaskFlowLogId(taskFlowLogId);
                    taskLog.setRollback(true);
                    taskLog.setCreationTime(now);
                    taskLog.setUpdateTime(now);
                    this.taskLogger.add(taskLog);
                    if (needRollback) {
                        // 要回滚的任务，初始化上下文
                        TaskContextWrapper taskContextWrapper = new TaskContextWrapper();
                        taskContextWrapper.put(ContextKey.TASK_LOG_ID, taskLogId);
                        String logFileId = UUID.randomUUID().toString();
                        taskContextWrapper.put(ContextKey.TASK_LOG_FILE_ID, logFileId);
                        List<Long> parentTaskIdList = new ArrayList<>();
                        taskFlow.getLinkList().forEach(link -> {
                            if (link.getTarget().equals(task.getId())) {
                                parentTaskIdList.add(link.getSource());
                            }
                        });
                        taskContextWrapper.put(ContextKey.PARENT_TASK_ID_LIST, parentTaskIdList);
                        TaskLog lastExecuteLog = this.taskLogger.getLastExecuteLog(task.getId());
                        if (lastExecuteLog != null) {
                            TaskContextWrapper lastTaskContextWrapper = new TaskContextWrapper(lastExecuteLog.getContext());
                            Long realLastExecuteLogId = lastTaskContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
                            if (realLastExecuteLogId != null) {
                                taskContextWrapper.put(ContextKey.TASK_LAST_EXECUTE_LOG_ID, realLastExecuteLogId);
                            }
                        }
                        taskFlowContextWrapper.putTaskContext(task.getId().toString(), taskContextWrapper.getContext());
                    } else {
                        // 不需要回滚的任务，导入任务上下文到本次任务流上下文
                        ConcurrentHashMap<String, Object> lastTaskContext = taskLog.getContext();
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

            // 预检查要回滚的任务
            for (Task task : rollbackTaskFlow.getTaskList()) {
                task.rollbackPreCheck(context);
            }
        } catch (Exception e) {
            throw new TaskFlowRollbackException(e);
        }
    }

    @Override
    public void rollback(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowRollbackException, TaskFlowInterruptedException {
        TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
        Long taskFlowLogId = taskFlowContextWrapper.getValue(ContextKey.LOG_ID, Long.class);
        TaskFlow rollbackTaskFlow = taskFlowContextWrapper.getValue(ContextKey.ROLLBACK_TASK_FLOW, TaskFlow.class);
        try {
            // 初始化线程池
            ThreadFactory executorThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("taskFlowExecutor-pool-%d").build();
            ExecutorService executorThreadPool = new ThreadPoolExecutor(this.executorConfig.getCorePoolSize(), this.executorConfig.getMaximumPoolSize(),
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(1024), executorThreadFactory, new ThreadPoolExecutor.AbortPolicy());
            // 反转任务流
            TaskFlow reversedTaskFlow = this.reverseTaskFlow(taskFlow);
            // 任务表、父任务表和子任务表
            Map<Long, Task> idToTaskMap = new HashMap<>();
            Map<Long, List<Long>> idToParentTaskIdsMap = new HashMap<>();
            reversedTaskFlow.getTaskList().forEach(task -> {
                idToTaskMap.put(task.getId(), task);
                List<Long> parentTaskIds = new ArrayList<>();
                for (Link link : reversedTaskFlow.getLinkList()) {
                    if (link.getTarget().equals(task.getId())) {
                        parentTaskIds.add(link.getSource());
                    }
                }
                idToParentTaskIdsMap.put(task.getId(), parentTaskIds);
            });
            // 参与执行的父任务表和子任务表
            Map<Long, List<Long>> idToParentTaskIds4ExecMap = new HashMap<>();
            Map<Long, List<Long>> idToChildTaskIds4ExecMap = new HashMap<>();
            rollbackTaskFlow.getTaskList().forEach(task -> {
                List<Long> parentTaskIds4Exec = new ArrayList<>();
                List<Long> childTaskIds4Exec = new ArrayList<>();
                for (Link link : rollbackTaskFlow.getLinkList()) {
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
            rollbackTaskFlow.getTaskList().forEach(task -> {
                int inDegree = idToParentTaskIds4ExecMap.get(task.getId()).size();
                idToInDegree4ExecMap.put(task.getId(), inDegree);
            });
            // 从入度为0的任务开始回滚
            Map<Long, String> idToTaskStatusMap = new ConcurrentHashMap<>();
            for (Long taskId : idToInDegree4ExecMap.keySet()) {
                int inDegree = idToInDegree4ExecMap.get(taskId);
                if (inDegree == 0) {
                    idToTaskStatusMap.put(taskId, TaskStatusEnum.WAIT_FOR_ROLLBACK.getCode());
                    Task task = idToTaskMap.get(taskId);
                    this.taskStatusEventPublisher.publishEvent(task, rollbackTaskFlow.getId(), context, TaskStatusEnum.WAIT_FOR_ROLLBACK.getCode(), null, true);
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
                    if (TaskStatusEnum.WAIT_FOR_ROLLBACK.getCode().equals(taskStatus)) {
                        // 等待回滚，自动任务状态改为回滚中，手工任务状态改为手工确认，并将任务加入线程池
                        Task task = idToTaskMap.get(taskId);
                        String statusCode = task.getManual() ? TaskStatusEnum.MANUAL_CONFIRM.getCode() : TaskStatusEnum.ROLLING_BACK.getCode();
                        idToTaskStatusMap.put(task.getId(), statusCode);
                        executorThreadPool.execute(() -> {
                            try {
                                // 检查父任务是否已经回滚成功
                                List<Long> parentTaskIds = idToParentTaskIdsMap.get(task.getId());
                                for (Long parentTaskId : parentTaskIds) {
                                    TaskLog parentTaskStatus = this.taskLogger.getTaskStatus(parentTaskId);
                                    if (this.taskLogger.canRollback(parentTaskStatus)) {
                                        throw new TaskRollbackException("父任务必须先回滚成功");
                                    }
                                }
                                this.taskStatusEventPublisher.publishEvent(task, rollbackTaskFlow.getId(), context, statusCode, null, true);
                                task.beforeRollback(context);
                                task.rollback(context);
                                task.afterRollback(context);
                                idToTaskStatusMap.put(task.getId(), TaskStatusEnum.ROLLBACK_SUCCESS.getCode());
                                this.taskStatusEventPublisher.publishEvent(task, rollbackTaskFlow.getId(), context, TaskStatusEnum.ROLLBACK_SUCCESS.getCode(), null, true);
                            } catch (Exception e) {
                                log.error("任务回滚失败！任务ID：" + task.getId() + " 异常信息：" + e.getMessage(), e);
                                idToTaskStatusMap.put(task.getId(), TaskStatusEnum.ROLLBACK_FAILURE.getCode());
                                try {
                                    this.taskStatusEventPublisher.publishEvent(task, rollbackTaskFlow.getId(), context, TaskStatusEnum.ROLLBACK_FAILURE.getCode(), e.getMessage(), true);
                                } catch (Exception e1) {
                                    log.error("发布任务状态变更事件失败！" + e.getMessage(), e);
                                }
                            }
                        });
                    } else if (requireToStop &&
                            (TaskStatusEnum.ROLLING_BACK.getCode().equals(taskStatus) || TaskStatusEnum.MANUAL_CONFIRM.getCode().equals(taskStatus))) {
                        Task task = idToTaskMap.get(taskId);
                        idToTaskStatusMap.put(task.getId(), TaskStatusEnum.STOPPING.getCode());
                        try {
                            this.taskStatusEventPublisher.publishEvent(task, rollbackTaskFlow.getId(), context, TaskStatusEnum.STOPPING.getCode(), null, true);
                            task.stop(context);
                        } catch (TaskStopException e) {
                            log.error("任务终止失败！任务ID：" + task.getId() + " 异常信息：" + e.getMessage(), e);
                        }
                    } else if (TaskStatusEnum.ROLLBACK_SUCCESS.getCode().equals(taskStatus)) {
                        // 回滚成功，将子任务的入度减1，如果子任务入度为0，则将子任务状态设置为等待回滚并加入状态检查，最后将此任务移除状态检查
                        List<Long> childTaskIds = idToChildTaskIds4ExecMap.get(taskId);
                        for (Long childTaskId : childTaskIds) {
                            int childTaskInDegree = idToInDegree4ExecMap.get(childTaskId);
                            childTaskInDegree--;
                            idToInDegree4ExecMap.put(childTaskId, childTaskInDegree);
                            if (childTaskInDegree == 0) {
                                idToTaskStatusMap.put(childTaskId, TaskStatusEnum.WAIT_FOR_ROLLBACK.getCode());
                                Task childTask = idToTaskMap.get(childTaskId);
                                this.taskStatusEventPublisher.publishEvent(childTask, rollbackTaskFlow.getId(), context, TaskStatusEnum.WAIT_FOR_ROLLBACK.getCode(), null, true);
                            }
                        }
                        idToTaskStatusMap.remove(taskId);
                    } else if (TaskStatusEnum.ROLLBACK_FAILURE.getCode().equals(taskStatus)) {
                        isSuccess = false;
                        // 回滚失败，将此任务及所有子任务移除状态检查
                        idToTaskStatusMap.remove(taskId);
                        List<Long> allChildTaskIds = this.listAllChildTaskIdList(taskId, idToChildTaskIds4ExecMap);
                        for (Long childTaskId : allChildTaskIds) {
                            idToTaskStatusMap.remove(childTaskId);
                        }
                    }
                }
            }
            if (requireToStop) {
                throw new TaskFlowInterruptedException("任务流被终止回滚");
            } else if (!isSuccess) {
                throw new TaskFlowRollbackException("一个或多个任务回滚失败");
            }
        } catch (TaskFlowRollbackException | TaskFlowInterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new TaskFlowRollbackException(e);
        } finally {
            this.clusterController.stopRequestRemove(taskFlowLogId);
        }
    }

    @Override
    public void afterRollback(TaskFlow taskFlow, ConcurrentHashMap<String, Object> context) throws TaskFlowRollbackException {
        // 不做任何操作
    }

    /**
     * 检查任务流
     *
     * @param taskFlow
     * @throws InvalidTaskFlowException
     */
    private void checkTaskFlow(TaskFlow taskFlow) throws InvalidTaskFlowException {
        // 检查任务流是否是一个有向无环图
        List<Task> sortedTaskList = TaskFlowUtils.topologicalSort(taskFlow);
        if (sortedTaskList == null) {
            throw new InvalidTaskFlowException("任务流不是一个有向无环图，请检查是否存在回路！");
        }
        if (taskFlow.getTaskList().size() == 0) {
            throw new InvalidTaskFlowException("任务流中不存在任何任务！");
        }
    }

    /**
     * 排除已执行成功的任务
     *
     * @param taskFlow
     * @exception TryLockException
     * @exception TaskLogException
     * @return
     */
    private TaskFlow excludeSuccessfulTasks(TaskFlow taskFlow) throws TryLockException, TaskLogException {
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
        unsuccessTaskFlow.setFromTaskId(taskFlow.getFromTaskId());
        unsuccessTaskFlow.setToTaskId(taskFlow.getToTaskId());
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
     * 筛选要回滚的任务
     *
     * @param taskFlow
     * @exception TryLockException
     * @exception TaskLogException
     * @return
     */
    private TaskFlow selectRollbackTasks(TaskFlow taskFlow) throws TryLockException, TaskLogException {
        TaskFlow rollbackTaskFlow = new TaskFlow();
        rollbackTaskFlow.setId(taskFlow.getId());
        rollbackTaskFlow.setCron(taskFlow.getCron());
        rollbackTaskFlow.setFromTaskId(taskFlow.getFromTaskId());
        rollbackTaskFlow.setToTaskId(taskFlow.getToTaskId());
        rollbackTaskFlow.setTaskList(new ArrayList<>());
        rollbackTaskFlow.setLinkList(new ArrayList<>());

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

        if (taskStatusList != null && !taskStatusList.isEmpty()) {
            List<Long> canRollbackTaskIdList = new ArrayList<>();
            for (TaskLog taskStatus : taskStatusList) {
                if (this.taskLogger.canRollback(taskStatus)) {
                    canRollbackTaskIdList.add(taskStatus.getTaskId());
                }
            }

            for (Task task : taskFlow.getTaskList()) {
                if (canRollbackTaskIdList.contains(task.getId())) {
                    rollbackTaskFlow.getTaskList().add(task);
                }
            }
            for (Link link : taskFlow.getLinkList()) {
                if (canRollbackTaskIdList.contains(link.getSource()) && canRollbackTaskIdList.contains(link.getTarget())) {
                    rollbackTaskFlow.getLinkList().add(link);
                }
            }
        }
        return rollbackTaskFlow;
    }

    /**
     * 反转任务流
     *
     * @param taskFlow
     * @return
     */
    private TaskFlow reverseTaskFlow(TaskFlow taskFlow) {
        TaskFlow reversedTaskFlow = new TaskFlow();
        reversedTaskFlow.setId(taskFlow.getId());
        reversedTaskFlow.setCron(taskFlow.getCron());
        reversedTaskFlow.setFromTaskId(taskFlow.getFromTaskId());
        reversedTaskFlow.setToTaskId(taskFlow.getToTaskId());
        reversedTaskFlow.setTaskList(new ArrayList<>());
        reversedTaskFlow.setLinkList(new ArrayList<>());
        for (Task task : taskFlow.getTaskList()) {
            reversedTaskFlow.getTaskList().add(task);
        }
        for (Link link : taskFlow.getLinkList()) {
            Link reversedLink = new Link();
            reversedLink.setSource(link.getTarget());
            reversedLink.setTarget(link.getSource());
            reversedTaskFlow.getLinkList().add(reversedLink);
        }
        return reversedTaskFlow;
    }

    /**
     * 获取所有子任务ID列表
     *
     * @param taskId
     * @param idToChildTaskIdsMap
     * @return
     */
    private List<Long> listAllChildTaskIdList(Long taskId, Map<Long, List<Long>> idToChildTaskIdsMap) {
        List<Long> childTaskIdList = idToChildTaskIdsMap.get(taskId);
        return listChildTaskIdList(childTaskIdList, idToChildTaskIdsMap);
    }

    /**
     * 递归获取子任务ID列表
     *
     * @param taskIdList
     * @param idToChildTaskIdsMap
     * @return
     */
    private List<Long> listChildTaskIdList(List<Long> taskIdList, Map<Long, List<Long>> idToChildTaskIdsMap) {
        for (Long taskId : taskIdList) {
            List<Long> childTaskIdList = idToChildTaskIdsMap.get(taskId);
            taskIdList.addAll(listChildTaskIdList(childTaskIdList, idToChildTaskIdsMap));
        }
        return taskIdList;
    }
}
