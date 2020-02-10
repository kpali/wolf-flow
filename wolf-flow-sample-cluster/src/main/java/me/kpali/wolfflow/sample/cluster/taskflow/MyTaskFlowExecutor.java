package me.kpali.wolfflow.sample.cluster.taskflow;

import me.kpali.wolfflow.core.executor.impl.DefaultTaskFlowExecutor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 可以覆写默认任务流执行器的方法实现自定义，但一般情况下直接使用默认任务流执行器即可
 * （可选）
 *
 * @author kpali
 */
@Primary
@Component
public class MyTaskFlowExecutor extends DefaultTaskFlowExecutor {
}
