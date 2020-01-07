package me.kpali.wolfflow.core.schedule;

/**
 * 任务流扫描器的默认实现
 *
 * @author kpali
 */
public class DefaultTaskFlowScaner implements ITaskFlowScaner {
    @Override
    public boolean tryLock() {
        return true;
    }
}
