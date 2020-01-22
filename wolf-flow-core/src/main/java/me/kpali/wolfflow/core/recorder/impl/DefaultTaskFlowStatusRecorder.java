package me.kpali.wolfflow.core.recorder.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.exception.TaskFlowStatusRecordException;
import me.kpali.wolfflow.core.model.TaskFlowStatus;
import me.kpali.wolfflow.core.recorder.ITaskFlowStatusRecorder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务流状态记录器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskFlowStatusRecorder implements ITaskFlowStatusRecorder {
    private Map<Long, TaskFlowStatus> taskFlowStatusMap = new HashMap<>();
    private final Object lock = new Object();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<TaskFlowStatus> list() throws TaskFlowStatusRecordException {
        synchronized (lock) {
            try {
                List<TaskFlowStatus> taskFlowStatusList = new ArrayList<>(taskFlowStatusMap.values());
                String json = objectMapper.writeValueAsString(taskFlowStatusList);
                JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, TaskFlowStatus.class);
                List<TaskFlowStatus> taskFlowStatusListCloned = objectMapper.readValue(json, javaType);
                return taskFlowStatusListCloned;
            } catch (JsonProcessingException e) {
                throw new TaskFlowStatusRecordException(e);
            }
        }
    }

    @Override
    public TaskFlowStatus get(Long taskFlowId) throws TaskFlowStatusRecordException {
        synchronized (lock) {
            TaskFlowStatus taskFlowStatusCloned = null;
            if (taskFlowStatusMap.containsKey(taskFlowId)) {
                TaskFlowStatus taskFlowStatus = taskFlowStatusMap.get(taskFlowId);
                try {
                    String json = objectMapper.writeValueAsString(taskFlowStatus);
                    taskFlowStatusCloned = objectMapper.readValue(json, TaskFlowStatus.class);
                } catch (JsonProcessingException e) {
                    throw new TaskFlowStatusRecordException(e);
                }
            }
            return taskFlowStatusCloned;
        }
    }

    @Override
    public void put(TaskFlowStatus taskFlowStatus) throws TaskFlowStatusRecordException {
        synchronized (lock) {
            try {
                String json = objectMapper.writeValueAsString(taskFlowStatus);
                TaskFlowStatus taskFlowStatusCloned = objectMapper.readValue(json, TaskFlowStatus.class);
                taskFlowStatusMap.put(taskFlowStatusCloned.getTaskFlow().getId(), taskFlowStatusCloned);
            } catch (JsonProcessingException e) {
                throw new TaskFlowStatusRecordException(e);
            }
        }
    }

    @Override
    public TaskFlowStatus putIfNotInProgress(TaskFlowStatus taskFlowStatus) throws TaskFlowStatusRecordException {
        synchronized (lock) {
            TaskFlowStatus oldTaskFlowStatus = this.get(taskFlowStatus.getTaskFlow().getId());
            boolean isInProgress = oldTaskFlowStatus != null && this.isInProgress(oldTaskFlowStatus);
            if (isInProgress) {
                return null;
            }
            this.put(taskFlowStatus);
            return taskFlowStatus;
        }
    }

    @Override
    public TaskFlowStatus toStoppingIfInProgress(Long taskFlowId) throws TaskFlowStatusRecordException {
        synchronized (lock) {
            TaskFlowStatus taskFlowStatus = this.get(taskFlowId);
            if (taskFlowStatus != null) {
                if (this.isInProgress(taskFlowStatus)) {
                    taskFlowStatus.setStatus(TaskFlowStatusEnum.STOPPING.getCode());
                    this.put(taskFlowStatus);
                    return taskFlowStatus;
                }
            }
            return null;
        }
    }

    private boolean isInProgress(TaskFlowStatus taskFlowStatus) {
        return !TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskFlowStatus.getStatus())
                || !TaskFlowStatusEnum.EXECUTE_FAILURE.getCode().equals(taskFlowStatus.getStatus());
    }

    @Override
    public void remove(Long taskFlowId) throws TaskFlowStatusRecordException {
        synchronized (lock) {
            taskFlowStatusMap.remove(taskFlowId);
        }
    }
}
