package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.exception.TaskFlowQueryException;
import me.kpali.wolfflow.core.model.Link;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.querier.impl.DefaultTaskFlowQuerier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Primary
@Component
public class MyTaskFlowQuerier extends DefaultTaskFlowQuerier {
    @Override
    public TaskFlow getTaskFlow(Long taskFlowId) throws TaskFlowQueryException {
        List<TaskFlow> taskFlowList = this.listTaskFlow();
        return taskFlowList.stream().filter(taskFlow -> {
           return taskFlow.getId().equals(taskFlowId);
        }).findFirst().get();
    }

    @Override
    public List<TaskFlow> listCronTaskFlow() throws TaskFlowQueryException {
        List<TaskFlow> taskFlowList = this.listTaskFlow();
        return taskFlowList.stream().filter(taskFlow -> {
            return (taskFlow.getCron() != null && !taskFlow.getCron().trim().isEmpty());
        }).collect(Collectors.toList());
    }

    private List<TaskFlow> listTaskFlow() {
        /**
         * 示例拓扑图：
         *                    --> 9
         * 1 --> 2 --> 4     |    ^
         *  \    \    ^     |    |
         *   \    v  /    /  --> 8
         *    --> 3 --> 5 --> 6
         *               \    |
         *                \   v
         *                --> 7
         * 10 --> 11
         */

        List<TaskFlow> taskFlowList = new ArrayList<>();

        TaskFlow taskFlow = new TaskFlow();
        taskFlow.setId(100L);
        //taskFlow.setCron("0 * * * * ?");
        taskFlow.setTaskList(new ArrayList<>());
        taskFlow.setLinkList(new ArrayList<>());

        MyTask myTask1 = new MyTask();
        myTask1.setId(1L);
        MyTask myTask2 = new MyTask();
        myTask2.setId(2L);
        MyTask myTask3 = new MyTask();
        myTask3.setId(3L);
        MyTask myTask4 = new MyTask();
        myTask4.setId(4L);
        MyTask myTask5 = new MyTask();
        myTask5.setId(5L);
        MyTask myTask6 = new MyTask();
        myTask6.setId(6L);
        MyTask myTask7 = new MyTask();
        myTask7.setId(7L);
        MyTask myTask8 = new MyTask();
        myTask8.setId(8L);
        MyTask myTask9 = new MyTask();
        myTask9.setId(9L);
        MyTask myTask10 = new MyTask();
        myTask10.setId(10L);
        MyTask myTask11 = new MyTask();
        myTask11.setId(11L);
        taskFlow.getTaskList().add(myTask3);
        taskFlow.getTaskList().add(myTask5);
        taskFlow.getTaskList().add(myTask1);
        taskFlow.getTaskList().add(myTask10);
        taskFlow.getTaskList().add(myTask9);
        taskFlow.getTaskList().add(myTask6);
        taskFlow.getTaskList().add(myTask4);
        taskFlow.getTaskList().add(myTask2);
        taskFlow.getTaskList().add(myTask7);
        taskFlow.getTaskList().add(myTask11);
        taskFlow.getTaskList().add(myTask8);

        taskFlow.getLinkList().add(new Link(1L, 2L));
        taskFlow.getLinkList().add(new Link(1L, 3L));
        taskFlow.getLinkList().add(new Link(2L, 3L));
        taskFlow.getLinkList().add(new Link(2L, 4L));
        taskFlow.getLinkList().add(new Link(3L, 4L));
        taskFlow.getLinkList().add(new Link(3L, 5L));
        taskFlow.getLinkList().add(new Link(5L, 6L));
        taskFlow.getLinkList().add(new Link(5L, 7L));
        taskFlow.getLinkList().add(new Link(6L, 7L));
        taskFlow.getLinkList().add(new Link(5L, 8L));
        taskFlow.getLinkList().add(new Link(5L, 9L));
        taskFlow.getLinkList().add(new Link(8L, 9L));
        taskFlow.getLinkList().add(new Link(10L, 11L));

        taskFlowList.add(taskFlow);

        return taskFlowList;
    }
}
