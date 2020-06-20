package me.kpali.wolfflow.core.util.context;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 传递上下文包装类
 *
 * @author kpali
 */
public class DeliveryContextWrapper extends ContextWrapper {
    public DeliveryContextWrapper() {
        super();
    }

    public DeliveryContextWrapper(ConcurrentHashMap<String, Object> context) {
        super(context);
    }
}
