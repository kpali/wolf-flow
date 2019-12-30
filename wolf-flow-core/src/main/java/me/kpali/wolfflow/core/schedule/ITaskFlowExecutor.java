package me.kpali.wolfflow.core.schedule;

import me.kpali.wolfflow.core.model.TaskFlowContext;
import me.kpali.wolfflow.core.model.TaskFlowLog;

/**
 * 任务流执行器接口
 *
 * @author kpali
 */
public interface ITaskFlowExecutor {

    /**
     * 创建任务流上下文
     *
     * @param taskFlowId
     * @return
     */
    TaskFlowContext createContext(Long taskFlowId);

    /**
     * 新增任务流日志
     *
     * @return taskFlowLogId 任务流日志ID
     */
    Long insertLog(TaskFlowLog taskFlowLog);

    /**
     * 任务流执行前置处理
     *
     * @param taskFlowContext
     */
    void beforeExecute(TaskFlowContext taskFlowContext);

    /**
     * 任务流执行
     *
     * @param taskFlowContext
     */
    void execute(TaskFlowContext taskFlowContext);

    /**
     * 任务流后置处理
     *
     * @param taskFlowContext
     */
    void afterExecute(TaskFlowContext taskFlowContext);

}
