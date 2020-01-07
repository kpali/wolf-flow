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

}
