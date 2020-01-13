package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 任务
 *
 * @author kpali
 */
public class Task implements Serializable {
    public Task() {
    }

    public Task(Long id) {
        this.id = id;
    }

    private static final long serialVersionUID = 1097164523753393528L;

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void execute(TaskFlowContext taskFlowContext, TaskContext taskContext) throws Exception {
        // 不做任何操作
    }

    public void stop(TaskFlowContext taskFlowContext, TaskContext taskContext) throws Exception {
        // 不做任何操作
    }
}
