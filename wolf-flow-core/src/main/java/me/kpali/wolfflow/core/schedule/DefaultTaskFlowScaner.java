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

    @Override
    public void beforeScanning() {
        // 不做任何操作
    }

    @Override
    public void afterScanning() {
        // 不做任何操作
    }
}
