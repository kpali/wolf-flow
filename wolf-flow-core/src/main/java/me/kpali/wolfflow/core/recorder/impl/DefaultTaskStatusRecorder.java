package me.kpali.wolfflow.core.recorder.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.recorder.ITaskStatusRecorder;
import me.kpali.wolfflow.core.exception.TaskFlowStatusRecordException;
import me.kpali.wolfflow.core.exception.TaskStatusRecordException;
import me.kpali.wolfflow.core.model.TaskStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务状态记录器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskStatusRecorder implements ITaskStatusRecorder {
    private Map<Long, List<TaskStatus>> taskStatusListMap = new HashMap<>();
    private final Object lock = new Object();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<TaskStatus> listByTaskFlowId(Long taskFlowId) throws TaskStatusRecordException {
        synchronized (lock) {
            List<TaskStatus> taskStatusList = new ArrayList<>();
            if (taskStatusListMap.containsKey(taskFlowId)) {
                taskStatusList = taskStatusListMap.get(taskFlowId);
            }
            try {
                String json = objectMapper.writeValueAsString(taskStatusList);
                JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, TaskStatus.class);
                List<TaskStatus> taskStatusListCloned = objectMapper.readValue(json, javaType);
                return taskStatusListCloned;
            } catch (JsonProcessingException e) {
                throw new TaskFlowStatusRecordException(e);
            }
        }
    }

    @Override
    public TaskStatus get(Long taskFlowId, Long taskId) throws TaskStatusRecordException {
        synchronized (lock) {
            TaskStatus taskStatusCloned = null;
            if (taskStatusListMap.containsKey(taskFlowId)) {
                List<TaskStatus> taskStatusList = taskStatusListMap.get(taskFlowId);
                for (TaskStatus taskStatus : taskStatusList) {
                    if (taskStatus.getTask().getId().equals(taskId)) {
                        try {
                            String json = objectMapper.writeValueAsString(taskStatus);
                            taskStatusCloned = objectMapper.readValue(json, TaskStatus.class);
                        } catch (JsonProcessingException e) {
                            throw new TaskFlowStatusRecordException(e);
                        }
                        break;
                    }
                }
            }
            return taskStatusCloned;
        }
    }

    @Override
    public void put(TaskStatus taskStatus) throws TaskStatusRecordException {
        synchronized (lock) {
            TaskStatus taskStatusCloned;
            try {
                String json = objectMapper.writeValueAsString(taskStatus);
                taskStatusCloned = objectMapper.readValue(json, TaskStatus.class);
            } catch (JsonProcessingException e) {
                throw new TaskFlowStatusRecordException(e);
            }
            if (!taskStatusListMap.containsKey(taskStatusCloned.getTaskFlowId())) {
                // 新增
                List<TaskStatus> taskStatusList = new ArrayList<>();
                taskStatusList.add(taskStatusCloned);
                taskStatusListMap.put(taskStatusCloned.getTaskFlowId(), taskStatusList);
            } else {
                // 更新
                List<TaskStatus> oldTaskStatusList = taskStatusListMap.get(taskStatusCloned.getTaskFlowId());
                for (TaskStatus oldTaskStatus : oldTaskStatusList) {
                    if (oldTaskStatus.getTask().getId().equals(taskStatusCloned.getTask().getId())) {
                        BeanUtils.copyProperties(taskStatusCloned, oldTaskStatus);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void remove(Long taskFlowId, Long taskId) throws TaskStatusRecordException {
        synchronized (lock) {
            if (taskStatusListMap.containsKey(taskFlowId)) {
                List<TaskStatus> oldTaskStatusList = taskStatusListMap.get(taskFlowId);
                List<TaskStatus> newTaskStatusList = new ArrayList<>();
                for (TaskStatus taskStatus : oldTaskStatusList) {
                    if (!taskStatus.getTask().getId().equals(taskId)) {
                        newTaskStatusList.add(taskStatus);
                    }
                }
                if (oldTaskStatusList.size() != newTaskStatusList.size()) {
                    taskStatusListMap.get(taskFlowId).clear();
                    taskStatusListMap.get(taskFlowId).addAll(newTaskStatusList);
                }
                if (newTaskStatusList.size() == 0) {
                    taskStatusListMap.remove(taskFlowId);
                }
            }
        }
    }
}
