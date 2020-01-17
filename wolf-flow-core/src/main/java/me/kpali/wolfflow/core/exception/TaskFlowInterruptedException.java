package me.kpali.wolfflow.core.exception;

/**
 * 任务流中断异常
 *
 * @author kpali
 */
public class TaskFlowInterruptedException extends RuntimeException {

    public TaskFlowInterruptedException(String message) {
        super(message);
    }

}