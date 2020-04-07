package me.kpali.wolfflow.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文
 *
 * @author kpali
 */
public class Context extends ConcurrentHashMap<String, Object> {
    public <T> T getValue(String key, Class<T> clazz) {
        Object value = this.get(key);
        return this.cast(value, clazz);
    }

    public <T> List<T> getList(String key, Class<T> clazz) {
        Object value = this.get(key);
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
