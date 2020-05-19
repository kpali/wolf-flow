package me.kpali.wolfflow.core.model;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.exception.TaskExecuteException;
import me.kpali.wolfflow.core.exception.TaskInterruptedException;
import me.kpali.wolfflow.core.exception.TaskStopException;
import me.kpali.wolfflow.core.util.SpringContextUtil;

import java.util.Map;

public class ManualTask extends Task {
    public ManualTask() {
        this.setManual(true);
    }

    private boolean requiredToStop = false;

    @Override
    public void execute(Map<String, Object> taskFlowContext) throws TaskExecuteException, TaskInterruptedException {
        try {
            IClusterController clusterController = SpringContextUtil.getBean(IClusterController.class);
            TaskFlowContextWrapper taskFlowContextWrapper = new TaskFlowContextWrapper(taskFlowContext);
            Map<String, Object> taskContext = taskFlowContextWrapper.getTaskContext(this.getId().toString());
            TaskContextWrapper taskContextWrapper = new TaskContextWrapper(taskContext);
            Long taskLogId = taskContextWrapper.getValue(ContextKey.TASK_LOG_ID, Long.class);

            while (true) {
                try {
                    Thread.sleep(5 * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // 检查是否收到停止任务指令
                if (requiredToStop) {
                    throw new TaskInterruptedException("任务被终止执行");
                }
                // 检查手工确认结果
                if (clusterController.manualConfirmedContains(taskLogId)) {
                    clusterController.manualConfirmedRemove(taskLogId);
                    break;
                }
            }
        } catch (Exception e) {
            throw new TaskExecuteException(e.getMessage());
        }
    }

    @Override
    public void stop(Map<String, Object> taskFlowContext) throws TaskStopException {
        this.requiredToStop = true;
    }
}
