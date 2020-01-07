package me.kpali.wolfflow.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * 任务流扫描前事件
 *
 * @author kpali
 */
public class BeforeScaningEvent extends ApplicationEvent {
    public BeforeScaningEvent(Object source) {
        super(source);
    }
}
