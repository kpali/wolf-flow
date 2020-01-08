package me.kpali.wolfflow.core.schedule;

import org.springframework.stereotype.Component;

/**
 * 任务流扫描器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskFlowScaner implements ITaskFlowScaner {
    @Override
    public boolean tryLock() {
        return true;
    }
}
