package me.kpali.wolfflow.core.exception;

/**
 * 无效的cron表达式异常
 *
 * @author kpali
 */
public class InvalidCronExpressionException extends RuntimeException {

    public InvalidCronExpressionException(String message) {
        super(message);
    }

}
