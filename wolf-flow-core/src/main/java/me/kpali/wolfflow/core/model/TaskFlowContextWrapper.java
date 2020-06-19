package me.kpali.wolfflow.core.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务流上下文包装类
 *
 * @author kpali
 */
public class TaskFlowContextWrapper extends ContextWrapper {
    public TaskFlowContextWrapper() {
        super();
    }

    public TaskFlowContextWrapper(ConcurrentHashMap<String, Object> context) {
        super(context);
    }

    public ConcurrentHashMap<String, Object> getTaskFlowContext() {
        ConcurrentHashMap<String, Object> taskFlowContext = null;
        if (this.getContext() != null) {
            taskFlowContext = new ConcurrentHashMap<>();
            for (String key : this.getContext().keySet()) {
                if (ContextKey.TASK_CONTEXTS.equals(key)) {
                    continue;
                }
                taskFlowContext.put(key, this.getContext().get(key));
            }
        }
        return taskFlowContext;
    }

    public ConcurrentHashMap<String, Object> getParams() {
        Object paramsObj = this.context.get(ContextKey.PARAMS);
        if (paramsObj == null) {
            return null;
        }
        return (ConcurrentHashMap<String, Object>) paramsObj;
    }

    public synchronized void setParams(ConcurrentHashMap<String, Object> params) {
        Object paramsObj = this.context.get(ContextKey.PARAMS);
        if (paramsObj == null) {
            this.context.put(ContextKey.PARAMS, params);
        } else {
            paramsObj = params;
        }
    }

    public Object getParam(String key) {
        ConcurrentHashMap<String, Object> params = this.getParams();
        if (params == null) {
            return null;
        }
        return params.get(key);
    }

    public synchronized void putParam(String key, Object value) {
        ConcurrentHashMap<String, Object> params = this.getParams();
        if (params == null) {
            params = new ConcurrentHashMap<>();
            this.context.put(ContextKey.PARAMS, params);
        }
        params.put(key, value);
    }

    public ParamsWrapper getParamsWrapper() {
        ConcurrentHashMap<String, Object> params = this.getParams();
        if (params == null) {
            return null;
        }
        return new ParamsWrapper(params);
    }

    public ConcurrentHashMap<String, Object> getDeliveryContext() {
        Object deliveryContextObj = this.context.get(ContextKey.DELIVERY_CONTEXT);
        if (deliveryContextObj == null) {
            return null;
        }
        return (ConcurrentHashMap<String, Object>) deliveryContextObj;
    }

    public synchronized void setDeliveryContext(ConcurrentHashMap<String, Object> deliveryContext) {
        Object deliveryContextObj = this.context.get(ContextKey.DELIVERY_CONTEXT);
        if (deliveryContextObj == null) {
            this.context.put(ContextKey.DELIVERY_CONTEXT, deliveryContext);
        } else {
            deliveryContextObj = deliveryContext;
        }
    }

    public Object getDeliveryContext(String key) {
        ConcurrentHashMap<String, Object> deliveryContext = this.getDeliveryContext();
        if (deliveryContext == null) {
            return null;
        }
        return deliveryContext.get(key);
    }

    public synchronized void putDeliveryContext(String key, Object value) {
        ConcurrentHashMap<String, Object> deliveryContext = this.getDeliveryContext();
        if (deliveryContext == null) {
            deliveryContext = new ConcurrentHashMap<>();
            this.context.put(ContextKey.DELIVERY_CONTEXT, deliveryContext);
        }
        deliveryContext.put(key, value);
    }

    public DeliveryContextWrapper getDeliveryContextWrapper() {
        ConcurrentHashMap<String, Object> deliveryContext = this.getDeliveryContext();
        if (deliveryContext == null) {
            return null;
        }
        return new DeliveryContextWrapper(deliveryContext);
    }

    public Map<String, ConcurrentHashMap<String, Object>> getTaskContexts() {
        Object taskContextObj = this.context.get(ContextKey.TASK_CONTEXTS);
        if (taskContextObj == null) {
            return null;
        }
        return (Map<String, ConcurrentHashMap<String, Object>>) taskContextObj;
    }

    public synchronized void setTaskContexts(Map<String, ConcurrentHashMap<String, Object>> taskContexts) {
        Object taskContextObj = this.context.get(ContextKey.TASK_CONTEXTS);
        if (taskContextObj == null) {
            this.context.put(ContextKey.TASK_CONTEXTS, taskContexts);
        } else {
            taskContextObj = taskContexts;
        }
    }

    public TaskContextWrapper getTaskContextWrapper(String taskId) {
        Map<String, ConcurrentHashMap<String, Object>> taskContexts = this.getTaskContexts();
        if (taskContexts == null) {
            return null;
        }
        ConcurrentHashMap<String, Object> taskContext = taskContexts.get(taskId);
        if (taskContext == null) {
            return null;
        }
        return new TaskContextWrapper(taskContext);
    }

    public ConcurrentHashMap<String, Object> getTaskContext(String taskId) {
        Map<String, ConcurrentHashMap<String, Object>> taskContexts = this.getTaskContexts();
        if (taskContexts == null) {
            return null;
        }
        return taskContexts.get(taskId);
    }

    public synchronized void putTaskContext(String taskId, ConcurrentHashMap<String, Object> taskContext) {
        Map<String, ConcurrentHashMap<String, Object>> taskContexts = this.getTaskContexts();
        if (taskContexts == null) {
            taskContexts = new ConcurrentHashMap<>();
            this.context.put(ContextKey.TASK_CONTEXTS, taskContexts);
        }
        taskContexts.put(taskId, taskContext);
    }
}
