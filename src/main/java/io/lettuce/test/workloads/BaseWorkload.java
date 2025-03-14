package io.lettuce.test.workloads;

import io.lettuce.test.DefaultWorkloadOptions;
import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.generator.KeyGenerator;
import io.lettuce.test.generator.RandomKeyGenerator;
import io.lettuce.test.generator.SequentialKeyGenerator;
import io.lettuce.test.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_KEY_GENERATION_STRATEGY;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_KEY_PATTERN;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_KEY_RANGE_MAX;
import static io.lettuce.test.DefaultWorkloadOptions.WorkloadOptionsConstants.DEFAULT_KEY_RANGE_MIN;

/**
 * Base class for workloads.
 * <p>
 * Workloads are executed by the {@link io.lettuce.test.WorkloadRunnerBase} and should implement the {@link Runnable} interface.
 * Workloads implementations are not thread safe and should not shared between threads.
 *
 */
public abstract class BaseWorkload {

    private static final Logger log = LoggerFactory.getLogger(BaseWorkload.class);

    private MetricsReporter metricsReporter;

    private final CommonWorkloadOptions options;

    private final KeyGenerator keyGenerator;

    public BaseWorkload() {
        options = DefaultWorkloadOptions.DEFAULT;
        keyGenerator = createKeyGenerator(options);
    }

    public BaseWorkload(CommonWorkloadOptions options) {

        this.options = options;
        this.keyGenerator = createKeyGenerator(options);
    }

    public abstract void run();

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

    protected KeyGenerator keyGenerator() {
        return keyGenerator;
    }

    private KeyGenerator createKeyGenerator(CommonWorkloadOptions options) {

        String keyGenerationStrategy = options.getString("keyGenerationStrategy", DEFAULT_KEY_GENERATION_STRATEGY);
        String pattern = options.getString("keyPattern", DEFAULT_KEY_PATTERN);
        Integer rangeMin = options.getInteger("keyRangeMin", DEFAULT_KEY_RANGE_MIN);
        Integer rangeMax = options.getInteger("keyRangeMax", DEFAULT_KEY_RANGE_MAX);

        switch (keyGenerationStrategy.toUpperCase()) {
            case "SEQUENTIAL":
                return new SequentialKeyGenerator(pattern, rangeMin, rangeMax);
            case "RANDOM":
                return new RandomKeyGenerator(pattern, rangeMin, rangeMax);
            default:
                throw new IllegalArgumentException("Unknown key generation strategy: " + keyGenerationStrategy);
        }
    }

}
