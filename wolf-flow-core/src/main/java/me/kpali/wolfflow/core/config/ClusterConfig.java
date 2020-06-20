package me.kpali.wolfflow.core.config;

import org.springframework.stereotype.Component;

/**
 * 集群控制器配置
 *
 * @author kpali
 */
@Component
public class ClusterConfig {
    /**
     * 节点发送心跳间隔时间，单位秒
     */
    private Integer nodeHeartbeatInterval;
    /**
     * 节点心跳有效期，单位秒
     */
    private Integer nodeHeartbeatDuration;
    /**
     * 生成节点ID获取锁后自动解锁时间，单位秒
     */
    private Integer generateNodeIdLockLeaseTime;
    /**
     * 任务流日志记录获取锁最大等待时间，单位秒
     */
    private Integer taskFlowLogLockWaitTime;
    /**
     * 任务流日志记录获取锁后自动解锁时间，单位秒
     */
    private Integer taskFlowLogLockLeaseTime;
    /**
     * 任务日志记录获取锁最大等待时间，单位秒
     */
    private Integer taskLogLockWaitTime;
    /**
     * 任务日志记录获取锁后自动解锁时间，单位秒
     */
    private Integer taskLogLockLeaseTime;

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
