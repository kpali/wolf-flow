package me.kpali.wolfflow.core.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务流上下文
 *
 * @author kpali
 */
public class TaskFlowContext extends ConcurrentHashMap<String, String> {
    private Map<Long, TaskContext> taskContextMap = new ConcurrentHashMap<>();

    public Map<Long, TaskContext> getTaskContextMap() {
        return taskContextMap;
    }

    public void setTaskContextMap(Map<Long, TaskContext> taskContextMap) {
        this.taskContextMap = taskContextMap;
    }
}
