package me.kpali.wolfflow.core.exception;

/**
 * 任务流终止异常
 *
 * @author kpali
 */
public class TaskFlowStopException extends RuntimeException {
    public TaskFlowStopException() {
        super();
    }

    public TaskFlowStopException(String message) {
        super(message);
    }

    public TaskFlowStopException(Throwable cause) {
        super(cause);
    }
}
