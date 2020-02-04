package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 任务日志结果
 *
 * @author kpali
 */
public class TaskLogResult implements Serializable {
    private static final long serialVersionUID = -237328970035347440L;

    private Integer fromLineNum;
    private Integer toLineNum;
    private String logContent;
    private Boolean end;

    public Integer getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(Integer fromLineNum) {
        this.fromLineNum = fromLineNum;
    }

    public Integer getToLineNum() {
        return toLineNum;
    }

    public void setToLineNum(Integer toLineNum) {
        this.toLineNum = toLineNum;
    }

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }

    public Boolean getEnd() {
        return end;
    }

    public void setEnd(Boolean end) {
        this.end = end;
    }
}
