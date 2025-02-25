package io.lettuce.test.metrics;

import io.lettuce.core.RedisFuture;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MetricsProxy<T> implements InvocationHandler {

    private final T target;

    private final MeterRegistry meterRegistry;

    private final Map<String, Timer> latencyTimers = new ConcurrentHashMap<>();

    private final Map<String, Counter> commandErrors = new ConcurrentHashMap<>();

    public MetricsProxy(T target, MeterRegistry meterRegistry) {
        this.target = target;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String commandName = method.getName();

        Timer latencyTimer = latencyTimers.computeIfAbsent(commandName, this::latencyTimer);
        Counter errorCounter = commandErrors.computeIfAbsent(commandName, this::errorCounter);

        long start = System.nanoTime();
        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            errorCounter.increment();
            throw ex.getCause();
        }

        // TODO: we measure the time from the invocation of the method to the completion of the command.
        // Should we measure only the API call latency?
        // e.g. time from the invocation of the method to API call returning result.
        if (result instanceof RedisFuture<?> command) {

            command.whenComplete((res, ex) -> {
                if (ex != null) {
                    errorCounter.increment(); // Increment error counter on failure
                }
                latencyTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            });

            return result;
        }

        latencyTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        return result;
    }

    private Timer latencyTimer(String commandName) {

        return Timer.builder("redis.command.latency").tag("command", commandName).register(meterRegistry);
    }

    private Counter errorCounter(String commandName) {

        return Counter.builder("redis.command.errors").tag("command", commandName).register(meterRegistry);
    }

}
