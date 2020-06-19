package me.kpali.wolfflow.core.model;

import me.kpali.wolfflow.core.exception.TaskExecuteException;
import me.kpali.wolfflow.core.exception.TaskInterruptedException;
import me.kpali.wolfflow.core.exception.TaskRollbackException;
import me.kpali.wolfflow.core.exception.TaskStopException;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务
 *
 * @author kpali
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1097164523753393528L;

    private Long id;
    /**
     * 是否手工任务
     */
    private boolean manual = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean getManual() {
        return this.manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public void executePreCheck(ConcurrentHashMap<String, Object> context) throws TaskExecuteException {
        // 不作任何操作
    }

    public void beforeExecute(ConcurrentHashMap<String, Object> context) throws TaskExecuteException {
        // 不做任何操作
    }

    public void execute(ConcurrentHashMap<String, Object> context) throws TaskExecuteException, TaskInterruptedException {
        // 不做任何操作
    }

    public void afterExecute(ConcurrentHashMap<String, Object> context) throws TaskExecuteException {
        // 不做任何操作
    }

    public void beforeRollback(ConcurrentHashMap<String, Object> context) throws TaskRollbackException {
        // 不做任何操作
    }

    public void rollbackPreCheck(ConcurrentHashMap<String, Object> context) throws TaskRollbackException {
        // 不作任何操作
    }

    public void rollback(ConcurrentHashMap<String, Object> context) throws TaskRollbackException, TaskInterruptedException {
        // 不做任何操作
    }

    public void afterRollback(ConcurrentHashMap<String, Object> context) throws TaskRollbackException {
        // 不做任何操作
    }

    public void stop(ConcurrentHashMap<String, Object> context) throws TaskStopException {
        // 不做任何操作
    }
}
