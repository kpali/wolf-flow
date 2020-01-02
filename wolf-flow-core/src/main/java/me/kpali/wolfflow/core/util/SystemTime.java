package me.kpali.wolfflow.core.util;

/**
 * 系统时间工具
 *
 * @author kpali
 */
public class SystemTime {
    public static synchronized Long getUniqueTime() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis();
    }
}
