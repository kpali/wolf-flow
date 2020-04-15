package me.kpali.wolfflow.core.model;

import java.io.Serializable;
import java.util.List;

/**
 * 任务流
 *
 * @author kpali
 */
public class TaskFlow implements Serializable {
    private static final long serialVersionUID = 5077291498959687573L;

    private Long id;
    private List<Task> taskList;
    private List<Link> linkList;
    private String cron;
    private Long fromTaskId;
    private Long toTaskId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }

    public List<Link> getLinkList() {
        return linkList;
    }

    public void setLinkList(List<Link> linkList) {
        this.linkList = linkList;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public Long getFromTaskId() {
        return fromTaskId;
    }

    public void setFromTaskId(Long fromTaskId) {
        this.fromTaskId = fromTaskId;
    }

    public Long getToTaskId() {
        return toTaskId;
    }

    public void setToTaskId(Long toTaskId) {
        this.toTaskId = toTaskId;
    }
}
