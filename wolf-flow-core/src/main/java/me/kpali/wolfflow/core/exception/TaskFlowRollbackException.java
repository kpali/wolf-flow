package me.kpali.wolfflow.core.exception;

/**
 * 任务流回滚异常
 *
 * @author kpali
 */
public class TaskFlowRollbackException extends Exception {
    public TaskFlowRollbackException() {
        super();
    }

    public TaskFlowRollbackException(String message) {
        super(message);
    }

    public TaskFlowRollbackException(Throwable cause) {
        super(cause);
    }
}
