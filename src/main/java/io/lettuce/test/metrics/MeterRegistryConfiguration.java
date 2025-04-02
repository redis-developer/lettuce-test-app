package io.lettuce.test.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class MeterRegistryConfiguration {

    @Value("${runId:${runner.test.workload.type}-#{T(org.apache.commons.lang3.RandomStringUtils).randomAlphanumeric(8)}}")
    private String runId;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return (registry) -> registry.config().commonTags("runId", runId);
    }

    @Bean
    @ConditionalOnProperty(prefix = "logging.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ExtendedLoggingMeterRegistry loggingMeterRegistry(Environment env) {
        LoggingRegistryConfig loggingConfig = new LoggingRegistryConfig() {

            public String prefix() {
                return "logging.metrics";
            }

            @Override
            public String get(String property) {

                return env.getProperty(property, String.class);
            }

        };

        ExtendedLoggingMeterRegistry registry = ExtendedLoggingMeterRegistry.builder(loggingConfig).build();
        meterFilters(registry);

        return registry;
    }

    @Bean
    @ConditionalOnProperty(prefix = "simple.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SimpleMeterRegistry simpleMeterRegistry() {

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        meterFilters(registry);
        return registry;
    }

    // * Deny all meters by default and accept only specific meters for metrics stored in logs
    private void meterFilters(MeterRegistry registry) {
        registry.config().meterFilter(MeterFilter.acceptNameStartsWith("redis.command"))
                .meterFilter(MeterFilter.acceptNameStartsWith("lettuce.connect"))
                .meterFilter(MeterFilter.acceptNameStartsWith("lettuce.reconnect"))
                .meterFilter(MeterFilter.acceptNameStartsWith("lettuce.reconnection")).meterFilter(MeterFilter.deny());
    }

}
