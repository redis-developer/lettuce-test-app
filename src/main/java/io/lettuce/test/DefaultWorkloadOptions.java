package io.lettuce.test;

import java.util.Collections;
import java.util.Map;

import static io.lettuce.test.WorkloadOptionsConstants.DEFAULT_ELEMENTS_COUNT;
import static io.lettuce.test.WorkloadOptionsConstants.DEFAULT_GET_SET_RATIO;
import static io.lettuce.test.WorkloadOptionsConstants.DEFAULT_ITERATION_COUNT;
import static io.lettuce.test.WorkloadOptionsConstants.DEFAULT_TRANSACTION_SIZE;
import static io.lettuce.test.WorkloadOptionsConstants.DEFAULT_VALUE_SIZE;

public class DefaultWorkloadOptions implements CommonWorkflowOptions {

    public static final DefaultWorkloadOptions DEFAULT = new DefaultWorkloadOptions();

    private Map<String, Object> options = Collections.emptyMap();

    // Size of the tests values
    private final int valueSize;

    // Number of elements to operate on
    // e.g. How many elements to push with single `lpush` command
    private final int elementsCount;

    // Number of iterations to execute
    // How many times to repeat block of commands.
    private final int iterationCount;

    // Ratio of GET to SET operations
    // e.g. On each iteration, choose randomly between GET and SET operations based on this ratio
    // value between 0 and 1
    // 0.2 means 20% of operations are SET, 80% are GET
    private final double getSetRatio;

    // Number of commands to execute per iteration
    // e.g. How many commands to execute in a single multi-exec transaction
    private final int transactionSize;

    public DefaultWorkloadOptions() {
        this(Collections.emptyMap());
    }

    public DefaultWorkloadOptions(Map<String, Object> options) {
        this.options = Collections.unmodifiableMap(options);

        this.iterationCount = getInteger("iterationCount", DEFAULT_ITERATION_COUNT);
        this.valueSize = getInteger("valueSize", DEFAULT_VALUE_SIZE);
        this.elementsCount = getInteger("elementsCount", DEFAULT_ELEMENTS_COUNT);
        this.getSetRatio = getDouble("getSetRatio", DEFAULT_GET_SET_RATIO);
        this.transactionSize = getInteger("transactionSize", DEFAULT_TRANSACTION_SIZE);
    }

    @Override
    public <T> T getOption(String key, Class<T> type) {
        return type.cast(options.get(key));
    }

    @Override
    public <T> T getOption(String key, Class<T> type, T defaultValue) {
        return options.containsKey(key) ? getOption(key, type) : defaultValue;
    }

    @Override
    public int valueSize() {
        return valueSize;
    }

    @Override
    public int elementsCount() {
        return elementsCount;
    }

    @Override
    public int iterationCount() {
        return iterationCount;
    }

    @Override
    public double getSetRatio() {
        return getSetRatio;
    }

    @Override
    public int transactionSize() {
        return transactionSize;
    }

    public static CommonWorkflowOptions create(Map<String, Object> options) {
        return new DefaultWorkloadOptions(options);
    }

}
