package me.kpali.wolfflow.core.model;

/**
 * 集群常量
 *
 * @author kpali
 */
public class ClusterConstants {
    public static final String GENERATE_NODE_ID_LOCK = "GenerateNodeIdLock";
    public static final String TASK_FLOW_LOG_LOCK_PREFIX = "TaskFlowLogLock_";
    public static final String TASK_LOG_LOCK_PREFIX = "TaskLogLock_";
    public static final String CRON_TASK_FLOW_SCAN_LOCK = "CronTaskFlowScanLock";

    public static String getTaskFlowLogLock(Long taskFlowId) {
        return TASK_FLOW_LOG_LOCK_PREFIX + String.valueOf(taskFlowId);
    }

    public static String getTaskLogLock(Long taskId) {
        return TASK_LOG_LOCK_PREFIX + String.valueOf(taskId);
    }
}
