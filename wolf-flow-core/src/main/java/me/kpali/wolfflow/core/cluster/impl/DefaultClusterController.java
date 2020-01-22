package me.kpali.wolfflow.core.cluster.impl;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 集群控制器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultClusterController implements IClusterController {
    private Queue<TaskFlowExecRequest> taskFlowExecRequestQueue = new LinkedList<>();

    @Override
    public boolean tryLock(String name) {
        return true;
    }

    @Override
    public boolean offer(TaskFlowExecRequest request) {
        return taskFlowExecRequestQueue.offer(request);
    }

    @Override
    public TaskFlowExecRequest poll() {
        return taskFlowExecRequestQueue.poll();
    }
}
