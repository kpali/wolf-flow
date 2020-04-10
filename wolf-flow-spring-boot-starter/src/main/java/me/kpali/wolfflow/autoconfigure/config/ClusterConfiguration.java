package me.kpali.wolfflow.autoconfigure.config;

import me.kpali.wolfflow.autoconfigure.properties.ClusterProperties;
import me.kpali.wolfflow.core.config.ClusterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 集群控制器配置
 *
 * @author kpali
 */
@ComponentScan(basePackages = {"me.kpali.wolfflow.core.cluster"})
public class ClusterConfiguration {
    @Bean
    public ClusterConfig getClusterConfig(ClusterProperties clusterProperties) {
        Integer nodeHeartbeatInterval = clusterProperties.getNodeHeartbeatInterval();
        Integer nodeHeartbeatDuration = clusterProperties.getNodeHeartbeatDuration();
        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setNodeHeartbeatInterval(nodeHeartbeatInterval);
        clusterConfig.setNodeHeartbeatDuration(nodeHeartbeatDuration);
        return clusterConfig;
    }
}
