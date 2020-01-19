package me.kpali.wolfflow.core.exception;

/**
 * 任务流状态记录异常
 *
 * @author kpali
 */
public class TaskFlowStatusRecordException extends RuntimeException {
    public TaskFlowStatusRecordException() {
        super();
    }

    public TaskFlowStatusRecordException(String message) {
        super(message);
    }

    public TaskFlowStatusRecordException(Throwable cause) {
        super(cause);
    }
}
