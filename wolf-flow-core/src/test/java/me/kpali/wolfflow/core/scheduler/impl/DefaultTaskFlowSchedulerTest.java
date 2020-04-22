package me.kpali.wolfflow.core.scheduler.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kpali.wolfflow.core.BaseTest;
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

    @BeforeClass
    public void setUp() {
        new MockUp<DefaultTaskFlowQuerier>() {
            @Mock
            public TaskFlow getTaskFlow(Long taskFlowId) throws TaskFlowQueryException {
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

                List<TaskFlow> taskFlowList = new ArrayList<>();

                TaskFlow taskFlow = new TaskFlow();
                taskFlow.setId(100L);
                taskFlow.setTaskList(new ArrayList<>());
                taskFlow.setLinkList(new ArrayList<>());

                MyTask myTask1 = new MyTask();
                myTask1.setId(1L);
                MyTask myTask2 = new MyTask();
                myTask2.setId(2L);
                MyTask myTask3 = new MyTask();
                myTask3.setId(3L);
                MyTask myTask4 = new MyTask();
                myTask4.setId(4L);
                MyTask myTask5 = new MyTask();
                myTask5.setId(5L);
                MyTask myTask6 = new MyTask();
                myTask6.setId(6L);
                MyTask myTask7 = new MyTask();
                myTask7.setId(7L);
                MyTask myTask8 = new MyTask();
                myTask8.setId(8L);
                MyTask myTask9 = new MyTask();
                myTask9.setId(9L);
                MyTask myTask10 = new MyTask();
                myTask10.setId(10L);
                MyTask myTask11 = new MyTask();
                myTask11.setId(11L);
                taskFlow.getTaskList().add(myTask3);
                taskFlow.getTaskList().add(myTask5);
                taskFlow.getTaskList().add(myTask1);
                taskFlow.getTaskList().add(myTask10);
                taskFlow.getTaskList().add(myTask9);
                taskFlow.getTaskList().add(myTask6);
                taskFlow.getTaskList().add(myTask4);
                taskFlow.getTaskList().add(myTask2);
                taskFlow.getTaskList().add(myTask7);
                taskFlow.getTaskList().add(myTask11);
                taskFlow.getTaskList().add(myTask8);

                taskFlow.getLinkList().add(new Link(1L, 2L));
                taskFlow.getLinkList().add(new Link(1L, 3L));
                taskFlow.getLinkList().add(new Link(2L, 3L));
                taskFlow.getLinkList().add(new Link(2L, 4L));
                taskFlow.getLinkList().add(new Link(3L, 4L));
                taskFlow.getLinkList().add(new Link(3L, 5L));
                taskFlow.getLinkList().add(new Link(5L, 6L));
                taskFlow.getLinkList().add(new Link(5L, 7L));
                taskFlow.getLinkList().add(new Link(6L, 7L));
                taskFlow.getLinkList().add(new Link(5L, 8L));
                taskFlow.getLinkList().add(new Link(5L, 9L));
                taskFlow.getLinkList().add(new Link(8L, 9L));
                taskFlow.getLinkList().add(new Link(10L, 11L));

                //taskFlow.setCron("0 * * * * ?");
                //taskFlow.setFromTaskId(1L);

                taskFlowList.add(taskFlow);

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
    public void testTriggerSingle() {
        long taskFlowId = 100L;
        long taskId = 10L;
        long taskFlowLogId = this.taskFlowScheduler.trigger(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(taskLogList.size(), 1);
        TaskLog taskLog = taskLogList.get(0);
        assertEquals(taskLog.getTask().getId().longValue(), taskId);
        assertEquals(taskLog.getStatus(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
    }

    @Test(dependsOnMethods = {"testTriggerSingle"})
    public void testTriggerFrom() {
        long taskFlowId = 100L;
        long taskId = 10L;
        long taskFlowLogId = this.taskFlowScheduler.triggerFrom(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(taskLogList.size(), 2);
        for (TaskLog taskLog : taskLogList) {
            assertEquals(taskLog.getStatus(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
        }
    }

    @Test(dependsOnMethods = {"testTriggerFrom"})
    public void testTriggerTo() {
        long taskFlowId = 100L;
        long taskId = 11L;
        long taskFlowLogId = this.taskFlowScheduler.triggerTo(taskFlowId, taskId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());

        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(taskLogList.size(), 2);
        for (TaskLog taskLog : taskLogList) {
            assertEquals(taskLog.getStatus(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
        }
    }

    @Test(dependsOnMethods = {"testTriggerTo"})
    public void testTrigger() {
        long taskFlowId = 100L;
        long taskFlowLogId = this.taskFlowScheduler.trigger(taskFlowId, null);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);
        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_SUCCESS.getCode());
        List<TaskLog> taskLogList = this.taskLogger.list(taskFlowLogId);
        assertEquals(taskLogList.size(), 11);
        for (TaskLog taskLog : taskLogList) {
            assertEquals(taskLog.getStatus(), TaskStatusEnum.EXECUTE_SUCCESS.getCode());
        }
    }

    @Test(dependsOnMethods = {"testTrigger"})
    public void testStop() {
        long taskFlowId = 100L;
        long taskFlowLogId = this.taskFlowScheduler.trigger(taskFlowId, null);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.taskFlowScheduler.stop(taskFlowLogId);
        this.waitDoneAndPrintLog(taskFlowId, taskFlowLogId);

        TaskFlowLog taskFlowLog = this.taskFlowLogger.get(taskFlowLogId);
        assertEquals(taskFlowLog.getStatus(), TaskFlowStatusEnum.EXECUTE_FAILURE.getCode());
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