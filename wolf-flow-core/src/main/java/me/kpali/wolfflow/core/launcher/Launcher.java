package me.kpali.wolfflow.core.launcher;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.monitor.IMonitor;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 启动器
 *
 * @author kpali
 */
@Component
public class Launcher {
    @Autowired
    IMonitor monitor;
    @Autowired
    ITaskFlowScheduler taskFlowScheduler;
    @Autowired
    IClusterController clusterController;

    public void startup() {
        this.monitor.init();
        this.taskFlowScheduler.startup();
        this.clusterController.startup();
    }
}
