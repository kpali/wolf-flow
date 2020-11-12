package me.kpali.wolfflow.core;

import me.kpali.wolfflow.core.config.ClusterConfig;
import me.kpali.wolfflow.core.config.ExecutorConfig;
import me.kpali.wolfflow.core.config.SchedulerConfig;
import me.kpali.wolfflow.core.launcher.Launcher;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class BaseTest {
    @Autowired
    ClusterConfig clusterConfig;

    @Autowired
    SchedulerConfig schedulerConfig;

    @Autowired
    ExecutorConfig executorConfig;

    @Autowired
    Launcher launcher;

    @Order(0)
    @Test
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

        this.executorConfig.setCorePoolSize(30);
        this.executorConfig.setMaximumPoolSize(30);

        this.launcher.startup();
    }
}
