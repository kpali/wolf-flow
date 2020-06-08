package me.kpali.wolfflow.core.util;

import me.kpali.wolfflow.core.cluster.IClusterController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ID生成器
 *
 * @author kpali
 */
@Component
public class IdGenerator {
    @Autowired
    private IClusterController clusterController;

    private SnowFlake snowFlake;

    public synchronized long nextId() {
        if (snowFlake == null) {
            snowFlake = new SnowFlake(this.clusterController.getNodeId());
        }
        return snowFlake.nextId();
    }
}
