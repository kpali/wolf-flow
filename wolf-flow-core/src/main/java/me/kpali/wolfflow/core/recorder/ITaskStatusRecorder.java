package me.kpali.wolfflow.core.recorder;

import me.kpali.wolfflow.core.exception.TaskStatusRecordException;
import me.kpali.wolfflow.core.model.TaskStatus;

import java.util.List;

/**
 * 任务状态记录器
 *
 * @author kpali
 */
public interface ITaskStatusRecorder {
    /**
     * 根据任务流ID获取任务状态列表
     *
     * @param taskFlowId
     * @return
     * @throws TaskStatusRecordException
     */
    List<TaskStatus> listByTaskFlowId(Long taskFlowId) throws TaskStatusRecordException;

    /**
     * 根据任务ID获取任务状态
     *
     * @param taskFlowId
     * @param taskId
     * @return
     * @throws TaskStatusRecordException
     */
    TaskStatus get(Long taskFlowId, Long taskId) throws TaskStatusRecordException;

    /**
     * 新增或更新任务状态
     *
     * @param taskStatus
     * @throws TaskStatusRecordException
     */
    void put(TaskStatus taskStatus) throws TaskStatusRecordException;

    /**
     * 删除任务状态
     *
     * @param taskFlowId
     * @param taskId
     * @throws TaskStatusRecordException
     */
    void remove(Long taskFlowId, Long taskId) throws TaskStatusRecordException;
}
