package me.kpali.wolfflow.core.model;

/**
 * 上下文键
 *
 * @author kpali
 */
public class ContextKey {
    // 任务流上下文

    public static final String IS_ROLLBACK = "isRollback";
    public static final String FROM_TASK_ID = "fromTaskId";
    public static final String TO_TASK_ID = "toTaskId";
    public static final String LOG_ID = "logId";
    public static final String LAST_LOG_ID = "lastLogId";
    public static final String EXECUTE_TASK_FLOW = "executeTaskFlow";
    public static final String ROLLBACK_TASK_FLOW = "rollbackTaskFlow";
    public static final String PARAMS = "params";
    public static final String TASK_CONTEXTS = "taskContexts";
    public static final String EXECUTED_BY_NODE = "executedByNode";

    // 任务上下文

    public static final String TASK_LOG_ID = "logId";
    public static final String TASK_LOG_FILE_ID = "logFileId";
    public static final String PARENT_TASK_ID_LIST = "parentTaskIdList";
}
