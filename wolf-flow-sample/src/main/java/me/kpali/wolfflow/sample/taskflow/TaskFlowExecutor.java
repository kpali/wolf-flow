package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.core.schedule.DefaultTaskFlowExecutor;

public class TaskFlowExecutor extends DefaultTaskFlowExecutor {
    @Override
    public void execute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
        try {
            Thread.sleep(30 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("========== 任务流执行完成，任务流ID：" + taskFlow.getId());
    }
}
