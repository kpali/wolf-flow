package me.kpali.wolfflow.autoconfigure;

import me.kpali.wolfflow.autoconfigure.config.CoreConfiguration;
import me.kpali.wolfflow.autoconfigure.properties.ScheduleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableConfigurationProperties(ScheduleProperties.class)
@Import({ CoreConfiguration.class })
@Configuration
public class WolfFlowAutoConfiguration {
}
