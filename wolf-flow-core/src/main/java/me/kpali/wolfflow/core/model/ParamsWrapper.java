package me.kpali.wolfflow.core.model;

import java.util.Map;

/**
 * 任务流上下文参数包装类
 *
 * @author kpali
 */
public class ParamsWrapper extends ContextWrapper {
    public ParamsWrapper() {
        super();
    }

    public ParamsWrapper(Map<String, Object> context) {
        super(context);
    }
}
