package me.kpali.wolfflow.core.exception;

/**
 * 任务流触发异常
 *
 * @author kpali
 */
public class TaskFlowTriggerException extends RuntimeException {
    public TaskFlowTriggerException() {
        super();
    }

    public TaskFlowTriggerException(String message) {
        super(message);
    }

    public TaskFlowTriggerException(Throwable cause) {
        super(cause);
    }
}
