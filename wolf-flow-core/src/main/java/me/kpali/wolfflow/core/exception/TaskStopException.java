package me.kpali.wolfflow.core.exception;

/**
 * 任务终止异常
 *
 * @author kpali
 */
public class TaskStopException extends RuntimeException {
    public TaskStopException() {
        super();
    }

    public TaskStopException(String message) {
        super(message);
    }

    public TaskStopException(Throwable cause) {
        super(cause);
    }
}
