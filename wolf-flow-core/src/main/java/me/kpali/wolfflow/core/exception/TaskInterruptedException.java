package me.kpali.wolfflow.core.exception;

/**
 * 任务中断异常
 *
 * @author kpali
 */
public class TaskInterruptedException extends Exception {
    public TaskInterruptedException() {
        super();
    }

    public TaskInterruptedException(String message) {
        super(message);
    }

    public TaskInterruptedException(Throwable cause) {
        super(cause);
    }
}
