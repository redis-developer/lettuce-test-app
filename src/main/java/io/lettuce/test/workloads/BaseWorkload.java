package io.lettuce.test.workloads;

import io.lettuce.test.WorkloadOptions;
import io.lettuce.test.metrics.MetricsProxy;
import io.lettuce.test.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.Random;

import static io.lettuce.test.workloads.WorkloadOptionsConstants.DEFAULT_ELEMENTS_COUNT;
import static io.lettuce.test.workloads.WorkloadOptionsConstants.DEFAULT_GET_SET_RATIO;
import static io.lettuce.test.workloads.WorkloadOptionsConstants.DEFAULT_ITERATION_COUNT;
import static io.lettuce.test.workloads.WorkloadOptionsConstants.DEFAULT_TRANSACTION_SIZE;
import static io.lettuce.test.workloads.WorkloadOptionsConstants.DEFAULT_VALUE_SIZE;

/**
 * Base class for workloads.
 * <p>
 * Workloads are executed by the {@link io.lettuce.test.WorkloadRunnerBase} and should implement the {@link Runnable} interface.
 * Workloads implementations are not thread safe and should not shared between threads.
 *
 */
public abstract class BaseWorkload implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(BaseWorkload.class);

    // Size of the tests values
    protected final int valueSize;

    // Number of elements to operate on
    // e.g. How many elements to push with single `lpush` command
    protected final int elementsCount;

    // Number of iterations to execute
    // How many times to repeat block of commands.
    protected final int iterationCount;

    // Ratio of GET to SET operations
    // e.g. On each iteration, choose randomly between GET and SET operations based on this ratio
    // value between 0 and 1
    // 0.2 means 20% of operations are SET, 80% are GET
    protected final double getSetRatio;

    // Number of commands to execute per iteration
    // e.g. How many commands to execute in a single multi-exec transaction
    protected final int transactionSize;

    private MetricsReporter metricsReporter;

    private final WorkloadOptions options;

    public BaseWorkload() {
        options = WorkloadOptions.DEFAULT;

        // Workload configuration available in all Workloads
        this.iterationCount = DEFAULT_ITERATION_COUNT;
        this.getSetRatio = DEFAULT_GET_SET_RATIO;
        this.valueSize = DEFAULT_VALUE_SIZE;
        this.elementsCount = DEFAULT_ELEMENTS_COUNT;
        this.transactionSize = DEFAULT_TRANSACTION_SIZE;
    }

    public BaseWorkload(WorkloadOptions options) {

        this.options = options;

        // Workload configuration available in all Workloads
        this.iterationCount = options.getInteger("iterationCount", DEFAULT_ITERATION_COUNT);
        this.valueSize = options.getInteger("valueSize", DEFAULT_VALUE_SIZE);
        this.elementsCount = options.getInteger("elementsCount", DEFAULT_ELEMENTS_COUNT);
        this.getSetRatio = options.getDouble("getSetRatio", DEFAULT_GET_SET_RATIO);
        this.transactionSize = options.getInteger("transactionSize", DEFAULT_TRANSACTION_SIZE);
    }

    public void metricsReporter(MetricsReporter metricsReporter) {
        this.metricsReporter = metricsReporter;
    }

    protected WorkloadOptions options() {
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

    public static String generateRandomString(int size) {
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            builder.append((char) ('a' + new Random().nextInt(26))); // Random lowercase letter
        }
        return builder.toString();
    }

}
