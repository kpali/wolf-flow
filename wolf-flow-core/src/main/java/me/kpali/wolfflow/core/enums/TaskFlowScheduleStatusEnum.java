package me.kpali.wolfflow.core.enums;

/**
 * 任务流调度状态枚举
 *
 * @author kpali
 */
public enum TaskFlowScheduleStatusEnum {

    JOIN("JOIN", "Join"),
    UPDATE("UPDATE", "Update"),
    FAIL("FAIL", "Fail");

    private String code;
    private String name;

    TaskFlowScheduleStatusEnum(String code, String name) {
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
