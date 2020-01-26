package me.kpali.wolfflow.core.scheduler.impl.quartz;

import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 动态调度器
 *
 * @author kpali
 */
public class MyDynamicScheduler {

    public MyDynamicScheduler() {
    }

    private static final Logger log = LoggerFactory.getLogger(MyDynamicScheduler.class);
    private static Scheduler scheduler;

    public void setScheduler(Scheduler scheduler) {
        MyDynamicScheduler.scheduler = scheduler;
    }

    public void start() throws Exception {
        Assert.notNull(scheduler, "quartz scheduler is null");
        log.info("Quartz 调度器初始化完成");
    }

    public void destroy() throws Exception {
    }

    public static boolean checkExists(String jobName, String jobGroup) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        return scheduler.checkExists(triggerKey);
    }

    public static void addJob(String jobName, String jobGroup, String cronExpression) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        JobKey jobKey = new JobKey(jobName, jobGroup);
        if (!scheduler.checkExists(triggerKey)) {
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing();
            CronTrigger cronTrigger = (CronTrigger)TriggerBuilder.newTrigger().withIdentity(triggerKey).withSchedule(cronScheduleBuilder).build();
            Class<? extends Job> jobClass_ = MyQuartzJobBean.class;
            JobDetail jobDetail = JobBuilder.newJob(jobClass_).withIdentity(jobKey).build();
            Date date = scheduler.scheduleJob(jobDetail, cronTrigger);
            log.info("Quartz 新增任务成功 -> jobKey:{}",  jobKey);
        }
    }

    public static void removeJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, jobGroup);
        scheduler.deleteJob(jobKey);
        log.info("Quartz 删除任务成功 -> jobKey:{}", jobKey);
    }

    public static void updateJobCron(String jobGroup, String jobName, String cronExpression) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        if (scheduler.checkExists(triggerKey)) {
            CronTrigger oldTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            String oldCron = oldTrigger.getCronExpression();
            if (!oldCron.equals(cronExpression)) {
                CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing();
                oldTrigger = (CronTrigger) oldTrigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(cronScheduleBuilder).build();
                scheduler.rescheduleJob(triggerKey, oldTrigger);
                log.info("Quartz 更新任务成功 -> jobGroup:{}, jobName:{}, cronExpression:{}", jobGroup, jobName, cronExpression);
            }
        }
    }

    public static void clear() throws SchedulerException {
        scheduler.clear();
    }

    public static Set<TriggerKey> getTriggerKeys() throws SchedulerException {
        return scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup());
    }

    public static Set<TriggerKey> getTriggerKeysGroupEquals(String group) throws SchedulerException {
        return scheduler.getTriggerKeys(GroupMatcher.groupEquals(group));
    }

    public static Set<JobKey> getJobKeys() throws SchedulerException {
        return scheduler.getJobKeys(GroupMatcher.anyJobGroup());
    }

    public static Set<JobKey> getJobKeysGroupEquals(String group) throws SchedulerException {
        return scheduler.getJobKeys(GroupMatcher.groupEquals(group));
    }

    public static List<String> getJobGroupNames() throws SchedulerException {
        return scheduler.getJobGroupNames();
    }

    public static List<String> getTriggerGroupNames() throws SchedulerException {
        return scheduler.getTriggerGroupNames();
    }

    public static List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException {
        return scheduler.getCurrentlyExecutingJobs();
    }

}
