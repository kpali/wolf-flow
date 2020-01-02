package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 任务
 *
 * @author kpali
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1097164523753393528L;

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
