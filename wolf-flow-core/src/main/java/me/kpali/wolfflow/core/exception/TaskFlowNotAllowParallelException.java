package me.kpali.wolfflow.core.exception;

/**
 * 任务流不允许并行异常
 *
 * @author kpali
 */
public class TaskFlowNotAllowParallelException extends RuntimeException {

    public TaskFlowNotAllowParallelException(String message) {
        super(message);
    }

}
