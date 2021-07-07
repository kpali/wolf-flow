package me.kpali.wolfflow.core.enums;

/**
 * 任务流状态枚举
 *
 * @author kpali
 */
public enum TaskFlowStatusEnum {

    WAIT_FOR_EXECUTE("WAIT_FOR_EXECUTE", "Wait for execute"),
    EXECUTING("EXECUTING", "Executing"),
    EXECUTE_SUCCESS("EXECUTE_SUCCESS", "Execute success"),
    EXECUTE_FAILURE("EXECUTE_FAILURE", "Execute failure"),
    EXECUTE_STOPPED("EXECUTE_STOPPED", "Execute stopped"),

    STOPPING("STOPPING", "Stopping"),

    WAIT_FOR_ROLLBACK("WAIT_FOR_ROLLBACK", "Wait for rollback"),
    ROLLING_BACK("ROLLING_BACK", "Rolling back"),
    ROLLBACK_SUCCESS("ROLLBACK_SUCCESS", "Rollback success"),
    ROLLBACK_FAILURE("ROLLBACK_FAILURE", "Rollback failure"),
    ROLLBACK_STOPPED("ROLLBACK_STOPPED", "Rollback stopped");

    private String code;
    private String name;

    TaskFlowStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}
