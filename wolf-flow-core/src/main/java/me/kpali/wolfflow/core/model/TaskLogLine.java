package me.kpali.wolfflow.core.model;

import java.io.Serializable;

/**
 * 任务日志行
 *
 * @author kpali
 */
public class TaskLogLine implements Serializable {
    private static final long serialVersionUID = -5469833248990193615L;

    private Integer lineNum;
    private String line;
    private Boolean end;

    public Integer getLineNum() {
        return lineNum;
    }

    public void setLineNum(Integer lineNum) {
        this.lineNum = lineNum;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public Boolean getEnd() {
        return end;
    }

    public void setEnd(Boolean end) {
        this.end = end;
    }
}
