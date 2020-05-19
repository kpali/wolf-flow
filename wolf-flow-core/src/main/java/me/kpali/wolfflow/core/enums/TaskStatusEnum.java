package me.kpali.wolfflow.core.enums;

/**
 * 任务状态枚举
 *
 * @author kpali
 */
public enum TaskStatusEnum {

    WAIT_FOR_EXECUTE("WAIT_FOR_EXECUTE", "等待执行"),
    EXECUTING("EXECUTING", "执行中"),
    EXECUTE_SUCCESS("EXECUTE_SUCCESS", "执行成功"),
    EXECUTE_FAILURE("EXECUTE_FAILURE", "执行失败"),
    STOPPING("STOPPING", "停止中"),
    SKIPPED("SKIPPED", "跳过"),
    MANUAL_CONFIRM("MANUAL_CONFIRM", "手工确认");

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
