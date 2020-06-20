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
        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setNodeHeartbeatInterval(clusterProperties.getNodeHeartbeatInterval());
        clusterConfig.setNodeHeartbeatDuration(clusterProperties.getNodeHeartbeatDuration());
        clusterConfig.setGenerateNodeIdLockLeaseTime(clusterProperties.getGenerateNodeIdLockLeaseTime());
        clusterConfig.setTaskFlowLogLockWaitTime(clusterProperties.getTaskFlowLogLockWaitTime());
        clusterConfig.setTaskFlowLogLockLeaseTime(clusterProperties.getTaskFlowLogLockLeaseTime());
        clusterConfig.setTaskLogLockWaitTime(clusterProperties.getTaskLogLockWaitTime());
        clusterConfig.setTaskLogLockLeaseTime(clusterProperties.getTaskLogLockLeaseTime());
        return clusterConfig;
    }
}
