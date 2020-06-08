package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 手工确认信息
 *
 * @author kpali
 */
public class ManualConfirmed implements Serializable {
    private static final long serialVersionUID = 1048105386454641255L;

    public ManualConfirmed() {
    }

    public ManualConfirmed(Long taskLogId, Boolean success, String message) {
        this.taskLogId = taskLogId;
        this.success = success;
        this.message = message;
    }

    private Long taskLogId;
    private Boolean success;
    private String message;

    public Long getTaskLogId() {
        return taskLogId;
    }

    public void setTaskLogId(Long taskLogId) {
        this.taskLogId = taskLogId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
