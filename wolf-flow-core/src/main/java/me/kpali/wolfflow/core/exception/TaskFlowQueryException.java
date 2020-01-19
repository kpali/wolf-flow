package me.kpali.wolfflow.core.exception;

/**
 * 任务流查询异常
 *
 * @author kpali
 */
public class TaskFlowQueryException extends RuntimeException {
    public TaskFlowQueryException() {
        super();
    }

    public TaskFlowQueryException(String message) {
        super(message);
    }

    public TaskFlowQueryException(Throwable cause) {
        super(cause);
    }
}
