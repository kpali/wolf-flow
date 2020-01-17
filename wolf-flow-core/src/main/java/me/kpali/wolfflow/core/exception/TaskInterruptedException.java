package me.kpali.wolfflow.core.exception;

/**
 * 任务中断异常
 *
 * @author kpali
 */
public class TaskInterruptedException extends RuntimeException {

    public TaskInterruptedException(String message) {
        super(message);
    }

}
