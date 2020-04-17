package me.kpali.wolfflow.sample.listener;

import me.kpali.wolfflow.core.launcher.Launcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 程序启动完成事件监听，在程序启动后启动任务流相关的后台线程
 * （必要）
 *
 * @author kpali
 */
@Component
public class ApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private Launcher launcher;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        this.launcher.startup();
    }
}
