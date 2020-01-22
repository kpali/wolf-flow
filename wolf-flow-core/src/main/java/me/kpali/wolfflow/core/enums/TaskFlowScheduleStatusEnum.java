package me.kpali.wolfflow.core.enums;

/**
 * 任务流调度状态枚举
 *
 * @author kpali
 */
public enum TaskFlowScheduleStatusEnum {

    JOIN("JOIN", "加入调度"),
    UPDATE("UPDATE", "更新调度"),
    FAIL("FAIL", "调度失败");

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
