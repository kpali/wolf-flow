package me.kpali.wolfflow.sample.taskflow;

import me.kpali.wolfflow.core.model.Task;

public class MyTask extends Task {
    public MyTask(Long id) {
        super(id);
    }

    @Override
    public void execute() {
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("========== 任务执行完成！任务ID：" + this.getId());
    }
}
