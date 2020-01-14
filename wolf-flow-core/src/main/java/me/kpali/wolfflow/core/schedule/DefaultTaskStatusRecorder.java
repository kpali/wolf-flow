package me.kpali.wolfflow.core.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import me.kpali.wolfflow.core.model.TaskStatus;
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
    public TaskStatus get(Long taskId) {
        List<TaskStatus> taskStatusListCloned;
        synchronized (lock) {
            List<TaskStatus> taskStatusList = new ArrayList<>();
            for (List<TaskStatus> list : taskStatusListMap.values()) {
                taskStatusList.addAll(list);
            }
            String json = JSON.toJSONString(taskStatusList);
            Type type = new TypeReference<List<TaskStatus>>() {
            }.getType();
            taskStatusListCloned = JSON.parseObject(json, type);
        }
        for (TaskStatus taskStatus : taskStatusListCloned) {
            if (taskStatus.getTask().getId().equals(taskId)) {
                return taskStatus;
            }
        }
        return null;
    }

    @Override
    public void put(TaskStatus taskStatus) {
        synchronized (lock) {
            if (!taskStatusListMap.containsKey(taskStatus.getTaskFlowId())) {
                taskStatusListMap.put(taskStatus.getTaskFlowId(), new ArrayList<>());
            }
            List<TaskStatus> taskStatusList = taskStatusListMap.get(taskStatus.getTaskFlowId());
            for (TaskStatus oldTaskStatus : taskStatusList) {
                if (oldTaskStatus.getTask().getId().equals(taskStatus.getTask().getId())) {
                    // 更新
                    oldTaskStatus.setTask(taskStatus.getTask());
                    oldTaskStatus.setTaskFlowId(taskStatus.getTaskFlowId());
                    oldTaskStatus.setContext(taskStatus.getContext());
                    oldTaskStatus.setStatus(taskStatus.getStatus());
                    oldTaskStatus.setMessage(taskStatus.getMessage());
                    return;
                }
            }
            // 新增
            taskStatusList.add(taskStatus);
        }
    }

    @Override
    public void remove(Long taskId) {
        synchronized (lock) {
            for (Long taskFlowId : taskStatusListMap.keySet()) {
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
            }
        }
    }
}
