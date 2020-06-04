package me.kpali.wolfflow.core.exception;

/**
 * 任务流日志异常
 *
 * @author kpali
 */
public class TaskFlowLogException extends Exception {
    public TaskFlowLogException() {
        super();
    }

    public TaskFlowLogException(String message) {
        super(message);
    }

    public TaskFlowLogException(Throwable cause) {
        super(cause);
    }
}
