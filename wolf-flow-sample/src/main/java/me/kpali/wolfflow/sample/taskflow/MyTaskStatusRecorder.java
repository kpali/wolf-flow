package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.recorder.impl.DefaultTaskStatusRecorder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MyTaskStatusRecorder extends DefaultTaskStatusRecorder {
}
