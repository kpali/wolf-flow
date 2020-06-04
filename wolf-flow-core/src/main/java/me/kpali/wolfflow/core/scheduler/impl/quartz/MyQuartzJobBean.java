package me.kpali.wolfflow.core.scheduler.impl.quartz;

import me.kpali.wolfflow.core.exception.InvalidTaskFlowException;
import me.kpali.wolfflow.core.exception.TaskFlowTriggerException;
import me.kpali.wolfflow.core.model.ContextKey;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz作业
 *
 * @author kpali
 */
public class MyQuartzJobBean extends QuartzJobBean {
    public MyQuartzJobBean() {
    }

    @Autowired
    ITaskFlowScheduler taskFlowScheduler;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getTrigger().getJobKey();
        Long taskFlowId = Long.valueOf(jobKey.getName());
        Long fromTaskId = null;
        Long toTaskId = null;
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        if (jobDataMap != null) {
            fromTaskId = (jobDataMap.get(ContextKey.FROM_TASK_ID) == null ? null : jobDataMap.getLong(ContextKey.FROM_TASK_ID));
            toTaskId = (jobDataMap.get(ContextKey.TO_TASK_ID) == null ? null : jobDataMap.getLong(ContextKey.TO_TASK_ID));
        }
        try {
            if (fromTaskId != null && fromTaskId.equals(toTaskId)) {
                taskFlowScheduler.execute(taskFlowId, fromTaskId, null);
            } else if (fromTaskId != null) {
                taskFlowScheduler.executeFrom(taskFlowId, fromTaskId, null);
            } else if (toTaskId != null) {
                taskFlowScheduler.executeTo(taskFlowId, toTaskId, null);
            } else {
                taskFlowScheduler.execute(taskFlowId, null);
            }
        } catch (TaskFlowTriggerException e) {
            throw new JobExecutionException(e);
        }
    }
}
