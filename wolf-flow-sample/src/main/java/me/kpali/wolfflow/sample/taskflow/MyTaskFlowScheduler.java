package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.scheduler.impl.DefaultTaskFlowScheduler;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MyTaskFlowScheduler extends DefaultTaskFlowScheduler {
}
