package me.kpali.wolfflow.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 集群控制器配置
 *
 * @author kpali
 */
@ConfigurationProperties(prefix = "wolf-flow.cluster")
public class ClusterProperties {
    private Integer nodeHeartbeatInterval = 30;
    private Integer nodeHeartbeatDuration = 90;
    private Integer generateNodeIdLockLeaseTime = 60;
    private Integer taskFlowLogLockWaitTime = 10;
    private Integer taskFlowLogLockLeaseTime = 15;
    private Integer taskLogLockWaitTime = 10;
    private Integer taskLogLockLeaseTime = 15;

    public Integer getNodeHeartbeatInterval() {
        return nodeHeartbeatInterval;
    }

    public void setNodeHeartbeatInterval(Integer nodeHeartbeatInterval) {
        this.nodeHeartbeatInterval = nodeHeartbeatInterval;
    }

    public Integer getNodeHeartbeatDuration() {
        return nodeHeartbeatDuration;
    }

    public void setNodeHeartbeatDuration(Integer nodeHeartbeatDuration) {
        this.nodeHeartbeatDuration = nodeHeartbeatDuration;
    }

    public Integer getGenerateNodeIdLockLeaseTime() {
        return generateNodeIdLockLeaseTime;
    }

    public void setGenerateNodeIdLockLeaseTime(Integer generateNodeIdLockLeaseTime) {
        this.generateNodeIdLockLeaseTime = generateNodeIdLockLeaseTime;
    }

    public Integer getTaskFlowLogLockWaitTime() {
        return taskFlowLogLockWaitTime;
    }

    public void setTaskFlowLogLockWaitTime(Integer taskFlowLogLockWaitTime) {
        this.taskFlowLogLockWaitTime = taskFlowLogLockWaitTime;
    }

    public Integer getTaskFlowLogLockLeaseTime() {
        return taskFlowLogLockLeaseTime;
    }

    public void setTaskFlowLogLockLeaseTime(Integer taskFlowLogLockLeaseTime) {
        this.taskFlowLogLockLeaseTime = taskFlowLogLockLeaseTime;
    }

    public Integer getTaskLogLockWaitTime() {
        return taskLogLockWaitTime;
    }

    public void setTaskLogLockWaitTime(Integer taskLogLockWaitTime) {
        this.taskLogLockWaitTime = taskLogLockWaitTime;
    }

    public Integer getTaskLogLockLeaseTime() {
        return taskLogLockLeaseTime;
    }

    public void setTaskLogLockLeaseTime(Integer taskLogLockLeaseTime) {
        this.taskLogLockLeaseTime = taskLogLockLeaseTime;
    }
}
