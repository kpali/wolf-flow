package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 任务流执行请求
 *
 * @author kpali
 */
public class TaskFlowExecRequest implements Serializable {
    private static final long serialVersionUID = 8484106840325360892L;

    public TaskFlowExecRequest() {
    }

    public TaskFlowExecRequest(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
        this.taskFlow = taskFlow;
        this.taskFlowContext = taskFlowContext;
    }

    private TaskFlow taskFlow;
    private TaskFlowContext taskFlowContext;

    public TaskFlow getTaskFlow() {
        return taskFlow;
    }

    public void setTaskFlow(TaskFlow taskFlow) {
        this.taskFlow = taskFlow;
    }

    public TaskFlowContext getTaskFlowContext() {
        return taskFlowContext;
    }

    public void setTaskFlowContext(TaskFlowContext taskFlowContext) {
        this.taskFlowContext = taskFlowContext;
    }
}
