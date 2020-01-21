package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.executor.impl.DefaultTaskFlowExecutor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MyTaskFlowExecutor extends DefaultTaskFlowExecutor {
}
