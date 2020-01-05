package me.kpali.wolfflow.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务流日志
 *
 * @author kpali
 */
public class TaskFlowLog implements Serializable {
    private static final long serialVersionUID = 7287277421894786956L;

    /**
     * 任务流日志ID
     */
    private Long id;
    /**
     * 任务流ID
     */
    private Long taskFlowId;
    /**
     * 任务流状态，默认状态有WAIT_FOR_EXECUTE, EXECUTING, EXECUTE_SUCCESS, EXECUTE_FAIL
     */
    private String status;
    /**
     * 任务流日志上下文，可以存放任务流相关的信息
     */
    private String context;
    /**
     * 创建时间
     */
    private Date creationTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskFlowId() {
        return taskFlowId;
    }

    public void setTaskFlowId(Long taskFlowId) {
        this.taskFlowId = taskFlowId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
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
