package me.kpali.wolfflow.core.enums;

/**
 * 服务节点角色枚举
 *
 * @author kpali
 */
public enum ServiceNodeRoleEnum {

    MASTER("MASTER", "Master"),
    WORKER("WORKER", "Worker");

    public final String code;
    public final String name;

    ServiceNodeRoleEnum(String code, String name) {
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
