package me.kpali.wolfflow.core.schedule;

/**
 * 任务流扫描器接口
 *
 * @author kpali
 */
public interface ITaskFlowScaner {

    /**
     * 扫描前尝试获取锁，获取成功返回true，如果不存在竞争可以直接返回true
     *
     * @return
     */
    boolean tryLock();

}
