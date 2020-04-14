package me.kpali.wolfflow.autoconfigure.config;

import org.springframework.context.annotation.ComponentScan;

/**
 * 事件发布器配置
 *
 * @author kpali
 */
@ComponentScan(basePackages = {"me.kpali.wolfflow.core.event"})
public class EventPublisherConfiguration {
}
