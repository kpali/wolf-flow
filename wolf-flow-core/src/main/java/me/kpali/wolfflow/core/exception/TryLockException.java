package me.kpali.wolfflow.core.exception;

/**
 * 尝试加锁异常
 *
 * @author kpali
 */
public class TryLockException extends Exception {
    public TryLockException() {
        super();
    }

    public TryLockException(String message) {
        super(message);
    }

    public TryLockException(Throwable cause) {
        super(cause);
    }
}
