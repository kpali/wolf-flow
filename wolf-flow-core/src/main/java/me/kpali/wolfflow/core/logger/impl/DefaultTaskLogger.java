package me.kpali.wolfflow.core.logger.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.enums.TaskStatusEnum;
import me.kpali.wolfflow.core.exception.TaskLogException;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.TaskLog;
import me.kpali.wolfflow.core.model.TaskLogLine;
import me.kpali.wolfflow.core.model.TaskLogResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务日志器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskLogger implements ITaskLogger {
    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskLogger.class);

    /**
     *  [任务流日志ID, [任务日志ID, 任务日志]]
     */
    private Map<Long, Map<Long, TaskLog>> taskFlowLogId_to_taskLogMap = new HashMap<>();
    /**
     * [任务ID, 任务状态]
     */
    private Map<Long, TaskLog> taskId_to_taskStatus = new HashMap<>();
    /**
     * [日志文件ID, 任务日志行列表]
     */
    private Map<String, List<TaskLogLine>> logFileId_to_taskLogLineList = new HashMap<>();
    private static final String NEWLINE = System.getProperty("line.separator");
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Object LOCK = new Object();

    @Override
    public List<TaskLog> list(Long taskFlowLogId) throws TaskLogException {
        synchronized (LOCK) {
            List<TaskLog> taskLogListCloned = new ArrayList<>();
            Map<Long, TaskLog> taskLogId_to_taskLog = taskFlowLogId_to_taskLogMap.get(taskFlowLogId);
            if (taskLogId_to_taskLog != null) {
                List<TaskLog> taskLogList = new ArrayList<>(taskLogId_to_taskLog.values());
                if (taskLogList != null) {
                    try {
                        String json = objectMapper.writeValueAsString(taskLogList);
                        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, TaskLog.class);
                        taskLogListCloned = objectMapper.readValue(json, javaType);
                    } catch (JsonProcessingException e) {
                        throw new TaskLogException(e);
                    }
                }
            }
            return taskLogListCloned;
        }
    }

    @Override
    public TaskLog get(Long taskFlowLogId, Long taskId) throws TaskLogException {
        synchronized (LOCK) {
            TaskLog taskLogCloned = null;
            Map<Long, TaskLog> taskLogId_to_taskLog = taskFlowLogId_to_taskLogMap.get(taskFlowLogId);
            if (taskLogId_to_taskLog != null) {
                List<TaskLog> taskLogList = new ArrayList<>(taskLogId_to_taskLog.values());
                if (taskLogList != null) {
                    for (TaskLog taskLog : taskLogList) {
                        if (taskLog.getTaskId().equals(taskId)) {
                            try {
                                String json = objectMapper.writeValueAsString(taskLog);
                                taskLogCloned = objectMapper.readValue(json, TaskLog.class);
                                break;
                            } catch (JsonProcessingException e) {
                                throw new TaskLogException(e);
                            }
                        }
                    }
                }
            }
            return taskLogCloned;
        }
    }

    @Override
    public void add(TaskLog taskLog) throws TaskLogException {
        this.put(taskLog);
    }

    @Override
    public void update(TaskLog taskLog) throws TaskLogException {
        this.put(taskLog);
    }

    private void put(TaskLog taskLog) throws TaskLogException {
        synchronized (LOCK) {
            try {
                String json = objectMapper.writeValueAsString(taskLog);
                TaskLog taskLogCloned = objectMapper.readValue(json, TaskLog.class);
                Map<Long, TaskLog> taskLogId_to_taskLog = taskFlowLogId_to_taskLogMap.computeIfAbsent(taskLogCloned.getTaskFlowLogId(), k -> new HashMap<>());
                TaskLog existsTaskLog = taskLogId_to_taskLog.get(taskLogCloned.getLogId());
                if (existsTaskLog != null) {
                    taskLogCloned.setCreationTime(existsTaskLog.getCreationTime());
                    BeanUtils.copyProperties(taskLogCloned, existsTaskLog);
                } else {
                    taskLogId_to_taskLog.put(taskLogCloned.getLogId(), taskLogCloned);
                }
                // 更新任务状态
                this.putTaskStatus(taskLogCloned);
            } catch (JsonProcessingException e) {
                throw new TaskLogException(e);
            }
        }
    }

    @Override
    public TaskLog getLastExecuteLog(Long taskId) throws TaskLogException {
        synchronized (LOCK) {
            TaskLog lastExecuteLogCloned = null;
            TaskLog lastExecuteLog = null;
            for (Map<Long, TaskLog> taskLogMap : taskFlowLogId_to_taskLogMap.values()) {
                for (TaskLog taskLog : taskLogMap.values()) {
                    if (!taskLog.getTaskId().equals(taskId) || taskLog.getRollback()) {
                        continue;
                    }
                    if (lastExecuteLog == null || taskLog.getLogId() > lastExecuteLog.getLogId()) {
                        lastExecuteLog = taskLog;
                    }
                }
            }
            if (lastExecuteLog != null) {
                try {
                    String json = objectMapper.writeValueAsString(lastExecuteLog);
                    lastExecuteLogCloned = objectMapper.readValue(json, TaskLog.class);
                } catch (JsonProcessingException e) {
                    throw new TaskLogException(e);
                }
            }
            return lastExecuteLogCloned;
        }
    }

    @Override
    public void deleteByTaskFlowLogId(Long taskFlowLogId) throws TaskLogException {
        synchronized (LOCK) {
            taskFlowLogId_to_taskLogMap.remove(taskFlowLogId);
        }
    }

    @Override
    public int log(String logFileId, String logContent, Boolean end) throws TaskLogException {
        synchronized (LOCK) {
            List<TaskLogLine> taskLogLineList = logFileId_to_taskLogLineList.computeIfAbsent(logFileId, k -> new ArrayList<>());
            if (logContent != null && logContent.length() > 0) {
                String[] lines1 = logContent.split("\r\n");
                List<String> lineList2 = new ArrayList<>();
                for (String line1 : lines1) {
                    String[] lines2 = line1.split("\r");
                    Collections.addAll(lineList2, lines2);
                }
                List<String> lineList3 = new ArrayList<>();
                for (String line2 : lineList2) {
                    String[] lines3 = line2.split("\n");
                    Collections.addAll(lineList3, lines3);
                }
                for (int i = 0; i < lineList3.size(); i++) {
                    String line3 = lineList3.get(i);
                    TaskLogLine taskLogLine = new TaskLogLine();
                    taskLogLine.setLineNum(taskLogLineList.size() + 1);
                    taskLogLine.setLine(line3);
                    if (i + 1 < lineList3.size()) {
                        taskLogLine.setEnd(false);
                    } else {
                        // 到达最后一行
                        taskLogLine.setEnd(end);
                    }
                    taskLogLineList.add(taskLogLine);
                }
            }
            return taskLogLineList.size();
        }
    }

    @Override
    public TaskLogResult query(String logFileId, Integer fromLineNum) throws TaskLogException {
        synchronized (LOCK) {
            TaskLogResult taskLogResult = null;
            List<TaskLogLine> allTaskLogLineList = logFileId_to_taskLogLineList.get(logFileId);
            if (allTaskLogLineList != null && allTaskLogLineList.size() >= fromLineNum) {
                taskLogResult = new TaskLogResult();
                taskLogResult.setFromLineNum(fromLineNum);
                StringBuilder logContentBuilder = new StringBuilder();
                for (int i = 0; i < allTaskLogLineList.size(); i++) {
                    if (i + 1 >= fromLineNum) {
                        TaskLogLine taskLogLine = allTaskLogLineList.get(i);
                        logContentBuilder.append(taskLogLine.getLine());
                        if (i + 1 < allTaskLogLineList.size()) {
                            logContentBuilder.append(NEWLINE);
                        } else {
                            // 到达最后一行
                            taskLogResult.setToLineNum(allTaskLogLineList.size());
                            taskLogResult.setLogContent(logContentBuilder.toString());
                            taskLogResult.setEnd(taskLogLine.getEnd());
                        }
                    }
                }
            }
            return taskLogResult;
        }
    }

    @Override
    public void putTaskStatus(TaskLog taskStatus) throws TaskLogException {
        synchronized (LOCK) {
            try {
                String json = objectMapper.writeValueAsString(taskStatus);
                TaskLog taskStatusCloned = objectMapper.readValue(json, TaskLog.class);
                taskId_to_taskStatus.put(taskStatusCloned.getTaskId(), taskStatusCloned);
            } catch (JsonProcessingException e) {
                throw new TaskLogException(e);
            }
        }
    }

    @Override
    public TaskLog getTaskStatus(Long taskId) throws TaskLogException {
        synchronized (LOCK) {
            TaskLog taskStatusCloned = null;
            TaskLog taskStatus = taskId_to_taskStatus.get(taskId);
            if (taskStatus != null) {
                try {
                    String json = objectMapper.writeValueAsString(taskStatus);
                    taskStatusCloned = objectMapper.readValue(json, TaskLog.class);
                } catch (JsonProcessingException e) {
                    throw new TaskLogException(e);
                }
            }
            return taskStatusCloned;
        }
    }

    @Override
    public List<TaskLog> listTaskStatus(Long taskFlowId) throws TaskLogException {
        synchronized (LOCK) {
            List<TaskLog> taskStatusListCloned = new ArrayList<>();
            for (TaskLog taskStatus : taskId_to_taskStatus.values()) {
                if (taskStatus.getTaskFlowId().equals(taskFlowId)) {
                    try {
                        String json = objectMapper.writeValueAsString(taskStatus);
                        TaskLog taskStatusCloned = objectMapper.readValue(json, TaskLog.class);
                        taskStatusListCloned.add(taskStatusCloned);
                    } catch (JsonProcessingException e) {
                        throw new TaskLogException(e);
                    }
                }
            }
            return taskStatusListCloned;
        }
    }

    @Override
    public void deleteTaskStatus(Long taskId) throws TaskLogException {
        synchronized (LOCK) {
            taskId_to_taskStatus.remove(taskId);
        }
    }

    @Override
    public boolean isInProgress(TaskLog taskLog) throws TaskLogException {
        return !TaskStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskLog.getStatus())
                && !TaskStatusEnum.EXECUTE_FAILURE.getCode().equals(taskLog.getStatus())
                && !TaskStatusEnum.EXECUTE_STOPPED.getCode().equals(taskLog.getStatus())
                && !TaskStatusEnum.ROLLBACK_SUCCESS.getCode().equals(taskLog.getStatus())
                && !TaskStatusEnum.ROLLBACK_FAILURE.getCode().equals(taskLog.getStatus())
                && !TaskStatusEnum.ROLLBACK_STOPPED.getCode().equals(taskLog.getStatus());
    }

    @Override
    public boolean canRollback(TaskLog taskLog) throws TaskLogException {
        return taskLog != null
                && (TaskStatusEnum.EXECUTE_SUCCESS.getCode().equals(taskLog.getStatus())
                || TaskStatusEnum.EXECUTE_FAILURE.getCode().equals(taskLog.getStatus())
                || TaskStatusEnum.EXECUTE_STOPPED.getCode().equals(taskLog.getStatus())
                || TaskStatusEnum.ROLLBACK_FAILURE.getCode().equals(taskLog.getStatus())
                || TaskStatusEnum.ROLLBACK_STOPPED.getCode().equals(taskLog.getStatus()));
    }
}
