package me.kpali.wolfflow.core.logger.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.exception.TaskLogException;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.TaskLog;
import me.kpali.wolfflow.core.model.TaskLogLine;
import me.kpali.wolfflow.core.model.TaskLogResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务日志器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultTaskLogger implements ITaskLogger {
    /**
     *  [任务流日志ID, [任务日志ID, 任务日志]]
     */
    private Map<Long, Map<Long, TaskLog>> taskFlowLogId_to_taskLogMap = new ConcurrentHashMap<>();
    /**
     * [任务ID, 任务状态]
     */
    private Map<Long, TaskLog> taskId_to_taskStatus = new ConcurrentHashMap<>();
    /**
     * [日志文件ID, 任务日志行列表]
     */
    private Map<String, List<TaskLogLine>> logFileId_to_taskLogLineList = new ConcurrentHashMap<>();
    private static final String NEWLINE = System.getProperty("line.separator");
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<TaskLog> list(Long taskFlowLogId) throws TaskLogException {
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

    @Override
    public TaskLog get(Long taskFlowLogId, Long taskId) throws TaskLogException {
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

    @Override
    public void add(TaskLog taskLog) throws TaskLogException {
        this.put(taskLog);
    }

    @Override
    public void update(TaskLog taskLog) throws TaskLogException {
        this.put(taskLog);
    }

    private void put(TaskLog taskLog) throws TaskLogException {
        try {
            String json = objectMapper.writeValueAsString(taskLog);
            TaskLog taskLogCloned = objectMapper.readValue(json, TaskLog.class);
            Date now = new Date();
            taskLogCloned.setCreationTime(now);
            taskLogCloned.setUpdateTime(now);
            Map<Long, TaskLog> taskLogId_to_taskLog = taskFlowLogId_to_taskLogMap.computeIfAbsent(taskLogCloned.getTaskFlowLogId(), k -> new ConcurrentHashMap<>());
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

    @Override
    public void deleteByTaskFlowLogId(Long taskFlowLogId) throws TaskLogException {
        Map<Long, TaskLog> taskLogMap = taskFlowLogId_to_taskLogMap.get(taskFlowLogId);
        if (taskLogMap != null) {
            for (TaskLog taskLog : taskLogMap.values()) {
                logFileId_to_taskLogLineList.remove(taskLog.getLogId());
                // 删除任务状态
                this.deleteTaskStatus(taskLog.getTaskId());
            }
        }
        taskFlowLogId_to_taskLogMap.remove(taskFlowLogId);
    }

    @Override
    public int log(String logFileId, String logContent, Boolean end) throws TaskLogException {
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

    @Override
    public TaskLogResult query(String logFileId, Integer fromLineNum) throws TaskLogException {
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

    @Override
    public void putTaskStatus(TaskLog taskStatus) throws TaskLogException {
        try {
            String json = objectMapper.writeValueAsString(taskStatus);
            TaskLog taskStatusCloned = objectMapper.readValue(json, TaskLog.class);
            taskId_to_taskStatus.put(taskStatusCloned.getTaskId(), taskStatusCloned);
        } catch (JsonProcessingException e) {
            throw new TaskLogException(e);
        }
    }

    @Override
    public TaskLog getTaskStatus(Long taskId) throws TaskLogException {
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

    @Override
    public List<TaskLog> listTaskStatus(Long taskFlowId) throws TaskLogException {
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

    @Override
    public void deleteTaskStatus(Long taskId) throws TaskLogException {
        taskId_to_taskStatus.remove(taskId);
    }
}
