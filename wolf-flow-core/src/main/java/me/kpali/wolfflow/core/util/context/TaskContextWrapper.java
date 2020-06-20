package me.kpali.wolfflow.core.util.context;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务上下文包装类
 *
 * @author kpali
 */
public class TaskContextWrapper extends ContextWrapper {
    public TaskContextWrapper() {
        super();
    }

    public TaskContextWrapper(ConcurrentHashMap<String, Object> context) {
        super(context);
    }
}
