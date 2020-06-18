package me.kpali.wolfflow.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上下文包装类
 *
 * @author kpali
 */
public class ContextWrapper {
    public ContextWrapper() {
        this.context = new HashMap<>();
    }

    public ContextWrapper(Map<String, Object> context) {
        this.context = (context == null ? new HashMap<>() : context);
    }

    protected Map<String, Object> context;

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Object get(String key) {
        return this.context.get(key);
    }

    public Object put(String key, Object value) {
        return this.context.put(key, value);
    }

    public boolean containsKey(String key) {
        return this.context.containsKey(key);
    }

    public <T> T getValue(String key, Class<T> clazz) {
        Object value = this.context.get(key);
        return this.cast(value, clazz);
    }

    public <T> List<T> getList(String key, Class<T> clazz) {
        Object value = this.context.get(key);
        if (value != null) {
            List<T> result = new ArrayList<T>();
            if (value instanceof List<?>) {
                for (Object o : (List<?>) value) {
                    result.add(this.cast(o, clazz));
                }
                return result;
            }
        }
        return null;
    }

    private <T> T cast(Object obj, Class<T> clazz) {
        if (obj != null) {
            if (!clazz.isInstance(obj) && obj instanceof Integer) {
                // Integer类型 转 Long类型
                Long longObj = ((Integer) obj).longValue();
                if (clazz.isInstance(longObj)) {
                    return clazz.cast(longObj);
                }
            }
            return clazz.cast(obj);
        }
        return null;
    }
}
