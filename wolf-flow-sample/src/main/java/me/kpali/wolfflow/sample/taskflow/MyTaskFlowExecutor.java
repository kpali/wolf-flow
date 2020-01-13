package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.core.schedule.DefaultTaskFlowExecutor;
import org.springframework.stereotype.Component;

@Component
public class MyTaskFlowExecutor extends DefaultTaskFlowExecutor {
    @Override
    public void afterExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
    }
}
