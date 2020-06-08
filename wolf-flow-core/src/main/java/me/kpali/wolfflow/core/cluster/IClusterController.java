package me.kpali.wolfflow.core.cluster;

import me.kpali.wolfflow.core.model.ManualConfirmed;
import me.kpali.wolfflow.core.model.TaskFlowExecRequest;

import java.util.concurrent.TimeUnit;

/**
 * 集群控制器
 *
 * @author kpali
 */
public interface IClusterController {
    /**
     * 启动集群控制器
     */
    void startup();

    /**
     * 获取当前节点ID
     *
     * @return 节点ID
     */
    String getNodeId();

    /**
     * 发送当前节点心跳
     */
    void heartbeat();

    /**
     * 查询其他节点是否存活
     *
     * @param nodeId
     * @return
     */
    boolean isNodeAlive(String nodeId);

    /**
     * 加锁，如果暂时无法加锁，则当前线程休眠，直到加锁成功
     * 加锁成功后，解锁需要调用unlock方法
     *
     * @param name
     */
    void lock(String name);

    /**
     * 加锁，如果暂时无法加锁，则当前线程休眠，直到加锁成功
     * 加锁成功后，解锁需要调用unlock方法或在到达指定时间后自动解锁
     *
     * @param name 锁名称
     * @param leaseTime 租赁时间，即上锁后多久自动解锁
     * @param unit 时间单位
     */
    void lock(String name, long leaseTime, TimeUnit unit);

    /**
     * 尝试加锁
     *
     * @param name 锁名称
     * @param waitTime 等待时间，即尝试加锁的最多等待时间
     * @param leaseTime 租赁时间，即上锁后多久自动解锁
     * @param unit 时间单位
     * @return 成功则返回true
     */
    boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 解锁
     *
     * @param name 锁名称
     */
    void unlock(String name);

    /**
     * 插入任务流执行请求到队列中
     *
     * @param request
     * @return 成功返回true
     */
    boolean execRequestOffer(TaskFlowExecRequest request);

    /**
     * 移除并返回任务流执行队列的首个元素
     *
     * @return 若队列为空则返回null
     */
    TaskFlowExecRequest execRequestPoll();

    /**
     * 新增任务流停止请求
     *
     * @param taskFlowLogId
     */
    void stopRequestAdd(Long taskFlowLogId);

    /**
     * 查询是否包含任务流停止请求
     *
     * @param taskFlowLogId
     * @return
     */
    Boolean stopRequestContains(Long taskFlowLogId);

    /**
     * 删除任务流停止请求
     *
     * @param taskFlowLogId
     */
    void stopRequestRemove(Long taskFlowLogId);

    /**
     * 新增手工确认信息
     *
     * @param manualConfirmed
     */
    void manualConfirmedAdd(ManualConfirmed manualConfirmed);

    /**
     * 获取手工确认信息
     *
     * @param taskLogId
     * @return
     */
    ManualConfirmed manualConfirmedGet(Long taskLogId);

    /**
     * 删除手工确认信息
     *
     * @param taskLogId
     */
    void manualConfirmedRemove(Long taskLogId);
}
