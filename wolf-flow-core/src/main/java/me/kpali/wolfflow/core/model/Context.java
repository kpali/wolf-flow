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
        if (value != null) {
            return clazz.cast(value);
        }
        return null;
    }

    public <T> List<T> getList(String key, Class<T> clazz) {
        Object value = this.get(key);
        if (value != null) {
            List<T> result = new ArrayList<T>();
            if (value instanceof List<?>) {
                for (Object o : (List<?>) value) {
                    result.add(clazz.cast(o));
                }
                return result;
            }
        }
        return null;
    }
}
