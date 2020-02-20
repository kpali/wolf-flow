package me.kpali.wolfflow.sample.cluster;

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
public class WolfFlowSampleClusterApplicationTests {
    @Autowired
    ITaskFlowScheduler taskFlowScheduler;
    @Autowired
    ITaskFlowLogger taskFlowLogger;
    @Autowired
    ITaskLogger taskLogger;

    @Test
    public void taskFlowTriggerTest() {
        try {
            long taskFLowLogId1 = taskFlowScheduler.triggerTo(100L, 5L, null);
            System.out.println(">>>>>>>>>> 日志ID：" + taskFLowLogId1);
            Thread.sleep(3 * 1000);
            //taskFlowScheduler.stop(taskFLowLogId1);
            this.waitDoneAndPrintLog(taskFLowLogId1);
            long taskFLowLogId2 = taskFlowScheduler.triggerTo(100L, 6L, null);
            System.out.println(">>>>>>>>>> 日志ID：" + taskFLowLogId2);
            this.waitDoneAndPrintLog(taskFLowLogId2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitDoneAndPrintLog(long taskFLowLogId) {
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TaskFlowLog taskFlowLog = taskFlowLogger.get(taskFLowLogId);
            if (!taskFlowLogger.isInProgress(taskFlowLog)) {
                List<TaskLog> taskLogList = taskLogger.list(taskFLowLogId);
                for (TaskLog taskLog : taskLogList) {
                    TaskLogResult taskLogResult = taskLogger.query(taskFLowLogId, taskLog.getTaskId(), 1);
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
