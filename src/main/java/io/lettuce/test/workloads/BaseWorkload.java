package io.lettuce.test.workloads;

import io.lettuce.test.DefaultWorkloadOptions;
import io.lettuce.test.CommonWorkflowOptions;
import io.lettuce.test.metrics.MetricsProxy;
import io.lettuce.test.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

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

    private final CommonWorkflowOptions options;

    public BaseWorkload() {
        options = DefaultWorkloadOptions.DEFAULT;

    }

    public BaseWorkload(CommonWorkflowOptions options) {

        this.options = options;
    }

    public void metricsReporter(MetricsReporter metricsReporter) {
        this.metricsReporter = metricsReporter;
    }

    protected CommonWorkflowOptions options() {
        return options;
    }

    @SuppressWarnings("unchecked")
    protected <T> T withMetrics(T target) {
        if (metricsReporter == null) {
            log.warn("No meter registry set. Skipping metrics.");
            return target;
        }

        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(),
                new MetricsProxy<>(target, metricsReporter));
    }

}
