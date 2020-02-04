package me.kpali.wolfflow.sample.listener;

import me.kpali.wolfflow.core.model.TaskFlowLog;
import me.kpali.wolfflow.core.model.TaskLog;
import me.kpali.wolfflow.core.model.TaskLogResult;
import me.kpali.wolfflow.sample.taskflow.MyTaskFlowLogger;
import me.kpali.wolfflow.sample.taskflow.MyTaskFlowScheduler;
import me.kpali.wolfflow.sample.taskflow.MyTaskLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    MyTaskFlowScheduler myTaskFlowScheduler;

    @Autowired
    MyTaskFlowLogger myTaskFlowLogger;
    @Autowired
    MyTaskLogger myTaskLogger;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        myTaskFlowScheduler.startup();
        this.test();
    }

    private void test() {
        try {
            long logId1 = myTaskFlowScheduler.triggerTo(100L, 5L, null);
            System.out.println(">>>>>>>>>> 日志ID：" + logId1);
            Thread.sleep(3 * 1000);
            //myTaskFlowScheduler.stop(logId1);
            this.waitDoneAndPrintLog(logId1);
            long logId2 = myTaskFlowScheduler.triggerTo(100L, 6L, null);
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
            TaskFlowLog taskFlowLog = myTaskFlowLogger.get(logId);
            if (!myTaskFlowLogger.isInProgress(taskFlowLog)) {
                List<TaskLog> taskLogList = myTaskLogger.list(logId);
                for (TaskLog taskLog : taskLogList) {
                    TaskLogResult taskLogResult = myTaskLogger.query(logId, taskLog.getTaskId(), 1);
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
