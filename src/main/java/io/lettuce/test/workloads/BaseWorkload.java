package io.lettuce.test.workloads;

import io.lettuce.test.DefaultWorkloadOptions;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Base class for workloads.
 * <p>
 * Workloads are executed by the {@link io.lettuce.test.WorkloadRunnerBase} and should implement the {@link Runnable} interface.
 * Workloads implementations are not thread safe and should not shared between threads.
 *
 */
public abstract class BaseWorkload implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(BaseWorkload.class);

    private MetricsReporter metricsReporter;

    private final CommonWorkloadOptions options;

    public BaseWorkload() {
        options = DefaultWorkloadOptions.DEFAULT;

    }

    public BaseWorkload(CommonWorkloadOptions options) {

        this.options = options;
    }

    public void metricsReporter(MetricsReporter metricsReporter) {
        this.metricsReporter = metricsReporter;
    }

    public CommonWorkloadOptions options() {
        return options;
    }

    protected <T> T withMetrics(T cmd) {
        return metricsReporter.withMetrics(cmd);
    }

    protected void delay(Duration delay) {
        if (Duration.ZERO.equals(delay)) {
            return;
        }

        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            log.error("Delay interrupted", e);
        }
    }

}
