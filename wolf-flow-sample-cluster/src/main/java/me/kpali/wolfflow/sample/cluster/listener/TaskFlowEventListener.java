package me.kpali.wolfflow.sample.cluster.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.event.TaskFlowScheduleStatusChangeEvent;
import me.kpali.wolfflow.core.event.TaskFlowStatusChangeEvent;
import me.kpali.wolfflow.core.event.TaskStatusChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TaskFlowEventListener {
    private ObjectMapper objectMapper = new ObjectMapper();

    @EventListener
    public void taskFlowScheduleStatusChange(TaskFlowScheduleStatusChangeEvent event) {
    }

    @EventListener
    public void taskFlowStatusChange(TaskFlowStatusChangeEvent event) {
        try {
            System.out.println(">>>>>>>>>> 任务流状态变更：\r\n" + objectMapper.writeValueAsString(event.getTaskFlowStatus()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void taskStatusChange(TaskStatusChangeEvent event) {
        try {
            System.out.println(">>>>>>>>>> 任务状态变更：\r\n" + objectMapper.writeValueAsString(event.getTaskStatus()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
