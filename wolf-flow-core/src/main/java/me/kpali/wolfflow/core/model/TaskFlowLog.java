package me.kpali.wolfflow.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务流日志
 *
 * @author kpali
 */
public class TaskFlowLog implements Serializable {
    private static final long serialVersionUID = -875754396936412243L;

    private Long logId;
    private Long taskFlowId;
    private TaskFlow taskFlow;
    private TaskFlowContext taskFlowContext;
    private String status;
    private String message;
    private Date creationTime;
    private Date updateTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getTaskFlowId() {
        return taskFlowId;
    }

    public void setTaskFlowId(Long taskFlowId) {
        this.taskFlowId = taskFlowId;
    }

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

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
