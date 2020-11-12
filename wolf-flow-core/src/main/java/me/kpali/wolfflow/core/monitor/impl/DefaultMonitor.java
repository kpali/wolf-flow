package me.kpali.wolfflow.core.monitor.impl;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import me.kpali.wolfflow.core.monitor.IMonitor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 监控器的默认实现
 *
 * @author kpali
 */
@Component
public class DefaultMonitor implements IMonitor {
    @Value("${spring.application.name:wolf-flow}")
    String applicationName;

    @Override
    public void init() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheusRegistry.config().commonTags("application", applicationName);
        Metrics.addRegistry(prometheusRegistry);

        new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
        new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
        new JvmGcMetrics().bindTo(Metrics.globalRegistry);
        new ProcessorMetrics().bindTo(Metrics.globalRegistry);
        new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
    }

    @Override
    public void monitor(ExecutorService executor, String executorName) {
        new ExecutorServiceMetrics(executor, executorName, Collections.emptyList()).bindTo(Metrics.globalRegistry);
    }

    @Override
    public String scrape() {
        List<MeterRegistry> meterRegistries = Metrics.globalRegistry.getRegistries()
                .stream()
                .filter(meterRegistry -> meterRegistry instanceof PrometheusMeterRegistry)
                .collect(Collectors.toList());
        for (MeterRegistry registry : meterRegistries) {
            if (registry instanceof PrometheusMeterRegistry) {
                return ((PrometheusMeterRegistry) registry).scrape();
            }
        }
        return "";
    }
}
