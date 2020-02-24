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
    public String getString(String key) {
        Object value = this.get(key);
        return (value == null) ? null : (String)value;
    }

    public Integer getInteger(String key) {
        Object value = this.get(key);
        return (value == null) ? null : (Integer)value;
    }

    public Long getLong(String key) {
        Object value = this.get(key);
        return (value == null) ? null : (Long)value;
    }

    public Float getFloat(String key) {
        Object value = this.get(key);
        return (value == null) ? null : (Float)value;
    }

    public Double getDouble(String key) {
        Object value = this.get(key);
        return (value == null) ? null : (Double)value;
    }

    public <T> List<T> castList(Object obj, Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return null;
    }

    public List<String> getStringList(String key) {
        Object value = this.get(key);
        return (value == null) ? null : castList(value, String.class);
    }

    public List<Integer> getIntegerList(String key) {
        Object value = this.get(key);
        return (value == null) ? null : castList(value, Integer.class);
    }

    public List<Long> getLongList(String key) {
        Object value = this.get(key);
        return (value == null) ? null : castList(value, Long.class);
    }

    public List<Float> getFloatList(String key) {
        Object value = this.get(key);
        return (value == null) ? null : castList(value, Float.class);
    }

    public List<Double> getDoubleList(String key) {
        Object value = this.get(key);
        return (value == null) ? null : castList(value, Double.class);
    }
}
