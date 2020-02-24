package me.kpali.wolfflow.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务流上下文
 *
 * @author kpali
 */
public class TaskFlowContext extends Context {
    private Map<String, Object> params = new HashMap<>();
    private ConcurrentHashMap<Long, TaskContext> taskContexts = new ConcurrentHashMap<>();

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public ConcurrentHashMap<Long, TaskContext> getTaskContexts() {
        return taskContexts;
    }

    public void setTaskContexts(ConcurrentHashMap<Long, TaskContext> taskContexts) {
        this.taskContexts = taskContexts;
    }
}
