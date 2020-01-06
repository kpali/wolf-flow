package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.model.Link;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.schedule.DefaultTaskFlowQuerier;

import java.util.ArrayList;
import java.util.List;

public class MyTaskFlowQuerier extends DefaultTaskFlowQuerier {
    public MyTaskFlowQuerier() {
        TaskFlow taskFlow = new TaskFlow();
        taskFlow.setId(1L);
        taskFlow.setCron("0 * * * * ?");
        taskFlow.setTaskList(new ArrayList<>());
        taskFlow.setLinkList(new ArrayList<>());

        MyTask task1 = new MyTask();
        task1.setId(2L);
        MyTask task2 = new MyTask();
        task2.setId(3L);
        taskFlow.getTaskList().add(task1);
        taskFlow.getTaskList().add(task2);

        Link link = new Link();
        link.setSource(4L);
        link.setTarget(5L);
        taskFlow.getLinkList().add(link);

        taskFlowList.add(taskFlow);
    }

    private List<TaskFlow> taskFlowList = new ArrayList<>();

    @Override
    public TaskFlow getTaskFlow(Long taskFlowId) {
        return taskFlowList.stream().filter(taskFlow -> {
           return taskFlow.getId().equals(taskFlowId);
        }).findFirst().get();
    }

    @Override
    public List<TaskFlow> listCronTaskFlow() {
        return this.taskFlowList;
    }
}
