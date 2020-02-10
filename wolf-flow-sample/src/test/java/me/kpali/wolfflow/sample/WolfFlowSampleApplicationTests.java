package me.kpali.wolfflow.sample;

import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.model.TaskLog;
import me.kpali.wolfflow.core.model.TaskLogResult;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class WolfFlowSampleApplicationTests {
    @Autowired
    ITaskFlowScheduler taskFlowScheduler;
    @Autowired
    ITaskFlowLogger taskFlowLogger;
    @Autowired
    ITaskLogger taskLogger;

    @Test
    public void taskFlowTriggerTest() {
        try {
            long logId1 = taskFlowScheduler.triggerTo(100L, 5L, null);
            System.out.println(">>>>>>>>>> 日志ID：" + logId1);
            Thread.sleep(3 * 1000);
            //taskFlowScheduler.stop(logId1);
            this.waitDoneAndPrintLog(logId1);
            long logId2 = taskFlowScheduler.triggerTo(100L, 6L, null);
            System.out.println(">>>>>>>>>> 日志ID：" + logId2);
            this.waitDoneAndPrintLog(logId2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitDoneAndPrintLog(long logId) {
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TaskFlowLog taskFlowLog = taskFlowLogger.get(logId);
            if (!taskFlowLogger.isInProgress(taskFlowLog)) {
                List<TaskLog> taskLogList = taskLogger.list(logId);
                for (TaskLog taskLog : taskLogList) {
                    TaskLogResult taskLogResult = taskLogger.query(logId, taskLog.getTaskId(), 1);
                    if (taskLogResult != null) {
                        System.out.println(">>>>>>>>>> 任务[" + taskLog.getTaskId() + "]日志内容：");
                        System.out.println(taskLogResult.getLogContent());
                    }
                }
                break;
            }
        }
    }

}
