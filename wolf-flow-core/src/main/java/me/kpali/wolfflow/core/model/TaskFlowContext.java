package me.kpali.wolfflow.core.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务流上下文
 *
 * @author kpali
 */
public class TaskFlowContext extends ConcurrentHashMap<String, String> {
    private Map<String, String> params = new ConcurrentHashMap<>();
    private Map<Long, TaskContext> taskContexts = new ConcurrentHashMap<>();

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Map<Long, TaskContext> getTaskContexts() {
        return taskContexts;
    }

    public void setTaskContexts(Map<Long, TaskContext> taskContexts) {
        this.taskContexts = taskContexts;
    }
}
