package me.kpali.wolfflow.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * 任务流调度状态变更事件
 *
 * @author kpali
 */
public class ScheduleStatusChangeEvent extends ApplicationEvent {
    public ScheduleStatusChangeEvent(Object source, Long taskFlowId, String cronExpression, String scheduleStatus) {
        super(source);
        this.taskFlowId = taskFlowId;
        this.cronExpression = cronExpression;
        this.scheduleStatus = scheduleStatus;
    }

    private Long taskFlowId;
    private String cronExpression;
    private String scheduleStatus;

    public Long getTaskFlowId() {
        return taskFlowId;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public String getScheduleStatus() {
        return scheduleStatus;
    }
}
