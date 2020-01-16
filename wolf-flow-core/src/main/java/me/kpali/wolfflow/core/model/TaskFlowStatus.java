package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 任务流状态
 *
 * @author kpali
 */
public class TaskFlowStatus implements Serializable {
    private static final long serialVersionUID = 4373673109768512258L;

    private TaskFlow taskFlow;
    private TaskFlowContext taskFlowContext;
    private String status;
    private String message;

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
