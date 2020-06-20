package me.kpali.wolfflow.core.util.context;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务流上下文参数包装类
 *
 * @author kpali
 */
public class ParamsWrapper extends ContextWrapper {
    public ParamsWrapper() {
        super();
    }

    public ParamsWrapper(ConcurrentHashMap<String, Object> context) {
        super(context);
    }
}
