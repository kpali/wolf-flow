package me.kpali.wolfflow.core.exception;

/**
 * 调度器未启动异常
 *
 * @author kpali
 */
public class SchedulerNotStartedException extends RuntimeException {
    public SchedulerNotStartedException(String message) {
        super(message);
    }
}
