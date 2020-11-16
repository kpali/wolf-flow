package me.kpali.wolfflow.core.enums;

/**
 * 任务状态枚举
 *
 * @author kpali
 */
public enum TaskStatusEnum {

    WAIT_FOR_EXECUTE("WAIT_FOR_EXECUTE", "Wait for execute"),
    EXECUTING("EXECUTING", "Executing"),
    EXECUTE_SUCCESS("EXECUTE_SUCCESS", "Execute success"),
    EXECUTE_FAILURE("EXECUTE_FAILURE", "Execute failure"),

    STOPPING("STOPPING", "Stopping"),
    SKIPPED("SKIPPED", "Skipped"),
    MANUAL_CONFIRM("MANUAL_CONFIRM", "Manual Confirm"),

    WAIT_FOR_ROLLBACK("WAIT_FOR_ROLLBACK", "Wait for rollback"),
    ROLLING_BACK("ROLLING_BACK", "Rolling back"),
    ROLLBACK_SUCCESS("ROLLBACK_SUCCESS", "Rollback success"),
    ROLLBACK_FAILURE("ROLLBACK_FAILURE", "Rollback failure");

    private String code;
    private String name;

    TaskStatusEnum(String code, String name) {
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
