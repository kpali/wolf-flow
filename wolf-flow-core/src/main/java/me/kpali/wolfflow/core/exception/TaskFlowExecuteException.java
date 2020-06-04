package me.kpali.wolfflow.core.exception;

/**
 * 任务流执行异常
 *
 * @author kpali
 */
public class TaskFlowExecuteException extends Exception {
    public TaskFlowExecuteException() {
        super();
    }

    public TaskFlowExecuteException(String message) {
        super(message);
    }

    public TaskFlowExecuteException(Throwable cause) {
        super(cause);
    }
}
