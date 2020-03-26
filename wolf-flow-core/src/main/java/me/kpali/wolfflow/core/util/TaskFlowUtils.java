package me.kpali.wolfflow.core.util;

import me.kpali.wolfflow.core.model.Link;
import me.kpali.wolfflow.core.model.Task;
import me.kpali.wolfflow.core.model.TaskFlow;

import java.util.*;

/**
 * 任务流工具类
 *
 * @author kpali
 */
public class TaskFlowUtils {

    /**
     * 拓扑排序算法
     *
     * @param taskFlow
     * @return
     */
    public static List<Task> topologicalSort(TaskFlow taskFlow) {
        List<Task> sortedTaskList = new ArrayList<>();
        int taskCount = taskFlow.getTaskList().size();

        Map<Long, Task> id_mapto_task = new HashMap<>();
        for (Task task : taskFlow.getTaskList()) {
            id_mapto_task.put(task.getId(), task);
        }

        // 计算节点入度
        Map<Long, Integer> taskId_mapto_inDegree = new HashMap<>();
        for (Task task : taskFlow.getTaskList()) {
            int inDegree = 0;
            for (Link link : taskFlow.getLinkList()) {
                if (link.getTarget().equals(task.getId())) {
                    inDegree++;
                }
            }
            taskId_mapto_inDegree.put(task.getId(), inDegree);
        }

        // 从入度为0的节点开始遍历，对遍历过的节点将其子节点的入度减1，重复此步骤，直到遍历完所有节点
        Deque<Task> deque = new ArrayDeque<>();
        for (Long taskId : taskId_mapto_inDegree.keySet()) {
            int inDegree = taskId_mapto_inDegree.get(taskId);
            if (inDegree == 0) {
                deque.offer(id_mapto_task.get(taskId));
            }
        }
        while (!deque.isEmpty()) {
            Task task = deque.poll();
            sortedTaskList.add(task);

            for (Link link : taskFlow.getLinkList()) {
                if (link.getSource().equals(task.getId())) {
                    Long childTaskId = link.getTarget();
                    int inDegree = taskId_mapto_inDegree.get(childTaskId);
                    inDegree--;
                    taskId_mapto_inDegree.put(childTaskId, inDegree);
                    if (inDegree == 0) {
                        deque.offer(id_mapto_task.get(childTaskId));
                    }
                }
            }
        }

        // 如果无法遍历完所有节点，则说明当前拓扑不是有向无环图，不存在拓扑排序。
        return sortedTaskList.size() == taskCount ? sortedTaskList : null;
    }

    /**
     * 根据从指定任务开始或到指定任务结束，对任务流进行剪裁
     *
     * @param taskFlow
     * @param fromTaskId
     * @param toTaskId
     * @return
     */
    public static TaskFlow prune(TaskFlow taskFlow, Long fromTaskId, Long toTaskId) {
        TaskFlow prunedTaskFlow = new TaskFlow();
        prunedTaskFlow.setId(taskFlow.getId());
        prunedTaskFlow.setCron(taskFlow.getCron());
        prunedTaskFlow.setTaskList(new ArrayList<>());
        prunedTaskFlow.setLinkList(new ArrayList<>());

        Map<Long, Task> id_mapto_task = new HashMap<>();
        for (Task task : taskFlow.getTaskList()) {
            id_mapto_task.put(task.getId(), task);
        }
        Deque<Task> deque = new ArrayDeque<>();

        if (fromTaskId == null && toTaskId == null) {
            // 所有任务
            prunedTaskFlow = taskFlow;
        } else if (fromTaskId != null && fromTaskId.equals(toTaskId)) {
            // 指定任务
            Task fromTask = id_mapto_task.get(fromTaskId);
            prunedTaskFlow.getTaskList().add(fromTask);
        } else if (fromTaskId != null) {
            // 从指定任务开始
            Task fromTask = id_mapto_task.get(fromTaskId);
            prunedTaskFlow.getTaskList().add(fromTask);
            deque.offer(fromTask);
            while (!deque.isEmpty()) {
                Task task = deque.poll();
                for (Link link : taskFlow.getLinkList()) {
                    if (link.getSource().equals(task.getId())) {
                        Task childTask = id_mapto_task.get(link.getTarget());
                        if (!prunedTaskFlow.getTaskList().contains(childTask)) {
                            prunedTaskFlow.getTaskList().add(childTask);
                        }
                        prunedTaskFlow.getLinkList().add(link);
                        deque.offer(childTask);
                    }
                }
            }
        } else {
            // 到指定任务结束
            Task toTask = id_mapto_task.get(toTaskId);
            prunedTaskFlow.getTaskList().add(toTask);
            deque.offer(toTask);
            while (!deque.isEmpty()) {
                Task task = deque.poll();
                for (Link link : taskFlow.getLinkList()) {
                    if (link.getTarget().equals(task.getId())) {
                        Task parentTask = id_mapto_task.get(link.getSource());
                        if (!prunedTaskFlow.getTaskList().contains(parentTask)) {
                            prunedTaskFlow.getTaskList().add(parentTask);
                        }
                        prunedTaskFlow.getLinkList().add(link);
                        deque.offer(parentTask);
                    }
                }
            }
        }
        return prunedTaskFlow;
    }

}
