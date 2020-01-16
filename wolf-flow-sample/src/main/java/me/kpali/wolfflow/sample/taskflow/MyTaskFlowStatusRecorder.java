package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.schedule.DefaultTaskFlowStatusRecorder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MyTaskFlowStatusRecorder extends DefaultTaskFlowStatusRecorder {
}
