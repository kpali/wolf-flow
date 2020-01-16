package me.kpali.wolfflow.core.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import me.kpali.wolfflow.core.model.TaskFlowStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
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

    @Override
    public List<TaskFlowStatus> list() {
        synchronized (lock) {
            List<TaskFlowStatus> taskFlowStatusList = new ArrayList<>(taskFlowStatusMap.values());
            String json = JSON.toJSONString(taskFlowStatusList);
            Type type = new TypeReference<List<TaskFlowStatus>>() {
            }.getType();
            List<TaskFlowStatus> taskFlowStatusListCloned = JSON.parseObject(json, type);
            return taskFlowStatusListCloned;
        }
    }

    @Override
    public TaskFlowStatus get(Long taskFlowId) {
        synchronized (lock) {
            TaskFlowStatus taskFlowStatusCloned = null;
            if (taskFlowStatusMap.containsKey(taskFlowId)) {
                TaskFlowStatus taskFlowStatus = taskFlowStatusMap.get(taskFlowId);
                String json = JSON.toJSONString(taskFlowStatus);
                Type type = new TypeReference<TaskFlowStatus>() {
                }.getType();
                taskFlowStatusCloned = JSON.parseObject(json, type);
            }
            return taskFlowStatusCloned;
        }
    }

    @Override
    public void put(TaskFlowStatus taskFlowStatus) {
        synchronized (lock) {
            String json = JSON.toJSONString(taskFlowStatus);
            Type type = new TypeReference<TaskFlowStatus>() {
            }.getType();
            TaskFlowStatus taskFlowStatusCloned = JSON.parseObject(json, type);
            taskFlowStatusMap.put(taskFlowStatusCloned.getTaskFlow().getId(), taskFlowStatusCloned);
        }
    }

    @Override
    public void remove(Long taskFlowId) {
        synchronized (lock) {
            taskFlowStatusMap.remove(taskFlowId);
        }
    }
}
