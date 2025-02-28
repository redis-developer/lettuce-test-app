package io.lettuce.test.metrics;

import io.lettuce.test.Config;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class Metrics {

    private static final Logger log = LoggerFactory.getLogger(Metrics.class);

    private static CompositeMeterRegistry meterRegistry;

    // Private constructor to prevent instantiation
    private Metrics() {
    }

    public static void initMeterRegistry(Config.MetricsConfig config) {
        if (config == null) {
            log.info("Metrics configuration is null. Skipping MeterRegistry creation.");
            return;
        }

        if (meterRegistry == null) {
            synchronized (Metrics.class) {
                if (meterRegistry == null) {
                    CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();

                    if (config.influx != null && config.influx.enable) {
                        InfluxMeterRegistry influxRegistry = InfluxMeterRegistry.builder(new CustomInfluxConfig(config.influx))
                                .build();
                        compositeRegistry.add(influxRegistry);
                    }

                    if (config.logging != null && config.logging.enable) {
                        LoggingMeterRegistry loggingMeterRegistry = LoggingMeterRegistry
                                .builder(new CustomLoggingRegistryConfig(config.logging)).build();
                        compositeRegistry.add(loggingMeterRegistry);
                    }

                    meterRegistry = compositeRegistry;
                }
            }
        }
    }

    public static MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    // Custom InfluxConfig implementation
    private static class CustomInfluxConfig implements InfluxConfig {

        private final Properties properties = new Properties();

        public CustomInfluxConfig(Config.InfluxConfig config) {
            setPropertyIfNotNull("influx.uri", config.uri);
            setPropertyIfNotNull("influx.db", config.db);
            setPropertyIfNotNull("influx.autoCreateDb", config.autoCreateDb);
            setPropertyIfNotNull("influx.step", config.step);
            setPropertyIfNotNull("influx.org", config.org);
            setPropertyIfNotNull("influx.bucket", config.bucket);
            setPropertyIfNotNull("influx.token", config.token);
            setPropertyIfNotNull("influx.apiVersion", config.apiVersion);
            setPropertyIfNotNull("influx.userName", config.userName);
            setPropertyIfNotNull("influx.password", config.password);
        }

        private void setPropertyIfNotNull(String key, Object value) {
            if (value != null) {
                properties.setProperty(key, value.toString());
            }
        }

        @Override
        public String get(String key) {
            return properties.getProperty(key);
        }

    }

    // Push metrics to log
    public static class CustomLoggingRegistryConfig implements LoggingRegistryConfig {

        private final Properties properties = new Properties();

        public CustomLoggingRegistryConfig(Config.LoggingConfig config) {
            properties.setProperty("logging.step", config.step);
        }

        @Override
        public String get(String key) {
            return properties.getProperty(key);
        }

    }

}
