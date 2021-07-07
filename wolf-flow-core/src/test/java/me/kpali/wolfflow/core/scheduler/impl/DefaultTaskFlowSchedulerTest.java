package me.kpali.wolfflow.core.scheduler.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.BaseTest;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.enums.TaskStatusEnum;
import me.kpali.wolfflow.core.exception.*;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.querier.ITaskFlowQuerier;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class DefaultTaskFlowSchedulerTest extends BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskFlowSchedulerTest.class);
    
    @MockBean
    ITaskFlowQuerier taskFlowQuerier;

    @Autowired
    ITaskFlowScheduler taskFlowScheduler;
    @Autowired
    ITaskFlowLogger taskFlowLogger;
    @Autowired
    ITaskLogger taskLogger;
    @Autowired
    IClusterController clusterController;

    @BeforeEach
    public void setUp() throws TaskFlowQueryException {
        when(taskFlowQuerier.getTaskFlow(1L)).thenAnswer((Answer<TaskFlow>) invocationOnMock -> {
            /**
             * 示例拓扑图：
             *                    --> 9
             * 1 --> 2 --> 4     |    ^
             *  \    \    ^     |    |
             *   \    v  /    /  --> 8
             *    --> 3 --> 5 --> 6
             *               \    |
             *                \   v
             *                --> 7
             * 10 --> 11
             */
            TaskFlow taskFlow1 = new TaskFlow();
            taskFlow1.setId(1L);
            taskFlow1.setTaskList(new ArrayList<>());
            taskFlow1.setLinkList(new ArrayList<>());

            AutoTask autoTask1 = new AutoTask();
            autoTask1.setId(1L);
            AutoTask autoTask2 = new AutoTask();
            autoTask2.setId(2L);
            AutoTask autoTask3 = new AutoTask();
            autoTask3.setId(3L);
            AutoTask autoTask4 = new AutoTask();
            autoTask4.setId(4L);
            AutoTask autoTask5 = new AutoTask();
            autoTask5.setId(5L);
            AutoTask autoTask6 = new AutoTask();
            autoTask6.setId(6L);
            AutoTask autoTask7 = new AutoTask();
            autoTask7.setId(7L);
            AutoTask autoTask8 = new AutoTask();
            autoTask8.setId(8L);
            AutoTask autoTask9 = new AutoTask();
            autoTask9.setId(9L);
            AutoTask autoTask10 = new AutoTask();
            autoTask10.setId(10L);
            AutoTask autoTask11 = new AutoTask();
            autoTask11.setId(11L);
            taskFlow1.getTaskList().add(autoTask3);
            taskFlow1.getTaskList().add(autoTask5);
            taskFlow1.getTaskList().add(autoTask1);
            taskFlow1.getTaskList().add(autoTask10);
            taskFlow1.getTaskList().add(autoTask9);
            taskFlow1.getTaskList().add(autoTask6);
            taskFlow1.getTaskList().add(autoTask4);
            taskFlow1.getTaskList().add(autoTask2);
            taskFlow1.getTaskList().add(autoTask7);
            taskFlow1.getTaskList().add(autoTask11);
            taskFlow1.getTaskList().add(autoTask8);

            taskFlow1.getLinkList().add(new Link(1L, 2L));
            taskFlow1.getLinkList().add(new Link(1L, 3L));
            taskFlow1.getLinkList().add(new Link(2L, 3L));
            taskFlow1.getLinkList().add(new Link(2L, 4L));
            taskFlow1.getLinkList().add(new Link(3L, 4L));
            taskFlow1.getLinkList().add(new Link(3L, 5L));
            taskFlow1.getLinkList().add(new Link(5L, 6L));
            taskFlow1.getLinkList().add(new Link(5L, 7L));
            taskFlow1.getLinkList().add(new Link(6L, 7L));
            taskFlow1.getLinkList().add(new Link(5L, 8L));
            taskFlow1.getLinkList().add(new Link(5L, 9L));
            taskFlow1.getLinkList().add(new Link(8L, 9L));
            taskFlow1.getLinkList().add(new Link(10L, 11L));

            //taskFlow1.setCron("0 * * * * ?");
            //taskFlow1.setFromTaskId(1L);

            return taskFlow1;
        });
        when(taskFlowQuerier.getTaskFlow(2L)).thenAnswer((Answer<TaskFlow>) invocationOnMock -> {
            /**
             * 示例拓扑图：
             *
             * 12（手工确认） --> 13
             */

            TaskFlow taskFlow2 = new TaskFlow();
            taskFlow2.setId(2L);
            taskFlow2.setTaskList(new ArrayList<>());
            taskFlow2.setLinkList(new ArrayList<>());

            ManualTask manualTask12 = new ManualTask();
            manualTask12.setId(12L);
            AutoTask autoTask13 = new AutoTask();
            autoTask13.setId(13L);

            taskFlow2.getTaskList().add(autoTask13);
            taskFlow2.getTaskList().add(manualTask12);

            taskFlow2.getLinkList().add(new Link(12L, 13L));

            return taskFlow2;
        });
    }

    @Test
    @Order(1)
    public void testRollbackNothing() throws TaskFlowTriggerException, TaskFlowLogException, TaskLogException {
        long taskFlowId = 1L;
        long taskFlowLogId = this.taskFlowScheduler.rollback(taskFlowId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode(), taskFlowLog.getStatus());
    }

    @Test
    @Order(2)
    public void testExecuteSingle() throws TaskFlowTriggerException, TaskFlowLogException, TaskLogException {
        long taskFlowId = 1L;
        long taskId = 10L;
        long taskFlowLogId = this.taskFlowScheduler.execute(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), taskFlowLog.getStatus());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(1, taskLogList.size());
        TaskLog taskLog = taskLogList.get(0);
        assertEquals(taskId, taskLog.getTask().getId().longValue());
        assertEquals(TaskStatusEnum.EXECUTE_SUCCESS.getCode(), taskLog.getStatus());
    }

    @Test
    @Order(3)
    public void testExecuteFrom() throws TaskFlowTriggerException, TaskFlowLogException, TaskLogException {
        long taskFlowId = 1L;
        long taskId = 10L;
        long taskFlowLogId = this.taskFlowScheduler.executeFrom(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), taskFlowLog.getStatus());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(2, taskLogList.size());
        for (TaskLog taskLog : taskLogList) {
            assertEquals(TaskStatusEnum.EXECUTE_SUCCESS.getCode(), taskLog.getStatus());
        }
    }

    @Test
    @Order(4)
    public void testExecuteTo() throws TaskFlowTriggerException, TaskFlowLogException, TaskLogException {
        long taskFlowId = 1L;
        long taskId = 11L;
        long taskFlowLogId = this.taskFlowScheduler.executeTo(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), taskFlowLog.getStatus());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(2, taskLogList.size());
        for (TaskLog taskLog : taskLogList) {
            assertEquals(TaskStatusEnum.EXECUTE_SUCCESS.getCode(), taskLog.getStatus());
        }
    }

    @Test
    @Order(5)
    public void testExecute() throws TaskFlowTriggerException, TaskFlowLogException, TaskLogException {
        long taskFlowId = 1L;
        long taskFlowLogId = this.taskFlowScheduler.execute(taskFlowId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), taskFlowLog.getStatus());
        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(11, taskLogList.size());
        for (TaskLog taskLog : taskLogList) {
            assertEquals(TaskStatusEnum.EXECUTE_SUCCESS.getCode(), taskLog.getStatus());
        }
    }

    @Test
    @Order(6)
    public void testStop() throws TaskFlowTriggerException, TaskLogException, TaskFlowStopException, TaskFlowLogException {
        long taskFlowId = 1L;
        long taskFlowLogId = this.taskFlowScheduler.execute(taskFlowId, null);
        while (true) {
            // 等待执行到任务3后，停止任务流
            TaskLog taskLog = taskLogger.get(taskFlowLogId, 3L);
            if (taskLog != null) {
                this.taskFlowScheduler.stop(taskFlowLogId);
                break;
            }
        }
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.EXECUTE_STOPPED.getCode(), taskFlowLog.getStatus());
    }

    @Test
    @Order(7)
    public void testRollback() throws TaskFlowTriggerException, TaskLogException, TaskFlowLogException {
        long taskFlowId = 1L;
        long taskFlowLogId = this.taskFlowScheduler.rollback(taskFlowId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode(), taskFlowLog.getStatus());
    }

    @Test
    @Order(8)
    public void testExecuteManualTask() throws TaskFlowTriggerException, TaskLogException, TaskFlowLogException {
        long taskFlowId = 2L;
        long taskFlowLogId = this.taskFlowScheduler.execute(taskFlowId, null);

        while (true) {
            TaskLog manualTaskLog = taskLogger.get(taskFlowLogId, 12L);
            if (manualTaskLog != null && TaskStatusEnum.MANUAL_CONFIRM.getCode().equals(manualTaskLog.getStatus())) {
                clusterController.manualConfirmedAdd(new ManualConfirmed(manualTaskLog.getLogId(), true, null));
                break;
            }
        }

        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode(), taskFlowLog.getStatus());
    }

    @Test
    @Order(9)
    public void testRollbackManualTask() throws TaskFlowTriggerException, TaskLogException, TaskFlowLogException {
        long taskFlowId = 2L;
        long taskFlowLogId = this.taskFlowScheduler.rollback(taskFlowId, null);

        while (true) {
            TaskLog manualTaskLog = taskLogger.get(taskFlowLogId, 12L);
            if (manualTaskLog != null && TaskStatusEnum.MANUAL_CONFIRM.getCode().equals(manualTaskLog.getStatus())) {
                clusterController.manualConfirmedAdd(new ManualConfirmed(manualTaskLog.getLogId(), true, null));
                break;
            }
        }

        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode(), taskFlowLog.getStatus());
    }

    private void waitDoneAndPrintLog(long taskFlowId, long taskFlowLogId) throws TaskFlowLogException, TaskLogException {
        logger.info(">>>>>>>>>> Task flow log id: " + taskFlowLogId);
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
        ObjectMapper objectMapper = new ObjectMapper();
        List<TaskLog> taskStatusList = taskLogger.listTaskStatus(taskFlowId);
        logger.info(">>>>>>>>>> Finished, status of tasks: ");
        try {
            logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(taskStatusList));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}