package me.kpali.wolfflow.core.cluster.impl;

import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 集群控制器的默认实现，只适用于单机，不支持集群
 *
 * @author kpali
 */
@Component
public class DefaultClusterController implements IClusterController {
    private static final Logger log = LoggerFactory.getLogger(DefaultClusterController.class);

    private final Object lock = new Object();
    private Map<String, Lock> lockMap = new HashMap<>();
    private Queue<TaskFlowExecRequest> taskFlowExecRequest = new LinkedList<>();
    private Set<Long> taskFlowStopRequest = new HashSet<>();

    private Lock getLock(String name) {
        Lock rLock = null;
        synchronized (lock) {
            if (!lockMap.containsKey(name)) {
                rLock = new ReentrantLock();
                lockMap.put(name, rLock);
            } else {
                rLock = lockMap.get(name);
            }
        }
        return rLock;
    }

    @Override
    public void lock(String name) {
        Lock rLock = this.getLock(name);
        rLock.lock();
    }

    @Override
    public void lock(String name, long leaseTime, TimeUnit unit) {
        Lock rLock = this.getLock(name);
        rLock.lock();
    }

    @Override
    public boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit) {
        Lock rLock = this.getLock(name);
        boolean res = false;
        try {
            res = rLock.tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        }
        return res;
    }

    @Override
    public void unlock(String name) {
        Lock rLock = this.getLock(name);
        rLock.unlock();
    }

    @Override
    public boolean execRequestOffer(TaskFlowExecRequest request) {
        synchronized (lock) {
            return taskFlowExecRequest.offer(request);
        }
    }

    @Override
    public TaskFlowExecRequest execRequestPoll() {
        synchronized (lock) {
            return taskFlowExecRequest.poll();
        }
    }

    @Override
    public void stopRequestAdd(Long taskFlowLogId) {
        synchronized (lock) {
            taskFlowStopRequest.add(taskFlowLogId);
        }
    }

    @Override
    public Boolean stopRequestContains(Long taskFlowLogId) {
        synchronized (lock) {
            return taskFlowStopRequest.contains(taskFlowLogId);
        }
    }

    @Override
    public void stopRequestRemove(Long taskFlowLogId) {
        synchronized (lock) {
            taskFlowStopRequest.remove(taskFlowLogId);
        }
    }
}
