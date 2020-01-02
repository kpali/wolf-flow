package me.kpali.wolfflow.admin.taskflow;

import me.kpali.wolfflow.core.model.Link;
import me.kpali.wolfflow.core.model.Task;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.schedule.DefaultTaskFlowScaner;

import java.util.ArrayList;
import java.util.List;

public class TaskFlowScaner extends DefaultTaskFlowScaner {
    @Override
    public List<TaskFlow> scan() {
        List<TaskFlow> taskFlowList = new ArrayList<>();

        TaskFlow taskFlow = new TaskFlow();
        taskFlow.setId(1L);
        taskFlow.setCron("0 * * * * ?");
        taskFlow.setTaskList(new ArrayList<>());
        taskFlow.setLinkList(new ArrayList<>());

        Task task1 = new Task();
        task1.setId(2L);
        Task task2 = new Task();
        task2.setId(3L);
        taskFlow.getTaskList().add(task1);
        taskFlow.getTaskList().add(task2);

        Link link = new Link();
        link.setSource(4L);
        link.setTarget(5L);
        taskFlow.getLinkList().add(link);


        taskFlowList.add(taskFlow);
        return taskFlowList;
    }
}
