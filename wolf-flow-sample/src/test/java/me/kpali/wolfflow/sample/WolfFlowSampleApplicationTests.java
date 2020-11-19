package me.kpali.wolfflow.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.exception.TaskFlowLogException;
import me.kpali.wolfflow.core.exception.TaskLogException;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.model.TaskLog;
import me.kpali.wolfflow.core.model.TaskLogResult;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class WolfFlowSampleApplicationTests {
    private static final Logger logger = LoggerFactory.getLogger(WolfFlowSampleApplicationTests.class);
    
    @Autowired
    ITaskFlowScheduler taskFlowScheduler;
    @Autowired
    ITaskFlowLogger taskFlowLogger;
    @Autowired
    ITaskLogger taskLogger;

    @Test
    public void taskFlowExecuteTest() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            long taskFlowLogId1 = taskFlowScheduler.execute(100L, 10L, null);
            logger.info(">>>>>>>>>> Task flow log id: " + taskFlowLogId1);
            //Thread.sleep(1000);
            //taskFlowScheduler.stop(taskFlowLogId1);
            this.waitDoneAndPrintLog(taskFlowLogId1);
            List<TaskLog> taskStatusList1 = taskLogger.listTaskStatus(100L);
            logger.info(">>>>>>>>>> Finished, status of tasks: ");
            logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(taskStatusList1));

            long taskFlowLogId2 = taskFlowScheduler.executeFrom(100L, 11L, null);
            logger.info(">>>>>>>>>> Task flow log id: " + taskFlowLogId2);
            this.waitDoneAndPrintLog(taskFlowLogId2);
            List<TaskLog> taskStatusList2  = taskLogger.listTaskStatus(100L);
            logger.info(">>>>>>>>>> Finished, status of tasks: ");
            logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(taskStatusList2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitDoneAndPrintLog(long taskFlowLogId) throws TaskFlowLogException, TaskLogException {
        while (true) {
            TaskFlowLog taskFlowLog = taskFlowLogger.get(taskFlowLogId);
            if (!taskFlowLogger.isInProgress(taskFlowLog)) {
                List<TaskLog> taskLogList = taskLogger.list(taskFlowLogId);
                for (TaskLog taskLog : taskLogList) {
                    TaskLogResult taskLogResult = taskLogger.query(taskLog.getLogFileId(), 1);
                    if (taskLogResult != null) {
                        logger.info(">>>>>>>>>> Task [" + taskLog.getTaskId() + "] log contents: ");
                        logger.info(taskLogResult.getLogContent());
                    }
                }
                break;
            }
        }
    }
}
