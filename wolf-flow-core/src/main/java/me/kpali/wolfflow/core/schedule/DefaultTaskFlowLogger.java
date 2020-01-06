package me.kpali.wolfflow.core.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.model.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.util.SystemTime;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
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
    private final Object lock = new Object();

    @Override
    public Long insert(TaskFlowLog taskFlowLog) {
        if (taskFlowLog.getTaskFlowId() == null) {
            throw new NullPointerException("任务流ID不能为空");
        }
        if (StringUtils.isBlank(taskFlowLog.getStatus())) {
            throw new NullPointerException("任务流状态不能为空");
        }
        String json = JSON.toJSONString(taskFlowLog);
        TaskFlowLog taskFlowLogCloned = JSON.parseObject(json, TaskFlowLog.class);
        Long taskFlowLogId = SystemTime.getUniqueTime();
        taskFlowLogCloned.setId(taskFlowLogId);
        this.taskFlowLogMap.put(taskFlowLogId, taskFlowLogCloned);
        return taskFlowLogId;
    }

    @Override
    public TaskFlowLog select(Long taskFlowLogId) {
        synchronized (lock) {
            if (!this.taskFlowLogMap.containsKey(taskFlowLogId)) {
                return null;
            }
            TaskFlowLog taskFlowLog = this.taskFlowLogMap.get(taskFlowLogId);
            String json = JSON.toJSONString(taskFlowLog);
            TaskFlowLog taskFlowLogCloned = JSON.parseObject(json, TaskFlowLog.class);
            return taskFlowLogCloned;
        }
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
            String json = JSON.toJSONString(taskFlowLog);
            TaskFlowLog taskFlowLogCloned = JSON.parseObject(json, TaskFlowLog.class);
            this.taskFlowLogMap.put(taskFlowLogCloned.getId(), taskFlowLogCloned);
        }
    }

    @Override
    public List<TaskFlowLog> list() {
        List<TaskFlowLog> taskFlowLogList = new ArrayList<>(taskFlowLogMap.values());
        String json = JSON.toJSONString(taskFlowLogList);
        Type type = new TypeReference<List<TaskFlowLog>>() {
        }.getType();
        List<TaskFlowLog> taskFlowLogListCloned = JSON.parseObject(json, type);
        return taskFlowLogListCloned;
    }

    @Override
    public List<TaskFlowLog> listUnfinishedLog() {
        List<TaskFlowLog> taskFlowLogList = this.list();
        List<TaskFlowLog> unfinishedLogList = taskFlowLogList.stream().filter(taskFlowLog -> {
            String status = taskFlowLog.getStatus();
            return (!TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode().equals(status)
                    && !TaskFlowStatusEnum.EXECUTE_FAIL.getCode().equals(status));
        }).collect(Collectors.toList());
        String json = JSON.toJSONString(unfinishedLogList);
        Type type = new TypeReference<List<TaskFlowLog>>() {
        }.getType();
        List<TaskFlowLog> unfinishedLogListCloned = JSON.parseObject(json, type);
        return unfinishedLogListCloned;
    }

    @Override
    public void delete(Long taskFlowLogId) {
        this.taskFlowLogMap.remove(taskFlowLogId);
    }

    @Override
    public void clear() {
        this.taskFlowLogMap.clear();
    }
}
