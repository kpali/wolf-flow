package me.kpali.wolfflow.core.logger.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.exception.TaskFlowLogException;
import me.kpali.wolfflow.core.exception.TaskLogException;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.TaskFlowLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务流日志器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskFlowLogger implements ITaskFlowLogger {
    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskFlowLogger.class);

    private Map<Long, TaskFlowLog> taskFlowLogId_to_taskFlowLog = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ITaskLogger taskLogger;
    
    private static final Object LOCK = new Object();

    @Override
    public List<TaskFlowLog> list(Long taskFlowId) throws TaskFlowLogException {
        synchronized (LOCK) {
            try {
                List<TaskFlowLog> taskFlowLogList = taskFlowLogId_to_taskFlowLog.values().stream().filter(taskFlowLog ->
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
    }

    @Override
    public TaskFlowLog get(Long taskFlowLogId) throws TaskFlowLogException {
        synchronized (LOCK) {
            TaskFlowLog taskFlowLogCloned = null;
            if (taskFlowLogId_to_taskFlowLog.containsKey(taskFlowLogId)) {
                TaskFlowLog taskFlowLog = taskFlowLogId_to_taskFlowLog.get(taskFlowLogId);
                try {
                    String json = objectMapper.writeValueAsString(taskFlowLog);
                    taskFlowLogCloned = objectMapper.readValue(json, TaskFlowLog.class);
                } catch (JsonProcessingException e) {
                    throw new TaskFlowLogException(e);
                }
            }
            return taskFlowLogCloned;
        }
    }

    @Override
    public TaskFlowLog last(Long taskFlowId) throws TaskFlowLogException {
        synchronized (LOCK) {
            List<TaskFlowLog> taskFlowLogList = taskFlowLogId_to_taskFlowLog.values().stream().filter(taskFlowLog ->
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
    }

    @Override
    public void add(TaskFlowLog taskFlowLog) throws TaskFlowLogException {
        this.put(taskFlowLog);
    }

    @Override
    public void update(TaskFlowLog taskFlowLog) throws TaskFlowLogException {
        this.put(taskFlowLog);
    }

    private void put(TaskFlowLog taskFlowLog) throws TaskFlowLogException {
        synchronized (LOCK) {
            try {
                String json = objectMapper.writeValueAsString(taskFlowLog);
                TaskFlowLog taskFlowLogCloned = objectMapper.readValue(json, TaskFlowLog.class);
                Date now = new Date();
                taskFlowLogCloned.setCreationTime(now);
                taskFlowLogCloned.setUpdateTime(now);
                TaskFlowLog existsTaskFlowLog = taskFlowLogId_to_taskFlowLog.get(taskFlowLogCloned.getLogId());
                if (existsTaskFlowLog != null) {
                    taskFlowLogCloned.setCreationTime(existsTaskFlowLog.getCreationTime());
                }
                taskFlowLogId_to_taskFlowLog.put(taskFlowLogCloned.getLogId(), taskFlowLogCloned);
            } catch (JsonProcessingException e) {
                throw new TaskFlowLogException(e);
            }
        }
    }

    @Override
    public void delete(Long taskFlowLogId) throws TaskFlowLogException {
        synchronized (LOCK) {
            taskFlowLogId_to_taskFlowLog.remove(taskFlowLogId);
            try {
                taskLogger.deleteByTaskFlowLogId(taskFlowLogId);
            } catch (TaskLogException e) {
                throw new TaskFlowLogException(e);
            }
        }
    }

    @Override
    public void deleteByTaskFlowId(Long taskFlowId) throws TaskFlowLogException {
        synchronized (LOCK) {
            List<TaskFlowLog> taskFlowLogList = taskFlowLogId_to_taskFlowLog.values().stream().filter(taskFlowLog ->
                    taskFlowId.equals(taskFlowLog.getTaskFlowId())
            ).collect(Collectors.toList());
            for (TaskFlowLog taskFlowLog : taskFlowLogList) {
                taskFlowLogId_to_taskFlowLog.remove(taskFlowLog.getLogId());
                try {
                    taskLogger.deleteByTaskFlowLogId(taskFlowLog.getLogId());
                } catch (TaskLogException e) {
                    throw new TaskFlowLogException(e);
                }
            }
        }
    }

    @Override
    public boolean isInProgress(Long taskFlowLogId) throws TaskFlowLogException {
        TaskFlowLog taskFlowLog = this.get(taskFlowLogId);
        return (taskFlowLog != null && this.isInProgress(taskFlowLog));
    }

    @Override
    public boolean isInProgress(TaskFlowLog taskFlowLog) throws TaskFlowLogException {
        return !TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskFlowLog.getStatus())
                && !TaskFlowStatusEnum.EXECUTE_FAILURE.getCode().equals(taskFlowLog.getStatus())
                && !TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode().equals(taskFlowLog.getStatus())
                && !TaskFlowStatusEnum.ROLLBACK_FAILURE.getCode().equals(taskFlowLog.getStatus());
    }
}
