package me.kpali.wolfflow.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务日志
 *
 * @author kpali
 */
public class TaskLog implements Serializable {
    private static final long serialVersionUID = 14370329912885306L;

    private Long logId;
    private Long taskFlowLogId;
    private Long taskId;
    private Task task;
    private Long taskFlowId;
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

    public Long getTaskFlowLogId() {
        return taskFlowLogId;
    }

    public void setTaskFlowLogId(Long taskFlowLogId) {
        this.taskFlowLogId = taskFlowLogId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

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
