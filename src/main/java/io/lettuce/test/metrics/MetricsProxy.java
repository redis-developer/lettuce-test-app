package io.lettuce.test.metrics;

import io.lettuce.core.RedisFuture;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MetricsProxy<T> implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(MetricsProxy.class);

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
                        log.error("Command failed", ex);
                    }
                    metricsReporter.recordCommandLatency(commandName, sample);
                });
                return result;
            }

            metricsReporter.recordCommandLatency(commandName, sample);
            return result;
        } catch (InvocationTargetException ex) {
            metricsReporter.incrementCommandError(commandName);
            log.error("Command failed", ex.getCause());
            throw ex.getCause();
        }
    }

}
