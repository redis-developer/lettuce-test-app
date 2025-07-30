package io.lettuce.test.metrics;

import io.lettuce.core.LettuceVersion;
import io.lettuce.test.config.TestRunProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class MeterRegistryConfiguration {

    private final TestRunProperties testRunProperties;

    public MeterRegistryConfiguration(TestRunProperties testRunProperties) {
        this.testRunProperties = testRunProperties;
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return (registry) -> {
            registry.config().commonTags("app_name", testRunProperties.getAppName())
                    .commonTags("run_id", testRunProperties.getRunId())
                    .commonTags("instance_id", testRunProperties.getInstanceId())
                    .commonTags("version", LettuceVersion.getVersion());

            // Ensure OTLP registry accepts all redis metrics
            if (registry.getClass().getSimpleName().contains("Otlp")) {
                registry.config().meterFilter(MeterFilter.acceptNameStartsWith("redis"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("lettuce"));
            }
        };
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
        registry.config().meterFilter(MeterFilter.acceptNameStartsWith("redis"))
                .meterFilter(MeterFilter.acceptNameStartsWith("lettuce.connect"))
                .meterFilter(MeterFilter.acceptNameStartsWith("lettuce.reconnect"))
                .meterFilter(MeterFilter.acceptNameStartsWith("lettuce.reconnection")).meterFilter(MeterFilter.deny());
    }

}
