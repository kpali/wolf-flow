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

    public TaskFlowExecRequest(Long taskFlowId, Map<String, Object> context) {
        this.taskFlowId = taskFlowId;
        this.context = context;
    }

    private Long taskFlowId;
    private Map<String, Object> context;

    public Long getTaskFlowId() {
        return taskFlowId;
    }

    public void setTaskFlowId(Long taskFlowId) {
        this.taskFlowId = taskFlowId;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
