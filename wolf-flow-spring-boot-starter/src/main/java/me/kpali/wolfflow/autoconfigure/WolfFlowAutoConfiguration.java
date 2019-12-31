package me.kpali.wolfflow.autoconfigure;

import me.kpali.wolfflow.autoconfigure.config.QuartzConfiguration;
import me.kpali.wolfflow.autoconfigure.properties.ScheduleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableConfigurationProperties(ScheduleProperties.class)
@Import({ QuartzConfiguration.class })
@Configuration
public class WolfFlowAutoConfiguration {
}
