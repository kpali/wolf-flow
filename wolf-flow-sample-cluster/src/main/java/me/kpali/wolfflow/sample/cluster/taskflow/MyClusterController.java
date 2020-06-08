package me.kpali.wolfflow.sample.cluster.taskflow;

import me.kpali.wolfflow.core.cluster.impl.DefaultClusterController;
import me.kpali.wolfflow.core.config.ClusterConfig;
import me.kpali.wolfflow.core.model.ManualConfirmed;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 可以覆写默认集群控制器的方法实现自定义，本示例使用Redis实现集群控制器来支持集群
 * （可选）
 *
 * @author kpali
 */
@Primary
@Component
public class MyClusterController extends DefaultClusterController {
    private static final Logger log = LoggerFactory.getLogger(MyClusterController.class);

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private ClusterConfig clusterConfig;

    private static final String NODE_HEARTBEAT = "nodeHeartbeat";
    private static final String TASK_FLOW_EXEC_REQUEST = "taskFlowExecRequest";
    private static final String TASK_FLOW_STOP_REQUEST = "taskFlowStopRequest";
    private static final String MANUAL_CONFIRMED = "manualConfirmed";

    @Override
    public void heartbeat() {
        RLock lock = redisson.getLock(NODE_HEARTBEAT + ":" + this.getNodeId());
        lock.lock(this.clusterConfig.getNodeHeartbeatDuration(), TimeUnit.SECONDS);
    }

    @Override
    public boolean isNodeAlive(String nodeId) {
        RLock lock = redisson.getLock(NODE_HEARTBEAT + ":" + nodeId);
        return lock.isLocked();
    }

    @Override
    public void lock(String name) {
        RLock lock = redisson.getLock(name);
        lock.lock();
    }

    @Override
    public void lock(String name, long leaseTime, TimeUnit unit) {
        RLock lock = redisson.getLock(name);
        lock.lock(leaseTime, unit);
    }

    @Override
    public boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redisson.getLock(name);
        boolean res = false;
        try {
            res = lock.tryLock(waitTime, leaseTime, unit);
        } catch (Exception e) {
            log.error("尝试获取锁异常：" + e.getMessage(), e);
        }
        return res;
    }

    @Override
    public void unlock(String name) {
        RLock lock = redisson.getLock(name);
        lock.unlock();
    }

    @Override
    public boolean execRequestOffer(TaskFlowExecRequest request) {
        RQueue<TaskFlowExecRequest> queue = redisson.getQueue(TASK_FLOW_EXEC_REQUEST);
        return queue.offer(request);
    }

    @Override
    public TaskFlowExecRequest execRequestPoll() {
        RQueue<TaskFlowExecRequest> queue = redisson.getQueue(TASK_FLOW_EXEC_REQUEST);
        return queue.poll();
    }

    @Override
    public void stopRequestAdd(Long taskFlowLogId) {
        RSet<Long> set = redisson.getSet(TASK_FLOW_STOP_REQUEST);
        set.add(taskFlowLogId);
    }

    @Override
    public Boolean stopRequestContains(Long taskFlowLogId) {
        RSet<Long> set = redisson.getSet(TASK_FLOW_STOP_REQUEST);
        return set.contains(taskFlowLogId);
    }

    @Override
    public void stopRequestRemove(Long taskFlowLogId) {
        RSet<Long> set = redisson.getSet(TASK_FLOW_STOP_REQUEST);
        set.remove(taskFlowLogId);
    }

    @Override
    public void manualConfirmedAdd(ManualConfirmed manualConfirmed) {
        RMap<Long, ManualConfirmed> map = redisson.getMap(MANUAL_CONFIRMED);
        map.put(manualConfirmed.getTaskLogId(), manualConfirmed);
    }

    @Override
    public ManualConfirmed manualConfirmedGet(Long taskLogId) {
        RMap<Long, ManualConfirmed> map = redisson.getMap(MANUAL_CONFIRMED);
        return map.get(taskLogId);
    }

    @Override
    public void manualConfirmedRemove(Long taskLogId) {
        RMap<Long, ManualConfirmed> map = redisson.getMap(MANUAL_CONFIRMED);
        map.remove(taskLogId);
    }
}
