package me.kpali.wolfflow.core.exception;

/**
 * 任务日志异常
 *
 * @author kpali
 */
public class TaskLogException extends Exception {
    public TaskLogException() {
        super();
    }

    public TaskLogException(String message) {
        super(message);
    }

    public TaskLogException(Throwable cause) {
        super(cause);
    }
}
