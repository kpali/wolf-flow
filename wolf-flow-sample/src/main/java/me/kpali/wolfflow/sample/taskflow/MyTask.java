package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.exception.TaskExecuteException;
import me.kpali.wolfflow.core.exception.TaskInterruptedException;
import me.kpali.wolfflow.core.exception.TaskStopException;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.ContextKey;
import me.kpali.wolfflow.core.model.Task;
import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.sample.util.SpringContextUtil;

/**
 * 自定义任务，覆写父类的方法，实现自定义任务的执行内容
 * （必要）
 *
 * @author kpali
 */
public class MyTask extends Task {
    private boolean requiredToStop = false;

    @Override
    public void execute(TaskFlowContext taskFlowContext) throws TaskExecuteException, TaskInterruptedException {
        ITaskLogger taskLogger = SpringContextUtil.getBean(ITaskLogger.class);
        Long taskLogId = Long.parseLong(taskFlowContext.getTaskContexts().get(this.getId()).get(ContextKey.LOG_ID));
        taskLogger.log(taskLogId, this.getId(), "任务开始执行", false);
        taskLogger.log(taskLogId, this.getId(), "日志第二行\r日志第三行\n日志第四行\r\n日志第五行", false);
        int totalTime = 0;
        int timeout = 2000;
        while (totalTime < timeout) {
            try {
                if (requiredToStop) {
                    taskLogger.log(taskLogId, this.getId(), "任务被终止执行", true);
                    throw new TaskInterruptedException("任务被终止执行");
                }
                Thread.sleep(1000);
                totalTime += 1000;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        taskLogger.log(taskLogId, this.getId(), "任务执行完成", true);
    }

    @Override
    public void stop(TaskFlowContext taskFlowContext) throws TaskStopException {
        this.requiredToStop = true;
    }
}
