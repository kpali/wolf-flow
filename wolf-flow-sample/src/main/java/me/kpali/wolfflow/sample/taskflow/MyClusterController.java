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
    public boolean tryLock(String name) {
        int lockLeaseTime = 60;
        int lockWaitTime = 10;
        RLock lock = redisson.getLock(name);
        boolean res = false;
        try {
            res = lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("尝试获取分布式锁异常：" + e.getMessage(), e);
        }
        return res;
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
