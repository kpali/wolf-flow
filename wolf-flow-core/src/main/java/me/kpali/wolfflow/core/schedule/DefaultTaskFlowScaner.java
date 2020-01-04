package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlow;

import java.util.List;

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
    public void whenLockSuccess() {
        // 不做任何操作
    }

    @Override
    public void whenLockFail() {
        // 不做任何操作
    }

    @Override
    public void beforeScanning() {
        // 不做任何操作
    }

    @Override
    public List<TaskFlow> scanCronTaskFlow() {
        return null;
    }

    @Override
    public void whenJoinSchedule(TaskFlow taskFlow) {
        // 不做任何操作
    }

    @Override
    public void whenUpdateSchedule(TaskFlow taskFlow) {
        // 不做任何操作
    }

    @Override
    public void whenSheduleFail(TaskFlow taskFlow) {
        // 不做任何操作
    }

    @Override
    public void afterScanning() {
        // 不做任何操作
    }
}
