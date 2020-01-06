package me.kpali.wolfflow.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * 获取锁成功事件
 *
 * @author kpali
 */
public class TryLockSuccessEvent extends ApplicationEvent {
    public TryLockSuccessEvent(Object source) {
        super(source);
    }
}
