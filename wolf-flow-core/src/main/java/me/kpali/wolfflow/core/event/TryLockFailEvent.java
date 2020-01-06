package me.kpali.wolfflow.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * 获取锁失败事件
 *
 * @author kpali
 */
public class TryLockFailEvent extends ApplicationEvent {
    public TryLockFailEvent(Object source) {
        super(source);
    }
}
