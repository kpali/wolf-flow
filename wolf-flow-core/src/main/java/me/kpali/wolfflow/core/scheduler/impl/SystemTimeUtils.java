package me.kpali.wolfflow.core.scheduler.impl;

import me.kpali.wolfflow.core.cluster.IClusterController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 系统时间工具
 *
 * @author kpali
 */
@Component
public class SystemTimeUtils {
    private static final Logger log = LoggerFactory.getLogger(SystemTimeUtils.class);

    @Autowired
    private IClusterController clusterController;

    private static final String UNIQUE_TIME_LOCK = "UniqueTimeLock";

    /**
     * 获取全局唯一的时间戳
     *
     * @return
     */
    public long getUniqueTimeStamp() {
        try {
            this.clusterController.lock(UNIQUE_TIME_LOCK, 3, TimeUnit.SECONDS);
            Thread.sleep(1);
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        } finally {
            try {
                this.clusterController.unlock(UNIQUE_TIME_LOCK);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
        return System.currentTimeMillis();
    }
}
