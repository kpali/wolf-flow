package me.kpali.wolfflow.core.exception;

/**
 * 无效的任务流异常
 *
 * @author kpali
 */
public class InvalidTaskFlowException extends Exception {
    public InvalidTaskFlowException() {
        super();
    }

    public InvalidTaskFlowException(String message) {
        super(message);
    }

    public InvalidTaskFlowException(Throwable cause) {
        super(cause);
    }
}
