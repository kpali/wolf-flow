package me.kpali.wolfflow.core.exception;

/**
 * 任务状态记录异常
 *
 * @author kpali
 */
public class TaskStatusRecordException extends RuntimeException {
    public TaskStatusRecordException() {
        super();
    }

    public TaskStatusRecordException(String message) {
        super(message);
    }

    public TaskStatusRecordException(Throwable cause) {
        super(cause);
    }
}
