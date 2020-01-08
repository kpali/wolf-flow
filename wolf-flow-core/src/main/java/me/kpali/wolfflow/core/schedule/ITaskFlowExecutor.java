package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlow;
import me.kpali.wolfflow.core.model.TaskFlowContext;

/**
 * 任务流执行器接口
 *
 * @author kpali
 */
public interface ITaskFlowExecutor {

    /**
     * 初始化任务流上下文
     *
     * @param taskFlow
     * @return
     */
    TaskFlowContext initContext(TaskFlow taskFlow);

    /**
     * 任务流执行前置处理
     *
     * @param taskFlow
     * @param taskFlowContext
     */
    void beforeExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext);

    /**
     * 任务流执行
     *
     * @param taskFlow
     * @param taskFlowContext
     * @param taskFlowExecutorCorePoolSize
     * @param taskFlowExecutorMaximumPoolSize
     */
    void execute(TaskFlow taskFlow, TaskFlowContext taskFlowContext,
                 Integer taskFlowExecutorCorePoolSize, Integer taskFlowExecutorMaximumPoolSize);

    /**
     * 任务流后置处理
     *
     * @param taskFlow
     * @param taskFlowContext
     */
    void afterExecute(TaskFlow taskFlow, TaskFlowContext taskFlowContext);

}
