package me.kpali.wolfflow.sample.controller;

import me.kpali.wolfflow.core.exception.TaskFlowTriggerException;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TriggerController {
    @Autowired
    ITaskFlowScheduler taskFlowScheduler;

    @GetMapping(value = "/trigger/{taskFlowId}")
    public void trigger(@PathVariable Long taskFlowId) throws TaskFlowTriggerException {
        taskFlowScheduler.execute(taskFlowId, null);
    }
}
