package me.kpali.wolfflow.core.model;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.exception.TaskExecuteException;
import me.kpali.wolfflow.core.exception.TaskInterruptedException;
import me.kpali.wolfflow.core.exception.TaskRollbackException;
import me.kpali.wolfflow.core.exception.TaskStopException;
import me.kpali.wolfflow.core.util.SpringContextUtil;
import me.kpali.wolfflow.core.util.context.TaskContextWrapper;
import me.kpali.wolfflow.core.util.context.TaskFlowContextWrapper;

import java.util.concurrent.ConcurrentHashMap;

public class ManualTask extends Task {
    public ManualTask() {
        this.setManual(true);
    }

    private boolean requiredToStop = false;

    @Override
    public void execute(ConcurrentHashMap<String, Object> context) throws TaskExecuteException, TaskInterruptedException {
        try {
            IClusterController clusterController = SpringContextUtil.getBean(IClusterController.class);
            TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
            ConcurrentHashMap<String, Object> taskContext = taskFlowContextWrapper.getTaskContext(this.getId().toString());
            TaskContextWrapper taskContextWrapper = new TaskContextWrapper(taskContext);
            Long taskLogId = taskContextWrapper.getValue(ContextKey.TASK_LOG_ID, Long.class);

            while (true) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // 检查是否收到停止任务指令
                if (requiredToStop) {
                    throw new TaskInterruptedException("Task execution is terminated");
                }
                // 检查手工确认结果
                ManualConfirmed manualConfirmed = clusterController.manualConfirmedGet(taskLogId);
                if (manualConfirmed != null) {
                    clusterController.manualConfirmedRemove(taskLogId);
                    if (manualConfirmed.getSuccess()) {
                        break;
                    } else {
                        throw new TaskExecuteException(manualConfirmed.getMessage());
                    }
                }
            }
        } catch (TaskInterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new TaskExecuteException(e);
        }
    }

    @Override
    public void rollback(ConcurrentHashMap<String, Object> context) throws TaskRollbackException, TaskInterruptedException {
        try {
            IClusterController clusterController = SpringContextUtil.getBean(IClusterController.class);
            TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(context);
            ConcurrentHashMap<String, Object> taskContext = taskFlowContextWrapper.getTaskContext(this.getId().toString());
            TaskContextWrapper taskContextWrapper = new TaskContextWrapper(taskContext);
            Long taskLogId = taskContextWrapper.getValue(ContextKey.TASK_LOG_ID, Long.class);

            while (true) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // 检查是否收到停止任务指令
                if (requiredToStop) {
                    throw new TaskInterruptedException("Task rollback is terminated");
                }
                // 检查手工确认结果
                ManualConfirmed manualConfirmed = clusterController.manualConfirmedGet(taskLogId);
                if (manualConfirmed != null) {
                    clusterController.manualConfirmedRemove(taskLogId);
                    if (manualConfirmed.getSuccess()) {
                        break;
                    } else {
                        throw new TaskRollbackException(manualConfirmed.getMessage());
                    }
                }
            }
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
