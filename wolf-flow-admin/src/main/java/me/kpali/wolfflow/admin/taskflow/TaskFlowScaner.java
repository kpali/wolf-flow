package me.kpali.wolfflow.admin.taskflow;

import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.schedule.ITaskFlowScaner;

import java.util.ArrayList;
import java.util.List;

public class TaskFlowScaner implements ITaskFlowScaner {

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public void beforeScanning() {

    }

    @Override
    public List<TaskFlow> scan() {
        return new ArrayList<>();
    }

    @Override
    public void afterScanning() {

    }
}
