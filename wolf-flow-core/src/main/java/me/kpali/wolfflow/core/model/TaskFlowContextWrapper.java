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

    public Map<String, Object> getTaskFlowContext() {
        Map<String, Object> taskFlowContext = null;
        if (this.getContext() != null) {
            taskFlowContext = new HashMap<>();
            for (String key : this.getContext().keySet()) {
                if (ContextKey.TASK_CONTEXTS.equals(key)) {
                    continue;
                }
                taskFlowContext.put(key, this.getContext().get(key));
            }
        }
        return taskFlowContext;
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

    public Object getParam(String key) {
        Map<String, Object> params = this.getParams();
        if (params == null) {
            return null;
        }
        return params.get(key);
    }

    public synchronized void putParam(String key, Object value) {
        Map<String, Object> params = this.getParams();
        if (params == null) {
            params = new HashMap<>();
            this.context.put(ContextKey.PARAMS, params);
        }
        params.put(key, value);
    }

    public ParamsWrapper getParamsWrapper() {
        Map<String, Object> params = this.getParams();
        if (params == null) {
            return null;
        }
        return new ParamsWrapper(params);
    }

    public Map<String, Object> getDeliveryContext() {
        Object deliveryContextObj = this.context.get(ContextKey.DELIVERY_CONTEXT);
        if (deliveryContextObj == null) {
            return null;
        }
        return (Map<String, Object>) deliveryContextObj;
    }

    public synchronized void setDeliveryContext(Map<String, Object> deliveryContext) {
        Object deliveryContextObj = this.context.get(ContextKey.DELIVERY_CONTEXT);
        if (deliveryContextObj == null) {
            this.context.put(ContextKey.DELIVERY_CONTEXT, deliveryContext);
        } else {
            deliveryContextObj = deliveryContext;
        }
    }

    public Object getDeliveryContext(String key) {
        Map<String, Object> deliveryContext = this.getDeliveryContext();
        if (deliveryContext == null) {
            return null;
        }
        return deliveryContext.get(key);
    }

    public synchronized void putDeliveryContext(String key, Object value) {
        Map<String, Object> deliveryContext = this.getDeliveryContext();
        if (deliveryContext == null) {
            deliveryContext = new HashMap<>();
            this.context.put(ContextKey.DELIVERY_CONTEXT, deliveryContext);
        }
        deliveryContext.put(key, value);
    }

    public DeliveryContextWrapper getDeliveryContextWrapper() {
        Map<String, Object> deliveryContext = this.getDeliveryContext();
        if (deliveryContext == null) {
            return null;
        }
        return new DeliveryContextWrapper(deliveryContext);
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

    public TaskContextWrapper getTaskContextWrapper(String taskId) {
        Map<String, Map<String, Object>> taskContexts = this.getTaskContexts();
        if (taskContexts == null) {
            return null;
        }
        Map<String, Object> taskContext = taskContexts.get(taskId);
        if (taskContext == null) {
            return null;
        }
        return new TaskContextWrapper(taskContext);
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
