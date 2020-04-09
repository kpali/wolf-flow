package me.kpali.wolfflow.core.model;

import java.util.Map;

/**
 * 任务上下文包装类
 *
 * @author kpali
 */
public class TaskContextWrapper extends ContextWrapper {
    public TaskContextWrapper() {
        super();
    }

    public TaskContextWrapper(Map<String, Object> context) {
        super(context);
    }
}
