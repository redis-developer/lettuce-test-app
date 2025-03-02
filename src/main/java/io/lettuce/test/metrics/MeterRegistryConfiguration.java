package io.lettuce.test.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class MeterRegistryConfiguration {

    @Value("${runId:${random.uuid}}")
    private String runId;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return (registry) -> {
            registry.config().commonTags("runId", runId);
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "logging.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LoggingMeterRegistry loggingMeterRegistry(Environment env) {
        LoggingRegistryConfig loggingConfig = new LoggingRegistryConfig() {

            public String prefix() {
                return "logging.metrics";
            }

            @Override
            public String get(String property) {

                return env.getProperty(property, String.class);
            }

        };

        return LoggingMeterRegistry.builder(loggingConfig).build();
    }

}
