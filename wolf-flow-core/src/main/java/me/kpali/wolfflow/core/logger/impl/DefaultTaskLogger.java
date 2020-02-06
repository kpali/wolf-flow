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
    private Map<Long, List<TaskLog>> taskLogListMap = new ConcurrentHashMap<>();
    private Map<Long, Map<Long, List<TaskLogLine>>> taskLogLineMap = new ConcurrentHashMap<>();
    private static final String NEWLINE = System.getProperty("line.separator");
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<TaskLog> list(Long logId) throws TaskLogException {
        List<TaskLog> taskLogList = taskLogListMap.get(logId);
        List<TaskLog> taskLogListCloned = new ArrayList<>();
        if (taskLogList != null) {
            try {
                String json = objectMapper.writeValueAsString(taskLogList);
                JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, TaskLog.class);
                taskLogListCloned = objectMapper.readValue(json, javaType);
            } catch (JsonProcessingException e) {
                throw new TaskLogException(e);
            }
        }
        return taskLogListCloned;
    }

    @Override
    public TaskLog last(Long taskId) throws TaskLogException {
        TaskLog lastTaskLog = null;
        for (List<TaskLog> taskLogList : taskLogListMap.values()) {
            for (TaskLog taskLog : taskLogList) {
                if (taskId.equals(taskLog.getTaskId()) && (lastTaskLog == null || taskLog.getLogId() > lastTaskLog.getLogId())) {
                    lastTaskLog = taskLog;
                }
            }
        }
        TaskLog taskLogCloned = null;
        if (lastTaskLog != null) {
            try {
                String json = objectMapper.writeValueAsString(lastTaskLog);
                taskLogCloned = objectMapper.readValue(json, TaskLog.class);
            } catch (JsonProcessingException e) {
                throw new TaskLogException(e);
            }
        }
        return taskLogCloned;
    }

    @Override
    public List<TaskLog> lastByTaskFlowId(Long taskFlowId) throws TaskLogException {
        List<TaskLog> lastTaskLogList = null;
        for (List<TaskLog> taskLogList : taskLogListMap.values()) {
            if (!taskLogList.isEmpty()) {
                TaskLog taskLog = taskLogList.get(0);
                if (taskFlowId.equals(taskLog.getTaskFlowId())) {
                    if (lastTaskLogList == null || taskLog.getLogId() > lastTaskLogList.get(0).getLogId()) {
                        lastTaskLogList = taskLogList;
                    }
                }
            }
        }
        List<TaskLog> taskLogListCloned = new ArrayList<>();
        if (lastTaskLogList != null) {
            try {
                String json = objectMapper.writeValueAsString(lastTaskLogList);
                JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, TaskLog.class);
                taskLogListCloned = objectMapper.readValue(json, javaType);
            } catch (JsonProcessingException e) {
                throw new TaskLogException(e);
            }
        }
        return taskLogListCloned;
    }

    @Override
    public TaskLog get(Long logId, Long taskId) throws TaskLogException {
        TaskLog taskLogCloned = null;
        List<TaskLog> taskLogList = taskLogListMap.get(logId);
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
            List<TaskLog> existsTaskLogList = taskLogListMap.get(taskLogCloned.getLogId());
            if (existsTaskLogList != null) {
                for (TaskLog existsTaskLog : existsTaskLogList) {
                    if (existsTaskLog.getTaskId().equals(taskLogCloned.getTaskId())) {
                        taskLogCloned.setCreationTime(existsTaskLog.getCreationTime());
                        BeanUtils.copyProperties(taskLogCloned, existsTaskLog);
                        return;
                    }
                }
                existsTaskLogList.add(taskLogCloned);
            } else {
                List<TaskLog> taskLogList = new ArrayList<>();
                taskLogList.add(taskLogCloned);
                taskLogListMap.put(taskLogCloned.getLogId(), taskLogList);
            }
        } catch (JsonProcessingException e) {
            throw new TaskLogException(e);
        }
    }

    @Override
    public void delete(Long logId) throws TaskLogException {
        taskLogListMap.remove(logId);
        taskLogLineMap.remove(logId);
    }

    @Override
    public void delete(Long logId, Long taskId) throws TaskLogException {
        List<TaskLog> taskLogList = taskLogListMap.get(logId);
        if (taskLogList != null) {
            List<TaskLog> newTaskLogList = new ArrayList<>();
            for (TaskLog taskLog : taskLogList) {
                if (!taskLog.getTaskId().equals(taskId)) {
                    newTaskLogList.add(taskLog);
                }
            }
            taskLogListMap.put(logId, newTaskLogList);
        }
        Map<Long, List<TaskLogLine>> taskLogLineListMap = taskLogLineMap.get(logId);
        if (taskLogLineListMap != null) {
            taskLogLineListMap.remove(taskId);
        }
    }

    @Override
    public int log(Long logId, Long taskId, String logContent, Boolean end) throws TaskLogException {
        if (!taskLogLineMap.containsKey(logId)) {
            taskLogLineMap.put(logId, new ConcurrentHashMap<>());
        }
        Map<Long, List<TaskLogLine>> taskLogLineListMap = taskLogLineMap.get(logId);
        if (!taskLogLineListMap.containsKey(taskId)) {
            taskLogLineListMap.put(taskId, new ArrayList<>());
        }
        List<TaskLogLine> taskLogLineList = taskLogLineListMap.get(taskId);
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
    public TaskLogResult query(Long logId, Long taskId, Integer fromLineNum) throws TaskLogException {
        TaskLogResult taskLogResult = null;
        Map<Long, List<TaskLogLine>> taskLogLineListMap = taskLogLineMap.get(logId);
        if (taskLogLineListMap != null) {
            List<TaskLogLine> allTaskLogLineList = taskLogLineListMap.get(taskId);
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
        }
        return taskLogResult;
    }
}
