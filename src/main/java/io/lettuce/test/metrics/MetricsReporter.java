package io.lettuce.test.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsReporter {

    private final MeterRegistry meterRegistry;

    private final Map<String, Timer> commandLatencyTimers = new ConcurrentHashMap<>();

    private final Map<String, Counter> commandErrorCounters = new ConcurrentHashMap<>();

    private final Timer connectionSuccessTimer;

    private final Timer connectionFailureTimer;

    private final Map<ConnectionKey, Counter> reconnectAttemptCounter = new ConcurrentHashMap<>();

    private final Map<ConnectionKey, Counter> reconnectFailureCounter = new ConcurrentHashMap<>();

    public MetricsReporter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.connectionSuccessTimer = Timer.builder("lettuce.connect.success")
                .description("Measures the duration and count of successful Redis connections").register(meterRegistry);
        this.connectionFailureTimer = Timer.builder("lettuce.connect.failure")
                .description("Measures the duration and count of failed Redis connection attempts").register(meterRegistry);
    }

    public Timer.Sample startConnectionTimer() {
        return Timer.start(meterRegistry);
    }

    Timer.Sample startCommandTimer() {
        return Timer.start(meterRegistry);
    }

    void recordCommandLatency(String commandName, Timer.Sample sample) {
        Timer timer = commandLatencyTimers.computeIfAbsent(commandName, this::createCommandLatencyTimer);
        sample.stop(timer);
    }

    void incrementCommandError(String commandName) {
        commandErrorCounters.computeIfAbsent(commandName, this::createCommandErrorCounter).increment();
    }

    public void recordSuccessfulConnection(Timer.Sample sample) {
        sample.stop(connectionSuccessTimer);
    }

    public void recordFailedConnection(Timer.Sample sample) {
        sample.stop(connectionFailureTimer);
    }

    public void incrementReconnectAttempt(ConnectionKey connectionKey) {
        reconnectAttemptCounter.computeIfAbsent(connectionKey, this::createReconnectAttemptCounter).increment();
    }

    public void incrementReconnectFailure(ConnectionKey connectionKey) {
        reconnectFailureCounter.computeIfAbsent(connectionKey, this::createReconnectFailedAttempCounter).increment();
    }

    private Timer createCommandLatencyTimer(String commandName) {
        return Timer.builder("redis.command.latency")
                .description("Measures the execution time of Redis commands from API invocation until command completion")
                .tag("command", commandName).register(meterRegistry);
    }

    private Counter createCommandErrorCounter(String commandName) {
        return Counter.builder("redis.command.errors")
                .description("Counts the number of failed Redis command API calls that completed with an exception")
                .tag("command", commandName).register(meterRegistry);
    }

    private Counter createReconnectAttemptCounter(ConnectionKey connectionKey) {
        return Counter.builder("lettuce.reconnect.attempts").description("Counts the number of Redis reconnect attempts")
                .tag("epid", connectionKey.getEpId()).tag("local", connectionKey.getLocalAddress().toString())
                .tag("remote", connectionKey.getRemoteAddress().toString()).register(meterRegistry);
    }

    private Counter createReconnectFailedAttempCounter(ConnectionKey connectionKey) {
        return Counter.builder("lettuce.reconnect.failures").description("Counts the number of failed Redis reconnect attempts")
                .tag("epid", connectionKey.getEpId()).tag("local", connectionKey.getLocalAddress().toString())
                .tag("remote", connectionKey.getRemoteAddress().toString()).register(meterRegistry);
    }

}
