package me.kpali.wolfflow.core.logger.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.exception.TaskFlowLogException;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.TaskFlowLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 任务流日志器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskFlowLogger implements ITaskFlowLogger {
    private Map<Long, TaskFlowLog> taskFlowLogMap = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ITaskLogger taskLogger;

    @Override
    public List<TaskFlowLog> list(Long taskFlowId) throws TaskFlowLogException {
        try {
            List<TaskFlowLog> taskFlowLogList = taskFlowLogMap.values().stream().filter(taskFlowLog ->
                taskFlowId.equals(taskFlowLog.getTaskFlowId())
            ).collect(Collectors.toList());
            String json = objectMapper.writeValueAsString(taskFlowLogList);
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, TaskFlowLog.class);
            List<TaskFlowLog> taskFlowLogListCloned = objectMapper.readValue(json, javaType);
            return taskFlowLogListCloned;
        } catch (JsonProcessingException e) {
            throw new TaskFlowLogException(e);
        }
    }

    @Override
    public TaskFlowLog get(Long logId) throws TaskFlowLogException {
        TaskFlowLog taskFlowLogCloned = null;
        if (taskFlowLogMap.containsKey(logId)) {
            TaskFlowLog taskFlowLog = taskFlowLogMap.get(logId);
            try {
                String json = objectMapper.writeValueAsString(taskFlowLog);
                taskFlowLogCloned = objectMapper.readValue(json, TaskFlowLog.class);
            } catch (JsonProcessingException e) {
                throw new TaskFlowLogException(e);
            }
        }
        return taskFlowLogCloned;
    }

    @Override
    public TaskFlowLog last(Long taskFlowId) throws TaskFlowLogException {
        List<TaskFlowLog> taskFlowLogList = taskFlowLogMap.values().stream().filter(taskFlowLog ->
                taskFlowId.equals(taskFlowLog.getTaskFlowId())
        ).collect(Collectors.toList());
        TaskFlowLog lastTaskFlowLog = null;
        for (TaskFlowLog taskFlowLog : taskFlowLogList) {
            if (lastTaskFlowLog == null || taskFlowLog.getLogId() > lastTaskFlowLog.getLogId()) {
                lastTaskFlowLog = taskFlowLog;
            }
        }
        TaskFlowLog taskFlowLogCloned = null;
        if (lastTaskFlowLog != null) {
            try {
                String json = objectMapper.writeValueAsString(lastTaskFlowLog);
                taskFlowLogCloned = objectMapper.readValue(json, TaskFlowLog.class);
            } catch (JsonProcessingException e) {
                throw new TaskFlowLogException(e);
            }
        }
        return taskFlowLogCloned;
    }

    @Override
    public void put(TaskFlowLog taskFlowLog) throws TaskFlowLogException {
        try {
            String json = objectMapper.writeValueAsString(taskFlowLog);
            TaskFlowLog taskFlowLogCloned = objectMapper.readValue(json, TaskFlowLog.class);
            Date now = new Date();
            taskFlowLogCloned.setCreationTime(now);
            taskFlowLogCloned.setUpdateTime(now);
            TaskFlowLog existsTaskFlowLog = taskFlowLogMap.get(taskFlowLogCloned.getLogId());
            if (existsTaskFlowLog != null) {
                taskFlowLogCloned.setCreationTime(existsTaskFlowLog.getCreationTime());
            }
            taskFlowLogMap.put(taskFlowLogCloned.getLogId(), taskFlowLogCloned);
        } catch (JsonProcessingException e) {
            throw new TaskFlowLogException(e);
        }
    }

    @Override
    public void delete(Long logId) throws TaskFlowLogException {
        taskFlowLogMap.remove(logId);
        taskLogger.delete(logId);
    }

    @Override
    public void deleteByTaskFlowId(Long taskFlowId) throws TaskFlowLogException {
        List<TaskFlowLog> taskFlowLogList = taskFlowLogMap.values().stream().filter(taskFlowLog ->
                taskFlowId.equals(taskFlowLog.getTaskFlowId())
        ).collect(Collectors.toList());
        for (TaskFlowLog taskFlowLog : taskFlowLogList) {
            taskFlowLogMap.remove(taskFlowLog.getLogId());
            taskLogger.delete(taskFlowLog.getLogId());
        }
    }

    @Override
    public boolean isInProgress(Long logId) throws TaskFlowLogException {
        TaskFlowLog taskFlowLog = this.get(logId);
        return (taskFlowLog != null && this.isInProgress(taskFlowLog));
    }

    @Override
    public boolean isInProgress(TaskFlowLog taskFlowLog) throws TaskFlowLogException {
        return !TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskFlowLog.getStatus())
                && !TaskFlowStatusEnum.EXECUTE_FAILURE.getCode().equals(taskFlowLog.getStatus());
    }
}
