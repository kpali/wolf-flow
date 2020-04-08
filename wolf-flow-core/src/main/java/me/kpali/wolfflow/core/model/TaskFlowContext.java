package me.kpali.wolfflow.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务流上下文
 *
 * @author kpali
 */
public class TaskFlowContext extends Context {
    public Map<String, Object> getParams() {
        Object paramsObj = this.get(ContextKey.PARAMS);
        if (paramsObj == null) {
            return null;
        }
        return (Map<String, Object>) paramsObj;
    }

    public synchronized void setParams(Map<String, Object> params) {
        Object paramsObj = this.get(ContextKey.PARAMS);
        if (paramsObj == null) {
            this.put(ContextKey.PARAMS, params);
        } else {
            paramsObj = params;
        }
    }

    public Object getParam(String name) {
        Map<String, Object> params = this.getParams();
        if (params == null) {
            return null;
        }
        return params.get(name);
    }

    public synchronized void putParam(String name, Object param) {
        Map<String, Object> params = this.getParams();
        if (params == null) {
            params = new HashMap<>();
            this.put(ContextKey.PARAMS, params);
        }
        params.put(name, param);
    }

    public Map<Long, TaskContext> getTaskContexts() {
        Object taskContextObj = this.get(ContextKey.TASK_CONTEXTS);
        if (taskContextObj == null) {
            return null;
        }
        return (Map<Long, TaskContext>) taskContextObj;
    }

    public synchronized void setTaskContexts(Map<Long, TaskContext> taskContexts) {
        Object taskContextObj = this.get(ContextKey.TASK_CONTEXTS);
        if (taskContextObj == null) {
            this.put(ContextKey.TASK_CONTEXTS, taskContexts);
        } else {
            taskContextObj = taskContexts;
        }
    }

    public TaskContext getTaskContext(Long taskId) {
        Map<Long, TaskContext> taskContexts = this.getTaskContexts();
        if (taskContexts == null) {
            return null;
        }
        return taskContexts.get(taskId);
    }

    public synchronized void putTaskContext(Long taskId, TaskContext taskContext) {
        Map<Long, TaskContext> taskContexts = this.getTaskContexts();
        if (taskContexts == null) {
            taskContexts = new HashMap<>();
            this.put(ContextKey.TASK_CONTEXTS, taskContexts);
        }
        taskContexts.put(taskId, taskContext);
    }
}
