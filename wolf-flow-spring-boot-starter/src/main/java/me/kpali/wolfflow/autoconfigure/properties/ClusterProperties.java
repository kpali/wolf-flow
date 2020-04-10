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
