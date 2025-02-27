package io.lettuce.test;

import java.util.Collections;
import java.util.Map;

public class WorkloadOptions {

    public static final WorkloadOptions DEFAULT = new WorkloadOptions();

    private Map<String, Object> options = Collections.emptyMap();

    public WorkloadOptions() {
    }

    public WorkloadOptions(Map<String, Object> options) {
        this.options = Collections.unmodifiableMap(options);
    }

    public <T> T getOption(String key, Class<T> type) {
        return type.cast(options.get(key));
    }

    public Integer getInteger(String key) {
        return getOption(key, Integer.class);
    }

    public Double getDouble(String key) {
        return getOption(key, Double.class);
    }

    public static WorkloadOptions create(Map<String, Object> options) {
        return new WorkloadOptions(options);
    }

}
