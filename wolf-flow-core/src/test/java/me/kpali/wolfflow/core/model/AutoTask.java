package me.kpali.wolfflow.core.model;

import me.kpali.wolfflow.core.exception.TaskExecuteException;
import me.kpali.wolfflow.core.exception.TaskInterruptedException;
import me.kpali.wolfflow.core.exception.TaskRollbackException;
import me.kpali.wolfflow.core.exception.TaskStopException;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.util.SpringContextUtil;
import me.kpali.wolfflow.core.util.context.TaskContextWrapper;
import me.kpali.wolfflow.core.util.context.TaskFlowContextWrapper;

import java.util.concurrent.ConcurrentHashMap;

public class AutoTask extends Task {
    private boolean requiredToStop = false;

    @Override
    public void execute(ConcurrentHashMap<String, Object> context) throws TaskExecuteException, TaskInterruptedException {
        if (context == null) {
            throw new IllegalArgumentException();
        }
        try {
            ITaskLogger taskLogger = SpringContextUtil.getBean(ITaskLogger.class);
            TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
            ConcurrentHashMap<String, Object> taskContext = taskFlowContextWrapper.getTaskContext(this.getId().toString());
            TaskContextWrapper taskContextWrapper = new TaskContextWrapper(taskContext);
            Long taskLogId = taskContextWrapper.getValue(ContextKey.TASK_LOG_ID, Long.class);
            String taskLogFileId = taskContextWrapper.getValue(ContextKey.TASK_LOG_FILE_ID, String.class);
            taskLogger.log(taskLogFileId, "Task executing...", false);
            taskLogger.log(taskLogFileId, "Second line...\rThird line...\nFourth line...\r\nFifth line...", false);
            if (requiredToStop) {
                taskLogger.log(taskLogFileId, "Task execution is terminated", true);
                throw new TaskInterruptedException("Task execution is terminated");
            }
            taskLogger.log(taskLogFileId, "Task execution finished", true);
        } catch (TaskInterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new TaskExecuteException(e);
        }
    }

    @Override
    public void rollback(ConcurrentHashMap<String, Object> context) throws TaskRollbackException, TaskInterruptedException {
        try {
            ITaskLogger taskLogger = SpringContextUtil.getBean(ITaskLogger.class);
            TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
            ConcurrentHashMap<String, Object> taskContext = taskFlowContextWrapper.getTaskContext(this.getId().toString());
            TaskContextWrapper taskContextWrapper = new TaskContextWrapper(taskContext);
            Long taskLogId = taskContextWrapper.getValue(ContextKey.TASK_LOG_ID, Long.class);
            String taskLogFileId = taskContextWrapper.getValue(ContextKey.TASK_LOG_FILE_ID, String.class);
            taskLogger.log(taskLogFileId, "Task rolling back...", false);
            taskLogger.log(taskLogFileId, "Second line...\rThird line...\nFourth line...\r\nFifth line...", false);
            if (requiredToStop) {
                taskLogger.log(taskLogFileId, "Task execution is terminated", true);
                throw new TaskInterruptedException("Task execution is terminated");
            }
            taskLogger.log(taskLogFileId, "Task rollback finished", true);
        } catch (TaskInterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new TaskRollbackException(e);
        }
    }

    @Override
    public void stop(ConcurrentHashMap<String, Object> context) throws TaskStopException {
        this.requiredToStop = true;
    }
}
