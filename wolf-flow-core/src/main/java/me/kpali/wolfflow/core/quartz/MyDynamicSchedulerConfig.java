package me.kpali.wolfflow.core.quartz;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * 动态调度器配置
 *
 * @author kpali
 */
@Configuration
public class MyDynamicSchedulerConfig {

    public MyDynamicSchedulerConfig() {
    }

    @Autowired
    MyJobFactory myJobFactory;

    @Bean
    public SchedulerFactoryBean getSchedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        //schedulerFactory.setDataSource(dataSource);
        schedulerFactory.setAutoStartup(true);
        schedulerFactory.setStartupDelay(20);
        schedulerFactory.setOverwriteExistingJobs(true);
        schedulerFactory.setApplicationContextSchedulerContextKey("applicationContext");
        schedulerFactory.setConfigLocation(new ClassPathResource("quartz.properties"));
        schedulerFactory.setJobFactory(myJobFactory);
        return schedulerFactory;
    }

    @Bean(
            initMethod = "start",
            destroyMethod = "destroy"
    )
    public MyDynamicScheduler getMyDynamicScheduler(SchedulerFactoryBean schedulerFactory) {
        StdSchedulerFactory
        Scheduler scheduler = schedulerFactory.getScheduler();
        MyDynamicScheduler myDynamicScheduler = new MyDynamicScheduler();
        myDynamicScheduler.setScheduler(scheduler);
        return myDynamicScheduler;
    }

//    @Bean
//    public StdSchedulerFactory getStdSchedulerFactory() {
//        return new StdSchedulerFactory();
//    }
//
//    @Bean(
//            initMethod = "start",
//            destroyMethod = "destroy"
//    )
//    public MyDynamicScheduler getMyDynamicScheduler(StdSchedulerFactory schedulerFactory) throws SchedulerException {
//        Scheduler scheduler = schedulerFactory.getScheduler();
//        MyDynamicScheduler myDynamicScheduler = new MyDynamicScheduler();
//        myDynamicScheduler.setScheduler(scheduler);
//        return myDynamicScheduler;
//    }

}
