package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.logger.impl.DefaultTaskLogger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MyTaskLogger extends DefaultTaskLogger {
}
