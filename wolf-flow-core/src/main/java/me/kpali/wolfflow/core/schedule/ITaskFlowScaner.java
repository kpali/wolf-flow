package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlow;

import java.util.List;

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

    /**
     * 当获取锁成功
     */
    void whenLockSuccess();

    /**
     * 当获取锁失败
     */
    void whenLockFail();

    /**
     * 任务流扫描前置处理
     */
    void beforeScanning();

    /**
     * 定时任务流扫描
     *
     * @return
     */
    List<TaskFlow> scanCronTaskFlow();

    /**
     * 当任务流加入调度
     *
     * @param taskFlow
     */
    void whenJoinSchedule(TaskFlow taskFlow);

    /**
     * 当任务流更新调度
     *
     * @param taskFlow
     */
    void whenUpdateSchedule(TaskFlow taskFlow);

    /**
     * 当任务流调度失败
     *
     * @param taskFlow
     */
    void whenSheduleFail(TaskFlow taskFlow);

    /**
     * 任务流扫描后置处理
     */
    void afterScanning();

}
