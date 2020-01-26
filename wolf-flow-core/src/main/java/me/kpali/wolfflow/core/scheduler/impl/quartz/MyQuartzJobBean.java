package me.kpali.wolfflow.core.scheduler.impl.quartz;

import me.kpali.wolfflow.core.exception.InvalidTaskFlowException;
import me.kpali.wolfflow.core.exception.TaskFlowTriggerException;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
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
        try {
            taskFlowScheduler.trigger(taskFlowId, null);
        } catch (InvalidTaskFlowException | TaskFlowTriggerException e) {
            throw new JobExecutionException(e);
        }
    }
}
