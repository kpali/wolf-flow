package me.kpali.wolfflow.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * 任务流扫描后事件
 *
 * @author kpali
 */
public class AfterScaningEvent extends ApplicationEvent {
    public AfterScaningEvent(Object source) {
        super(source);
    }
}
