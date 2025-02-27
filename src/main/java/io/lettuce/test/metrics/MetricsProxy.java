package io.lettuce.test.metrics;

import io.lettuce.core.RedisFuture;
import io.micrometer.core.instrument.Timer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MetricsProxy<T> implements InvocationHandler {

    private final T target;

    private final MetricsReporter metricsReporter;

    public MetricsProxy(T target, MetricsReporter metricsReporter) {
        this.target = target;
        this.metricsReporter = metricsReporter;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String commandName = method.getName();

        Timer.Sample sample = metricsReporter.startCommandTimer();
        Object result;
        try {
            result = method.invoke(target, args);

            // TODO: we measure the time from the invocation of the method to the completion of the command.
            // Should we measure only the API call latency?
            // e.g. time from the invocation of the method to API call returning result.
            if (result instanceof RedisFuture<?> command) {

                command.whenComplete((res, ex) -> {
                    if (ex != null) {
                        metricsReporter.incrementCommandError(commandName);
                    }
                    metricsReporter.recordCommandLatency(commandName, sample);
                });
                return result;
            }

            metricsReporter.recordCommandLatency(commandName, sample);
            return result;
        } catch (InvocationTargetException ex) {
            metricsReporter.incrementCommandError(commandName);
            throw ex.getCause();
        }
    }

}
