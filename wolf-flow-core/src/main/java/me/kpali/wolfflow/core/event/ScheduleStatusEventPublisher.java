package me.kpali.wolfflow.core.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 任务流调度状态事件发布器
 *
 * @author kpali
 */
@Component
public class ScheduleStatusEventPublisher {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public void publishEvent(ScheduleStatusChangeEvent scheduleStatusChangeEvent) {
        this.eventPublisher.publishEvent(scheduleStatusChangeEvent);
    }
}
