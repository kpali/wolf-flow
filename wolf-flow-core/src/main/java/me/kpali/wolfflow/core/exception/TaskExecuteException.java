package me.kpali.wolfflow.core.exception;

/**
 * 任务执行异常
 *
 * @author kpali
 */
public class TaskExecuteException extends RuntimeException {
    public TaskExecuteException() {
        super();
    }

    public TaskExecuteException(String message) {
        super(message);
    }

    public TaskExecuteException(Throwable cause) {
        super(cause);
    }
}
