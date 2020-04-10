package me.kpali.wolfflow.core.cluster;

import me.kpali.wolfflow.core.model.TaskFlowExecRequest;

import java.util.concurrent.TimeUnit;

/**
 * 集群控制器
 *
 * @author kpali
 */
public interface IClusterController {
    /**
     * 获取当前节点ID
     *
     * @return 节点ID
     */
    String getNodeId();

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
}
