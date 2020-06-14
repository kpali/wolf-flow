package me.kpali.wolfflow.core.model;

import java.util.Map;

/**
 * 投递上下文包装类
 *
 * @author kpali
 */
public class DeliveryContextWrapper extends ContextWrapper {
    public DeliveryContextWrapper() {
        super();
    }

    public DeliveryContextWrapper(Map<String, Object> context) {
        super(context);
    }
}
