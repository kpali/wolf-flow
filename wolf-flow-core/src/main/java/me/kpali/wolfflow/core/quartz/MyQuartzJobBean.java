package me.kpali.wolfflow.core.quartz;

import me.kpali.wolfflow.core.schedule.TaskFlowScheduler;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Quartz作业
 *
 * @author kpali
 */
public class MyQuartzJobBean extends QuartzJobBean {

    public MyQuartzJobBean() {
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getTrigger().getJobKey();
        Long taskFlowId = Long.valueOf(jobKey.getName());
        TaskFlowScheduler.execute(taskFlowId);
    }

}
