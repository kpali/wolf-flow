package me.kpali.wolfflow.core.model;

/**
 * 集群常量
 *
 * @author kpali
 */
public class ClusterConstants {
    public static final String TASK_FLOW_STATUS_RECORD_LOCK = "TaskFlowStatusRecordLock";
    public static final String TASK_STATUS_RECORD_LOCK = "TaskStatusRecordLock";
    public static final int STATUS_RECORD_LOCK_WAIT_TIME = 10;
    public static final int STATUS_RECORD_LOCK_LEASE_TIME = 15;
    public static final String CRON_TASK_FLOW_SCAN_LOCK = "CronTaskFlowScanLock";
}
