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
    private String cron;
    private List<Task> taskList;
    private List<Link> linkList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
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
}
