package me.kpali.wolfflow.core.schedule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.kpali.wolfflow.core.exception.InvalidTaskFlowException;
import me.kpali.wolfflow.core.model.Task;
import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.core.util.TaskFlowUtils;

import java.util.List;
import java.util.concurrent.*;

/**
 * 任务流执行器的默认实现
 *
 * @author kpali
 */
public class DefaultTaskFlowExecutor implements ITaskFlowExecutor {
    @Override
    public TaskFlowContext initContext(TaskFlow taskFlow) {
        return null;
    }

    @Override
    public void beforeExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
        // 不做任何操作
    }

    @Override
    public void execute(TaskFlow taskFlow, TaskFlowContext taskFlowContext,
                        Integer taskFlowExecutorCorePoolSize, Integer taskFlowExecutorMaximumPoolSize) {
        // 检查任务流是否是一个有向无环图
        List<Task> sortedTaskList = TaskFlowUtils.topologicalSort(taskFlow);
        if (sortedTaskList == null) {
            throw new InvalidTaskFlowException("任务流不是一个有向无环图，请检查是否存在回路！");
        }
        // 初始化线程池
        ThreadFactory executorThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("taskFlowExecutor-pool-%d").build();
        ExecutorService executorThreadPool = new ThreadPoolExecutor(taskFlowExecutorCorePoolSize, taskFlowExecutorMaximumPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), executorThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        // 任务流执行
        for (Task task : sortedTaskList) {
            task.execute();
        }
    }

    @Override
    public void afterExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext) {
        // 不做任何操作
    }
}
