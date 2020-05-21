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

    public TaskFlowExecRequest(TaskFlow taskFlow, Map<String, Object> context) {
        this.taskFlow = taskFlow;
        this.context = context;
    }

    private TaskFlow taskFlow;
    private Map<String, Object> context;

    public TaskFlow getTaskFlow() {
        return taskFlow;
    }

    public void setTaskFlow(TaskFlow taskFlow) {
        this.taskFlow = taskFlow;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
