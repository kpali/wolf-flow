package me.kpali.wolfflow.core.cluster.impl;

import me.kpali.wolfflow.core.BaseTest;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.exception.GenerateNodeIdException;
import me.kpali.wolfflow.core.model.ManualConfirmed;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefaultClusterControllerTest extends BaseTest {

    @Autowired
    IClusterController clusterController;

    @Test
    public void testGetNodeId() throws GenerateNodeIdException {
        this.clusterController.generateNodeId();
        assertNotNull(this.clusterController.getNodeId());
    }

    @Order(1)
    @Test
    public void testHeartbeat() {
        this.clusterController.heartbeat();
    }

    @Order(2)
    @Test
    public void testIsNodeAlive() {
        assertTrue(this.clusterController.isNodeAlive(this.clusterController.getNodeId()));
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
        assertFalse(isContains);
    }

    @Test
    public void testManualConfirmed() {
        Long taskLogId = 1L;
        this.clusterController.manualConfirmedAdd(new ManualConfirmed(taskLogId, true, null));
        ManualConfirmed manualConfirmed = this.clusterController.manualConfirmedGet(taskLogId);
        assertNotNull(manualConfirmed);
        this.clusterController.manualConfirmedRemove(taskLogId);
        manualConfirmed = this.clusterController.manualConfirmedGet(taskLogId);
        assertNull(manualConfirmed);
    }
}