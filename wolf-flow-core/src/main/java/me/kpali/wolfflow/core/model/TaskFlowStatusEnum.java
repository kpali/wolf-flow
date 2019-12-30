package me.kpali.wolfflow.core.model;

/**
 * 任务流状态枚举
 *
 * @author kpali
 */
public enum TaskFlowStatusEnum {

    WAIT_FOR_TRIGGER("WAIT_FOR_TRIGGER", "等待触发"),
    TRIGGER_FAIL("TRIGGER_FAIL", "触发失败"),
    EXECUTING("EXECUTING", "执行中"),
    EXECUTE_SUCCESS("EXECUTE_SUCCESS", "执行成功"),
    EXECUTE_FAIL("EXECUTE_FAIL", "执行失败");

    public final String code;
    public final String name;

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
