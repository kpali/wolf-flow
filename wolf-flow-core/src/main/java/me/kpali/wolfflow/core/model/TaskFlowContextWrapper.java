package me.kpali.wolfflow.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务流上下文包装类
 *
 * @author kpali
 */
public class TaskFlowContextWrapper extends ContextWrapper {
    public TaskFlowContextWrapper() {
        super();
    }

    public TaskFlowContextWrapper(Map<String, Object> context) {
        super(context);
    }

    public Map<String, Object> getParams() {
        Object paramsObj = this.context.get(ContextKey.PARAMS);
        if (paramsObj == null) {
            return null;
        }
        return (Map<String, Object>) paramsObj;
    }

    public synchronized void setParams(Map<String, Object> params) {
        Object paramsObj = this.context.get(ContextKey.PARAMS);
        if (paramsObj == null) {
            this.context.put(ContextKey.PARAMS, params);
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
            this.context.put(ContextKey.PARAMS, params);
        }
        params.put(name, param);
    }

    public Map<String, Map<String, Object>> getTaskContexts() {
        Object taskContextObj = this.context.get(ContextKey.TASK_CONTEXTS);
        if (taskContextObj == null) {
            return null;
        }
        return (Map<String, Map<String, Object>>) taskContextObj;
    }

    public synchronized void setTaskContexts(Map<String, Map<String, Object>> taskContexts) {
        Object taskContextObj = this.context.get(ContextKey.TASK_CONTEXTS);
        if (taskContextObj == null) {
            this.context.put(ContextKey.TASK_CONTEXTS, taskContexts);
        } else {
            taskContextObj = taskContexts;
        }
    }

    public Map<String, Object> getTaskContext(String taskId) {
        Map<String, Map<String, Object>> taskContexts = this.getTaskContexts();
        if (taskContexts == null) {
            return null;
        }
        return taskContexts.get(taskId);
    }

    public synchronized void putTaskContext(String taskId, Map<String, Object> taskContext) {
        Map<String, Map<String, Object>> taskContexts = this.getTaskContexts();
        if (taskContexts == null) {
            taskContexts = new HashMap<>();
            this.context.put(ContextKey.TASK_CONTEXTS, taskContexts);
        }
        taskContexts.put(taskId, taskContext);
    }
}
