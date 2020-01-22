package me.kpali.wolfflow.core.cluster;

import me.kpali.wolfflow.core.model.TaskFlowExecRequest;

/**
 * 集群控制器
 *
 * @author kpali
 */
public interface IClusterController {
    /**
     * 尝试获得分布式锁
     *
     * @param name 分布式锁名称
     * @return 成功则返回true
     */
    boolean tryLock(String name);

    /**
     * 插入任务流执行请求到队列中
     *
     * @param request
     * @return 成功返回true
     */
    boolean offer(TaskFlowExecRequest request);

    /**
     * 移除并返回任务流执行队列的首个元素
     *
     * @return 若队列为空则返回null
     */
    TaskFlowExecRequest poll();
}
