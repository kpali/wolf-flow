package me.kpali.wolfflow.core.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import me.kpali.wolfflow.core.model.TaskStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
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

    @Override
    public List<TaskStatus> listByTaskFlowId(Long taskFlowId) {
        synchronized (lock) {
            List<TaskStatus> taskStatusList = new ArrayList<>();
            if (taskStatusListMap.containsKey(taskFlowId)) {
                taskStatusList = taskStatusListMap.get(taskFlowId);
            }
            String json = JSON.toJSONString(taskStatusList);
            Type type = new TypeReference<List<TaskStatus>>() {
            }.getType();
            List<TaskStatus> taskStatusListCloned = JSON.parseObject(json, type);
            return taskStatusListCloned;
        }
    }

    @Override
    public TaskStatus get(Long taskFlowId, Long taskId) {
        synchronized (lock) {
            TaskStatus taskStatusCloned = null;
            if (taskStatusListMap.containsKey(taskFlowId)) {
                List<TaskStatus> taskStatusList = taskStatusListMap.get(taskFlowId);
                for (TaskStatus taskStatus : taskStatusList) {
                    if (taskStatus.getTask().getId().equals(taskId)) {
                        String json = JSON.toJSONString(taskStatus);
                        Type type = new TypeReference<TaskStatus>() {
                        }.getType();
                        taskStatusCloned = JSON.parseObject(json, type);
                        break;
                    }
                }
            }
            return taskStatusCloned;
        }
    }

    @Override
    public void put(TaskStatus taskStatus) {
        synchronized (lock) {
            String json = JSON.toJSONString(taskStatus);
            Type type = new TypeReference<TaskStatus>() {
            }.getType();
            TaskStatus taskStatusCloned = JSON.parseObject(json, type);
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
    public void remove(Long taskFlowId, Long taskId) {
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
