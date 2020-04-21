package me.kpali.wolfflow.core.cluster.impl;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.ClusterConfig;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@SpringBootTest
public class DefaultClusterControllerTest extends AbstractTestNGSpringContextTests {

    @Autowired
    IClusterController clusterController;

    @Autowired
    ClusterConfig clusterConfig;

    @BeforeClass
    public void setUp() {
        this.clusterConfig.setNodeHeartbeatInterval(30);
        this.clusterConfig.setNodeHeartbeatDuration(90);
    }

    @Test
    public void testStartup() {
        this.clusterController.startup();
    }

    @Test
    public void testGetNodeId() {
        assertNotNull(this.clusterController.getNodeId());
    }

    @Test
    public void testHeartbeat() {
        this.clusterController.heartbeat();
    }

    @Test
    public void testIsNodeAlive() {
        assertEquals(this.clusterController.isNodeAlive(this.clusterController.getNodeId()), true);
    }

    @Test
    public void testLock() {
        this.clusterController.lock("testLock");
    }

    @Test
    public void testLockWithLeaseTime() {
        this.clusterController.lock("testLockWithLeaseTime", 15, TimeUnit.SECONDS);
    }

    @Test
    public void testTryLock() {
        this.clusterController.tryLock("testTryLock", 10, 15, TimeUnit.SECONDS);
    }

    @Test
    public void testUnlock() {
        String lockName = "testUnlock";
        this.clusterController.lock(lockName);
        this.clusterController.unlock(lockName);
    }

    @Test
    public void testExecRequest() {
        this.clusterController.execRequestOffer(new TaskFlowExecRequest());
        assertNotNull(this.clusterController.execRequestPoll());
    }

    @Test
    public void testStopRequest() {
        Long taskFlowLogId = 1L;
        this.clusterController.stopRequestAdd(taskFlowLogId);
        boolean isContains = this.clusterController.stopRequestContains(taskFlowLogId);
        assertEquals(isContains, true);
        this.clusterController.stopRequestRemove(taskFlowLogId);
        isContains = this.clusterController.stopRequestContains(taskFlowLogId);
        assertEquals(isContains, false);
    }
}