package me.kpali.wolfflow.core.model;

/**
 * 集群常量
 *
 * @author kpali
 */
public class ClusterConstants {
    public static final String GENERATE_NODE_ID_LOCK = "GenerateNodeIdLock";
    public static final int GENERATE_NODE_ID_LOCK_LEASE_TIME = 60;
    public static final String TASK_FLOW_LOG_LOCK = "TaskFlowLogLock";
    public static final String TASK_LOG_LOCK = "TaskLogLock";
    public static final int LOG_LOCK_WAIT_TIME = 10;
    public static final int LOG_LOCK_LEASE_TIME = 15;
    public static final String CRON_TASK_FLOW_SCAN_LOCK = "CronTaskFlowScanLock";
}
