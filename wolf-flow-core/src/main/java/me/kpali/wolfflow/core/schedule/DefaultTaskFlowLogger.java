package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.model.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.util.SystemTime;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 任务流日志器的默认实现
 *
 * @author kpali
 */
public class DefaultTaskFlowLogger implements ITaskFlowLogger {
    private ConcurrentHashMap<Long, TaskFlowLog> taskFlowLogMap = new ConcurrentHashMap<>();
    private Object lock = new Object();

    @Override
    public Long insert(TaskFlowLog taskFlowLog) {
        if (taskFlowLog.getTaskFlowId() == null) {
            throw new NullPointerException("任务流ID不能为空");
        }
        if (StringUtils.isBlank(taskFlowLog.getStatus())) {
            throw new NullPointerException("任务流状态不能为空");
        }
        Long taskFlowLogId = SystemTime.getUniqueTime();
        taskFlowLog.setId(taskFlowLogId);
        this.taskFlowLogMap.put(taskFlowLogId, taskFlowLog);
        return taskFlowLogId;
    }

    @Override
    public void update(TaskFlowLog taskFlowLog) {
        if (taskFlowLog.getId() == null) {
            throw new NullPointerException("任务流日志ID不能为空");
        }
        synchronized (lock) {
            if (!this.taskFlowLogMap.containsKey(taskFlowLog.getId())) {
                throw new IllegalArgumentException("指定的任务流日志不存在");
            }
            this.taskFlowLogMap.put(taskFlowLog.getId(), taskFlowLog);
        }
    }

    @Override
    public List<TaskFlowLog> listUnfinishedLog() {
        List<TaskFlowLog> taskFlowLogList = new ArrayList<>(taskFlowLogMap.values());
        return taskFlowLogList.stream().filter(taskFlowLog -> {
            String status = taskFlowLog.getStatus();
            return (!TaskFlowStatusEnum.TRIGGER_FAIL.getCode().equals(status)
                    && !TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode().equals(status)
                    && !TaskFlowStatusEnum.EXECUTE_FAIL.getCode().equals(status));
        }).collect(Collectors.toList());
    }

    @Override
    public void delete(Long taskFlowLogId) {
        this.taskFlowLogMap.remove(taskFlowLogId);
    }
}
