package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.exception.TaskFlowQueryException;
import me.kpali.wolfflow.core.model.Link;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.schedule.DefaultTaskFlowQuerier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Primary
@Component
public class MyTaskFlowQuerier extends DefaultTaskFlowQuerier {
    public MyTaskFlowQuerier() {
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

        TaskFlow taskFlow = new TaskFlow();
        taskFlow.setId(100L);
        //taskFlow.setCron("0 * * * * ?");
        taskFlow.setTaskList(new ArrayList<>());
        taskFlow.setLinkList(new ArrayList<>());

        taskFlow.getTaskList().add(new MyTask(3L));
        taskFlow.getTaskList().add(new MyTask(5L));
        taskFlow.getTaskList().add(new MyTask(1L));
        taskFlow.getTaskList().add(new MyTask(10L));
        taskFlow.getTaskList().add(new MyTask(9L));
        taskFlow.getTaskList().add(new MyTask(6L));
        taskFlow.getTaskList().add(new MyTask(4L));
        taskFlow.getTaskList().add(new MyTask(2L));
        taskFlow.getTaskList().add(new MyTask(7L));
        taskFlow.getTaskList().add(new MyTask(11L));
        taskFlow.getTaskList().add(new MyTask(8L));

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
    }

    private List<TaskFlow> taskFlowList = new ArrayList<>();

    @Override
    public TaskFlow getTaskFlow(Long taskFlowId) throws TaskFlowQueryException {
        return this.taskFlowList.stream().filter(taskFlow -> {
           return taskFlow.getId().equals(taskFlowId);
        }).findFirst().get();
    }

    @Override
    public List<TaskFlow> listCronTaskFlow() throws TaskFlowQueryException {
        return this.taskFlowList.stream().filter(taskFlow -> {
            return (taskFlow.getCron() != null && !taskFlow.getCron().trim().isEmpty());
        }).collect(Collectors.toList());
    }
}
