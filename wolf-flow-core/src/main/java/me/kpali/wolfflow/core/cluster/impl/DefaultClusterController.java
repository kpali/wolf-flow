package me.kpali.wolfflow.core.cluster.impl;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.model.ServiceNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 集群控制器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultClusterController implements IClusterController {
    @Override
    public List<ServiceNode> register() {
        return null;
    }

    @Override
    public boolean competeForMaster() {
        return true;
    }

    @Override
    public boolean tryLock(String name) {
        return true;
    }
}
