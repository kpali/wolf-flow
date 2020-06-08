package me.kpali.wolfflow.core.cluster.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.config.ClusterConfig;
import me.kpali.wolfflow.core.model.ManualConfirmed;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
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

    @Autowired
    private ClusterConfig clusterConfig;

    protected Long nodeId;
    private final Object lock = new Object();
    private Map<String, Lock> lockMap = new HashMap<>();
    private Queue<TaskFlowExecRequest> taskFlowExecRequest = new LinkedList<>();
    private Set<Long> taskFlowStopRequest = new HashSet<>();
    private Map<Long, ManualConfirmed> manualConfirmedMap = new HashMap<>();
    private Map<Long, Date> heartbeatMap = new HashMap<>();

    private boolean started = false;

    @Override
    public void startup() {
        if (this.started) {
            return;
        }
        log.info("集群控制器启动，节点发送心跳间隔时间：{}秒，节点心跳有效期：{}秒",
                this.clusterConfig.getNodeHeartbeatInterval(),
                this.clusterConfig.getNodeHeartbeatDuration());
        this.started = true;
        this.generateNodeId();
        this.startNodeHeartbeat();
    }

    /**
     * 启动节点心跳发送
     */
    private void startNodeHeartbeat() {
        ThreadFactory heartbeatThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("nodeHeartbeat-pool-%d").build();
        ExecutorService heartbeatThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), heartbeatThreadFactory, new ThreadPoolExecutor.AbortPolicy());

        log.info("节点心跳线程启动");
        heartbeatThreadPool.execute(() -> {
            while (true) {
                try {
                    log.info("发送节点心跳，当前节点ID：{}", this.getNodeId());
                    Integer heartbeatIntervalInMilliseconds = this.clusterConfig.getNodeHeartbeatInterval() * 1000;
                    this.heartbeat();
                    Thread.sleep(heartbeatIntervalInMilliseconds);
                } catch (Exception e) {
                    log.error("发送节点心跳异常！" + e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void generateNodeId() {
        this.nodeId = 1L;
    }

    @Override
    public Long getNodeId() {
        return this.nodeId;
    }

    @Override
    public void heartbeat() {
        synchronized (lock) {
            long heartbeatDurationInMilliseconds = this.clusterConfig.getNodeHeartbeatDuration() * 1000;
            Date now = new Date();
            Date heartbeatExpireDate = new Date(now.getTime() + heartbeatDurationInMilliseconds);
            this.heartbeatMap.put(this.getNodeId(), heartbeatExpireDate);
        }
    }

    @Override
    public boolean isNodeAlive(Long nodeId) {
        synchronized (lock) {
            if (this.heartbeatMap.containsKey(nodeId)) {
                Date heartbeatExpireDate = this.heartbeatMap.get(nodeId);
                Date now = new Date();
                return now.before(heartbeatExpireDate);
            }
            return false;
        }
    }

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

    @Override
    public void manualConfirmedAdd(ManualConfirmed manualConfirmed) {
        synchronized (lock) {
            if (manualConfirmed != null) {
                manualConfirmedMap.put(manualConfirmed.getTaskLogId(), manualConfirmed);
            }
        }
    }

    @Override
    public ManualConfirmed manualConfirmedGet(Long taskLogId) {
        synchronized (lock) {
            return manualConfirmedMap.get(taskLogId);
        }
    }

    @Override
    public void manualConfirmedRemove(Long taskLogId) {
        synchronized (lock) {
            manualConfirmedMap.remove(taskLogId);
        }
    }
}
