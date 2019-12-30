package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 任务流
 *
 * @author kpali
 */
public class TaskFlow implements Serializable {
    private static final long serialVersionUID = 5077291498959687573L;

    /**
     * 任务流ID
     */
    private Long id;
    /**
     * cron表达式
     */
    private String cron;

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
}
