package me.kpali.wolfflow.core.model;

import me.kpali.wolfflow.core.exception.TaskExecuteException;
import me.kpali.wolfflow.core.exception.TaskInterruptedException;
import me.kpali.wolfflow.core.exception.TaskStopException;

import java.io.Serializable;
import java.util.Map;

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

    public void beforeExecute(Map<String, Object> taskFlowContext) throws TaskExecuteException {
        // 不做任何操作
    }

    public void execute(Map<String, Object> taskFlowContext) throws TaskExecuteException, TaskInterruptedException {
        // 不做任何操作
    }

    public void afterExecute(Map<String, Object> taskFlowContext) throws TaskExecuteException {
        // 不做任何操作
    }

    public void stop(Map<String, Object> taskFlowContext) throws TaskStopException {
        // 不做任何操作
    }
}
