package me.kpali.wolfflow.core.scheduler.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.BaseTest;
import me.kpali.wolfflow.core.cluster.IClusterController;
import me.kpali.wolfflow.core.enums.TaskFlowStatusEnum;
import me.kpali.wolfflow.core.enums.TaskStatusEnum;
import me.kpali.wolfflow.core.exception.TaskFlowQueryException;
import me.kpali.wolfflow.core.logger.ITaskFlowLogger;
import me.kpali.wolfflow.core.logger.ITaskLogger;
import me.kpali.wolfflow.core.model.*;
import me.kpali.wolfflow.core.querier.impl.DefaultTaskFlowQuerier;
import me.kpali.wolfflow.core.scheduler.ITaskFlowScheduler;
import mockit.Mock;
import mockit.MockUp;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class DefaultTaskFlowSchedulerTest extends BaseTest {

    @Autowired
    ITaskFlowScheduler taskFlowScheduler;
    @Autowired
    ITaskFlowLogger taskFlowLogger;
    @Autowired
    ITaskLogger taskLogger;
    @Autowired
    IClusterController clusterController;

    @BeforeClass
    public void setUp() {
        new MockUp<DefaultTaskFlowQuerier>() {
            @Mock
            public TaskFlow getTaskFlow(Long taskFlowId) throws TaskFlowQueryException {
                List<TaskFlow> taskFlowList = new ArrayList<>();

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

                taskFlowList.add(taskFlow1);

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

                taskFlowList.add(taskFlow2);

                for (TaskFlow tf : taskFlowList) {
                    if (tf.getId().equals(taskFlowId)) {
                        return tf;
                    }
                }
                return null;
            }
        };
    }

    @Test
    public void testRollbackNothing() {
        long taskFlowId = 1L;
        long taskFlowLogId = this.taskFlowScheduler.rollback(taskFlowId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode());
    }

    @Test(dependsOnMethods = {"testRollbackNothing"})
    public void testExecuteSingle() {
        long taskFlowId = 1L;
        long taskId = 10L;
        long taskFlowLogId = this.taskFlowScheduler.execute(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(taskLogList.size(), 1);
        TaskLog taskLog = taskLogList.get(0);
        assertEquals(taskLog.getTask().getId().longValue(), taskId);
        assertEquals(taskLog.getStatus(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
    }

    @Test(dependsOnMethods = {"testExecuteSingle"})
    public void testExecuteFrom() {
        long taskFlowId = 1L;
        long taskId = 10L;
        long taskFlowLogId = this.taskFlowScheduler.executeFrom(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(taskLogList.size(), 2);
        for (TaskLog taskLog : taskLogList) {
            assertEquals(taskLog.getStatus(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
        }
    }

    @Test(dependsOnMethods = {"testExecuteFrom"})
    public void testExecuteTo() {
        long taskFlowId = 1L;
        long taskId = 11L;
        long taskFlowLogId = this.taskFlowScheduler.executeTo(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(taskLogList.size(), 2);
        for (TaskLog taskLog : taskLogList) {
            assertEquals(taskLog.getStatus(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
        }
    }

    @Test(dependsOnMethods = {"testExecuteTo"})
    public void testExecute() {
        long taskFlowId = 1L;
        long taskFlowLogId = this.taskFlowScheduler.execute(taskFlowId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());
        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(taskLogList.size(), 11);
        for (TaskLog taskLog : taskLogList) {
            assertEquals(taskLog.getStatus(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
        }
    }

    @Test(dependsOnMethods = {"testExecute"})
    public void testStop() {
        long taskFlowId = 1L;
        long taskFlowLogId = this.taskFlowScheduler.execute(taskFlowId, null);
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 等待执行到任务3后，停止任务流
            TaskLog taskLog = taskLogger.get(taskFlowLogId, 3L);
            if (taskLog != null) {
                this.taskFlowScheduler.stop(taskFlowLogId);
                break;
            }
        }
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_FAILURE.getCode());
    }

    @Test(dependsOnMethods = {"testStop"})
    public void testRollback() {
        long taskFlowId = 1L;
        long taskFlowLogId = this.taskFlowScheduler.rollback(taskFlowId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode());
    }

    @Test
    public void testExecuteManualTask() {
        long taskFlowId = 2L;
        long taskFlowLogId = this.taskFlowScheduler.execute(taskFlowId, null);

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TaskLog manualTaskLog = taskLogger.get(taskFlowLogId, 12L);
            if (manualTaskLog != null && TaskStatusEnum.MANUAL_CONFIRM.getCode().equals(manualTaskLog.getStatus())) {
                clusterController.manualConfirmedAdd(manualTaskLog.getLogId());
                break;
            }
        }

        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());
    }

    @Test(dependsOnMethods = {"testExecuteManualTask"})
    public void testRollbackManualTask() {
        long taskFlowId = 2L;
        long taskFlowLogId = this.taskFlowScheduler.rollback(taskFlowId, null);

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TaskLog manualTaskLog = taskLogger.get(taskFlowLogId, 12L);
            if (manualTaskLog != null && TaskStatusEnum.MANUAL_CONFIRM.getCode().equals(manualTaskLog.getStatus())) {
                clusterController.manualConfirmedAdd(manualTaskLog.getLogId());
                break;
            }
        }

        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.ROLLBACK_SUCCESS.getCode());
    }

    private void waitDoneAndPrintLog(long taskFlowId, long taskFlowLogId) {
        System.out.println(">>>>>>>>>> 任务流日志ID：" + taskFlowLogId);
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TaskFlowLog taskFlowLog = taskFlowLogger.get(taskFlowLogId);
            if (!taskFlowLogger.isInProgress(taskFlowLog)) {
                List<TaskLog> taskLogList = taskLogger.list(taskFlowLogId);
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
        ObjectMapper objectMapper = new ObjectMapper();
        List<TaskLog> taskStatusList = taskLogger.listTaskStatus(taskFlowId);
        System.out.println(">>>>>>>>>> 执行完成，当前各任务状态：");
        try {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(taskStatusList));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}