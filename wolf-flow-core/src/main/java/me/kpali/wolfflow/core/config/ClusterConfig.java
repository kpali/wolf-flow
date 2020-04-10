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
}
