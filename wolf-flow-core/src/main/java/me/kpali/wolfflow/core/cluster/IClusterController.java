package me.kpali.wolfflow.core.cluster;

import me.kpali.wolfflow.core.model.ServiceNode;

import java.util.List;

/**
 * 集群控制器
 *
 * @author kpali
 */
public interface IClusterController {
    /**
     * 服务注册
     *
     * @return 已注册的服务节点列表
     */
    List<ServiceNode> register();

    /**
     * 竞争master节点
     *
     * @return 成功则返回true
     */
    boolean competeForMaster();

    /**
     * 尝试获得分布式锁
     *
     * @param name 分布式锁名称
     * @return 成功则返回true
     */
    boolean tryLock(String name);
}
