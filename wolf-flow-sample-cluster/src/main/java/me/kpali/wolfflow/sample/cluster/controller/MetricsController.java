package me.kpali.wolfflow.sample.cluster.controller;

import me.kpali.wolfflow.core.monitor.IMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {
    @Autowired
    IMonitor monitor;

    @GetMapping(value = "/prometheus", produces = {"text/plain; version=0.0.4; charset=utf-8"})
    public String getMetrics() {
        String response = monitor.scrape();
        return response;
    }
}
