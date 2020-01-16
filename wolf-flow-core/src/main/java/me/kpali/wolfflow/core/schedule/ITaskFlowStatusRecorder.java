package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlowStatus;

import java.util.List;

/**
 * 任务流状态记录器
 *
 * @author kpali
 */
public interface ITaskFlowStatusRecorder {
    /**
     * 获取任务流状态列表
     *
     * @return
     */
    List<TaskFlowStatus> list();

    /**
     * 根据任务流ID获取任务流状态
     *
     * @param taskFlowId
     * @return
     */
    TaskFlowStatus get(Long taskFlowId);

    /**
     * 新增或更新任务流状态
     *
     * @param taskFlowStatus
     */
    void put(TaskFlowStatus taskFlowStatus);

    /**
     * 删除任务流状态
     * @param taskFlowId
     */
    void remove(Long taskFlowId);
}
