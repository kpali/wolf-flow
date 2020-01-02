package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 任务之间的连接
 *
 * @author kpali
 */
public class Link implements Serializable {
    private static final long serialVersionUID = -4190148088560825591L;

    private Long source;
    private Long target;

    public Long getSource() {
        return source;
    }

    public void setSource(Long source) {
        this.source = source;
    }

    public Long getTarget() {
        return target;
    }

    public void setTarget(Long target) {
        this.target = target;
    }
}
