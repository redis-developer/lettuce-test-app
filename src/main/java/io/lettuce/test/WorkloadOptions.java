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

    public <T> T getOption(String key, Class<T> type, T defaultValue) {
        return options.containsKey(key) ? getOption(key, type) : defaultValue;
    }

    public Integer getInteger(String key) {
        return getOption(key, Integer.class);
    }

    public Integer getInteger(String key, Integer defaultValue) {
        return getOption(key, Integer.class, defaultValue);
    }

    public Double getDouble(String key) {
        return getOption(key, Double.class);
    }

    public Double getDouble(String key, Double defaultValue) {
        return getOption(key, Double.class, defaultValue);
    }

    public String getString(String key) {
        return getOption(key, String.class);
    }

    public String getString(String key, String defaultValue) {
        return getOption(key, String.class, defaultValue);
    }

    public static WorkloadOptions create(Map<String, Object> options) {
        return new WorkloadOptions(options);
    }

}
