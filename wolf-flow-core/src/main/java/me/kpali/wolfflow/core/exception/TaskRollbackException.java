package me.kpali.wolfflow.core.exception;

/**
 * 任务回滚异常
 *
 * @author kpali
 */
public class TaskRollbackException extends Exception {
    public TaskRollbackException() {
        super();
    }

    public TaskRollbackException(String message) {
        super(message);
    }

    public TaskRollbackException(Throwable cause) {
        super(cause);
    }
}
