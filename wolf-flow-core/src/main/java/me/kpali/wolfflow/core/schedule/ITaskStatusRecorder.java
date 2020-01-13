package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskStatus;

import java.util.List;

/**
 * 任务状态记录器
 *
 * @author kpali
 */
public interface ITaskStatusRecorder {
    /**
     * 获取所有任务状态列表
     *
     * @return
     */
    List<TaskStatus> list();

    /**
     * 根据任务流ID获取任务状态列表
     *
     * @param taskFlowId
     * @return
     */
    List<TaskStatus> listByTaskFlowId(Long taskFlowId);

    /**
     * 根据任务ID获取任务状态
     *
     * @param taskId
     * @return
     */
    TaskStatus get(Long taskId);

    /**
     * 新增或更新任务状态
     *
     * @param taskStatus
     */
    void put(TaskStatus taskStatus);

    /**
     * 删除任务状态
     * @param taskId
     */
    void remove(Long taskId);

    /**
     * 根据任务流ID删除任务状态
     *
     * @param taskFlowId
     */
    void removeByTaskFlowId(Long taskFlowId);
}
