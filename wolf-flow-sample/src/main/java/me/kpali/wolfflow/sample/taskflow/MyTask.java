package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.exception.TaskExecuteException;
import me.kpali.wolfflow.core.exception.TaskInterruptedException;
import me.kpali.wolfflow.core.exception.TaskStopException;
import me.kpali.wolfflow.core.model.Task;
import me.kpali.wolfflow.core.model.TaskFlowContext;

public class MyTask extends Task {
    public MyTask() {
        super();
    }

    public MyTask(Long id) {
        super(id);
    }

    private boolean requiredToStop = false;

    @Override
    public void execute(TaskFlowContext taskFlowContext) throws TaskExecuteException {
        int totalTime = 0;
        int timeout = 3 * 1000;
        while (totalTime < timeout) {
            try {
                if (requiredToStop) {
                    throw new TaskInterruptedException("任务被终止执行");
                }
                Thread.sleep( 1000);
                totalTime += 1000;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop(TaskFlowContext taskFlowContext) throws TaskStopException {
        this.requiredToStop = true;
    }
}
