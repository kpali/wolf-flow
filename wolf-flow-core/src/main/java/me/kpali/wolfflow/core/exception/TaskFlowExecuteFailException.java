package me.kpali.wolfflow.core.exception;

/**
 * 任务流执行异常
 *
 * @author kpali
 */
public class TaskFlowExecuteFailException extends RuntimeException {
    public TaskFlowExecuteFailException() {
        super();
    }

    public TaskFlowExecuteFailException(String message) {
        super(message);
    }

    public TaskFlowExecuteFailException(Throwable cause) {
        super(cause);
    }
}
