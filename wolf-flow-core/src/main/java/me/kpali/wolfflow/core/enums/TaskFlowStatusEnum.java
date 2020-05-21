package me.kpali.wolfflow.core.enums;

/**
 * 任务流状态枚举
 *
 * @author kpali
 */
public enum TaskFlowStatusEnum {

    WAIT_FOR_EXECUTE("WAIT_FOR_EXECUTE", "等待执行"),
    EXECUTING("EXECUTING", "执行中"),
    EXECUTE_SUCCESS("EXECUTE_SUCCESS", "执行成功"),
    EXECUTE_FAILURE("EXECUTE_FAILURE", "执行失败"),

    STOPPING("STOPPING", "停止中"),

    WAIT_FOR_ROLLBACK("WAIT_FOR_ROLLBACK", "等待回滚"),
    ROLLING_BACK("ROLLING_BACK", "回滚中"),
    ROLLBACK_SUCCESS("ROLLBACK_SUCCESS", "回滚成功"),
    ROLLBACK_FAILURE("ROLLBACK_FAILURE", "回滚失败");

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
