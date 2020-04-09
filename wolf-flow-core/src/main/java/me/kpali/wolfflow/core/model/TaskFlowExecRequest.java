package me.kpali.wolfflow.core.model;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务流执行请求
 *
 * @author kpali
 */
public class TaskFlowExecRequest implements Serializable {
    private static final long serialVersionUID = 8484106840325360892L;

    public TaskFlowExecRequest() {
    }

    public TaskFlowExecRequest(TaskFlow taskFlow, Map<String, Object> taskFlowContext) {
        this.taskFlow = taskFlow;
        this.taskFlowContext = taskFlowContext;
    }

    private TaskFlow taskFlow;
    private Map<String, Object> taskFlowContext;

    public TaskFlow getTaskFlow() {
        return taskFlow;
    }

    public void setTaskFlow(TaskFlow taskFlow) {
        this.taskFlow = taskFlow;
    }

    public Map<String, Object> getTaskFlowContext() {
        return taskFlowContext;
    }

    public void setTaskFlowContext(Map<String, Object> taskFlowContext) {
        this.taskFlowContext = taskFlowContext;
    }
}
