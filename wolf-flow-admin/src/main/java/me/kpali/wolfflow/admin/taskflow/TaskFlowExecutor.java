package me.kpali.wolfflow.admin.taskflow;

import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.core.schedule.DefaultTaskFlowExecutor;

public class TaskFlowExecutor extends DefaultTaskFlowExecutor {
    @Override
    public void execute(TaskFlowContext taskFlowContext) {
        try {
            Thread.sleep(30 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("========== 任务执行完成");
    }
}
