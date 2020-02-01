package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.cluster.impl.DefaultClusterController;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Primary
@Component
public class MyClusterController extends DefaultClusterController {
    private static final Logger log = LoggerFactory.getLogger(MyClusterController.class);

    @Autowired
    private RedissonClient redisson;

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
    public boolean offer(TaskFlowExecRequest request) {
        RQueue<TaskFlowExecRequest> queue = redisson.getQueue("taskFlowExecQueue");
        return queue.offer(request);
    }

    @Override
    public TaskFlowExecRequest poll() {
        RQueue<TaskFlowExecRequest> queue = redisson.getQueue("taskFlowExecQueue");
        return queue.poll();
    }
}
