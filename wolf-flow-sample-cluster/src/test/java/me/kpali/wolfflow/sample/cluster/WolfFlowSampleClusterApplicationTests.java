package me.kpali.wolfflow.sample.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            long taskFlowLogId1 = taskFlowScheduler.triggerTo(100L, 5L, null);
            System.out.println(">>>>>>>>>> 任务流日志ID：" + taskFlowLogId1);
            Thread.sleep(3 * 1000);
            //taskFlowScheduler.stop(taskFLowLogId1);
            this.waitDoneAndPrintLog(taskFlowLogId1);
            List<TaskLog> taskStatusList1 = taskLogger.listTaskStatus(100L);
            System.out.println(">>>>>>>>>> 执行完成，当前各任务状态：");
            System.out.println(objectMapper.writeValueAsString(taskStatusList1));

            long taskFlowLogId2 = taskFlowScheduler.triggerTo(100L, 6L, null);
            System.out.println(">>>>>>>>>> 任务流日志ID：" + taskFlowLogId2);
            this.waitDoneAndPrintLog(taskFlowLogId2);
            List<TaskLog> taskStatusList2  = taskLogger.listTaskStatus(100L);
            System.out.println(">>>>>>>>>> 执行完成，当前各任务状态：");
            System.out.println(objectMapper.writeValueAsString(taskStatusList2));
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
                    TaskLogResult taskLogResult = taskLogger.query(taskLog.getLogFileId(), 1);
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
