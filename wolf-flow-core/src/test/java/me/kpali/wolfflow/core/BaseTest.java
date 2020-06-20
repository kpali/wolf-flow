package me.kpali.wolfflow.core;

import me.kpali.wolfflow.core.config.ClusterConfig;
import me.kpali.wolfflow.core.config.ExecutorConfig;
import me.kpali.wolfflow.core.config.SchedulerConfig;
import me.kpali.wolfflow.core.launcher.Launcher;
import me.kpali.wolfflow.core.listener.TaskFlowEventListener;
import me.kpali.wolfflow.core.util.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

@SpringBootTest(classes = {
        WolfFlowCoreApplication.class,
        SpringContextUtil.class,
        TaskFlowEventListener.class
})
public class BaseTest extends AbstractTestNGSpringContextTests {
    @Autowired
    ClusterConfig clusterConfig;

    @Autowired
    SchedulerConfig schedulerConfig;

    @Autowired
    ExecutorConfig executorConfig;

    @Autowired
    Launcher launcher;

    @BeforeClass
    public void init() {
        this.clusterConfig.setNodeHeartbeatInterval(30);
        this.clusterConfig.setNodeHeartbeatDuration(90);
        this.clusterConfig.setGenerateNodeIdLockLeaseTime(60);
        this.clusterConfig.setTaskFlowLogLockWaitTime(10);
        this.clusterConfig.setTaskFlowLogLockLeaseTime(15);
        this.clusterConfig.setTaskLogLockWaitTime(10);
        this.clusterConfig.setTaskLogLockLeaseTime(15);

        this.schedulerConfig.setExecRequestScanInterval(1);
        this.schedulerConfig.setCronScanInterval(10);
        this.schedulerConfig.setCronScanLockWaitTime(10);
        this.schedulerConfig.setCronScanLockLeaseTime(60);
        this.schedulerConfig.setCorePoolSize(10);
        this.schedulerConfig.setMaximumPoolSize(10);

        this.executorConfig.setCorePoolSize(3);
        this.executorConfig.setMaximumPoolSize(3);

        this.launcher.startup();
    }
}
