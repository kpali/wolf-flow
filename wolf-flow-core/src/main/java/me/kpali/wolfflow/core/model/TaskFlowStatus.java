package me.kpali.wolfflow.core.model;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务流状态
 *
 * @author kpali
 */
public class TaskFlowStatus implements Serializable {
    private static final long serialVersionUID = 4373673109768512258L;

    private TaskFlow taskFlow;
    private Map<String, Object> context;
    private String status;
    private String message;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
