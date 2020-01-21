package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.cluster.impl.DefaultClusterController;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MyClusterController extends DefaultClusterController {
}
