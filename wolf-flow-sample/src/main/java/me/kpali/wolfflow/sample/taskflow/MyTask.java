package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.model.Task;
import me.kpali.wolfflow.core.model.TaskFlowContext;

public class MyTask extends Task {
    public MyTask() {
        super();
    }

    public MyTask(Long id) {
        super(id);
    }

    @Override
    public void execute(TaskFlowContext taskFlowContext) throws Exception {
        try {
            Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
