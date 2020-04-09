package me.kpali.wolfflow.core.model;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务状态
 *
 * @author kpali
 */
public class TaskStatus implements Serializable {
    private static final long serialVersionUID = -933124446911559604L;

    private Task task;
    private Long taskFlowId;
    private Map<String, Object> taskFlowContext;
    private String status;
    private String message;

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Long getTaskFlowId() {
        return taskFlowId;
    }

    public void setTaskFlowId(Long taskFlowId) {
        this.taskFlowId = taskFlowId;
    }

    public Map<String, Object> getTaskFlowContext() {
        return taskFlowContext;
    }

    public void setTaskFlowContext(Map<String, Object> taskFlowContext) {
        this.taskFlowContext = taskFlowContext;
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
