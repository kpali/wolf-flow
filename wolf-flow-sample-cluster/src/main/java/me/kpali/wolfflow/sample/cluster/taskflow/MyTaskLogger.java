package me.kpali.wolfflow.sample.cluster.taskflow;

import me.kpali.wolfflow.core.logger.impl.DefaultTaskLogger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 可以覆写默认任务日志器的方法实现自定义，推荐使用数据库DAO实现日志持久化到硬盘。本示例为简单起见仍然使用默认实现存储到内存
 * （可选）
 *
 * @author kpali
 */
@Primary
@Component
public class MyTaskLogger extends DefaultTaskLogger {
}
