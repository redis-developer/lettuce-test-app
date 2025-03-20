package io.lettuce.test.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class MetricsReporter {

    private static final Logger log = LoggerFactory.getLogger("simple-metrics-reporter");

    private final MeterRegistry meterRegistry;

    private final SimpleMeterRegistry simpleMeterRegistry;

    private final TaskScheduler taskScheduler;

    @Value("${simple.metrics.dumpRate:PT5S}")
    private Duration dumpRate;

    private final Map<String, Timer> commandLatencyTimers = new ConcurrentHashMap<>();

    private final Timer commandLatencyTotalTimer;

    private final Map<String, Counter> commandErrorCounters = new ConcurrentHashMap<>();

    private final Counter commandErrorTotalCounter;

    private final Timer connectionSuccessTimer;

    private final Timer connectionFailureTimer;

    private final Map<ConnectionKey, Counter> reconnectAttemptCounter = new ConcurrentHashMap<>();

    private final Counter reconnectAttemptTotalCounter;

    private final Map<ConnectionKey, Counter> reconnectFailureCounter = new ConcurrentHashMap<>();

    private final Counter reconnectFailureTotalCounter;

    private ScheduledFuture<?> scheduledFuture;

    public MetricsReporter(MeterRegistry meterRegistry, SimpleMeterRegistry simpleMeterRegistry,
            @Qualifier("metricReporterScheduler") TaskScheduler taskScheduler) {
        this.meterRegistry = meterRegistry;
        this.simpleMeterRegistry = simpleMeterRegistry;
        this.connectionSuccessTimer = Timer.builder("lettuce.connect.success")
                .description("Measures the duration and count of successful Redis connections").register(meterRegistry);
        this.connectionFailureTimer = Timer.builder("lettuce.connect.failure")
                .description("Measures the duration and count of failed Redis connection attempts").register(meterRegistry);

        this.commandLatencyTotalTimer = createCommandLatencyTotalTimer();
        this.commandErrorTotalCounter = createCommandErrorTotalCounter();
        this.reconnectAttemptTotalCounter = createReconnectAttemptTotalCounter();
        this.reconnectFailureTotalCounter = createReconnectFailedAttempTotalCounter();
        this.taskScheduler = taskScheduler;
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public Timer.Sample startConnectionTimer() {
        return Timer.start(meterRegistry);
    }

    Timer.Sample startCommandTimer() {
        return Timer.start(meterRegistry);
    }

    void recordCommandLatency(String commandName, Timer.Sample sample) {
        Timer timer = commandLatencyTimers.computeIfAbsent(commandName, this::createCommandLatencyTimer);
        long timeNs = sample.stop(timer);

        commandLatencyTotalTimer.record(Duration.ofNanos(timeNs));
    }

    void incrementCommandError(String commandName) {
        commandErrorCounters.computeIfAbsent(commandName, this::createCommandErrorCounter).increment();
        commandErrorTotalCounter.increment();
    }

    public void recordSuccessfulConnection(Timer.Sample sample) {
        sample.stop(connectionSuccessTimer);
    }

    public void recordFailedConnection(Timer.Sample sample) {
        sample.stop(connectionFailureTimer);
    }

    public void incrementReconnectAttempt(ConnectionKey connectionKey) {
        reconnectAttemptCounter.computeIfAbsent(connectionKey, this::createReconnectAttemptCounter).increment();
        reconnectAttemptTotalCounter.increment();
    }

    public void incrementReconnectFailure(ConnectionKey connectionKey) {
        reconnectFailureCounter.computeIfAbsent(connectionKey, this::createReconnectFailedAttempCounter).increment();
        reconnectFailureTotalCounter.increment();
    }

    private Timer createCommandLatencyTimer(String commandName) {
        return Timer.builder("redis.command.latency").description(
                "Measures the execution time of Redis commands from API invocation until command completion per command")
                .tag("command", commandName).register(meterRegistry);
    }

    private Timer createCommandLatencyTotalTimer() {
        return Timer.builder("redis.command.total.latency")
                .description("Measures the execution time of Redis commands from API invocation until command completion")
                .register(meterRegistry);
    }

    private Counter createCommandErrorCounter(String commandName) {
        return Counter.builder("redis.command.errors")
                .description("Counts the number of failed Redis command API calls that completed with an exception per command")
                .tag("command", commandName).register(meterRegistry);
    }

    private Counter createCommandErrorTotalCounter() {
        return Counter.builder("redis.command.total.errors")
                .description("Counts the number of failed Redis command API calls that completed with an exception")
                .register(meterRegistry);
    }

    private Counter createReconnectAttemptCounter(ConnectionKey connectionKey) {
        return Counter.builder("lettuce.reconnect.attempts")
                .description("Counts the number of Redis reconnect attempts per connection")
                .tag("epid", connectionKey.getEpId()).tag("local", connectionKey.getLocalAddress().toString())
                .tag("remote", connectionKey.getRemoteAddress().toString()).register(meterRegistry);
    }

    private Counter createReconnectAttemptTotalCounter() {
        return Counter.builder("lettuce.reconnect.total.attempts").description("Counts the number of Redis reconnect attempts")
                .register(meterRegistry);
    }

    private Counter createReconnectFailedAttempCounter(ConnectionKey connectionKey) {
        return Counter.builder("lettuce.reconnect.failures").description("Counts the number of failed Redis reconnect attempts")
                .tag("epid", connectionKey.getEpId()).tag("local", connectionKey.getLocalAddress().toString())
                .tag("remote", connectionKey.getRemoteAddress().toString()).register(meterRegistry);
    }

    private Counter createReconnectFailedAttempTotalCounter() {
        return Counter.builder("lettuce.reconnect.total.failures")
                .description("Counts the number of failed Redis reconnect attempts").register(meterRegistry);
    }

    @SuppressWarnings("unchecked")
    public <T> T withMetrics(T target) {
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(),
                new MetricsProxy<>(target, this));
    }

    // @Scheduled(fixedRateString = "${simple.metrics.dumpRate}")
    public void dumpMetrics() {
        if (simpleMeterRegistry == null) {
            return;
        }

        log.info("--- Metric Summary --- ");
        simpleMeterRegistry.getMeters().stream().sorted(Comparator.comparing((m) -> m.getId().getName())).forEach(meter -> {
            if (meter instanceof Counter) {
                Counter counter = (Counter) meter;
                log.info("Counter: " + counter.getId() + " value: " + counter.count());
            } else if (meter instanceof Timer) {
                Timer timer = (Timer) meter;
                log.info("Timer: " + timer.getId() + " count: " + timer.count() + " total time: "
                        + timer.totalTime(TimeUnit.MILLISECONDS));
            } else if (meter instanceof Gauge) {
                Gauge gauge = (Gauge) meter;
                log.info("Gauge: " + gauge.getId() + " value: " + gauge.value());
            }
        });
    }

    @PostConstruct
    public void startScheduledTask() {
        scheduledFuture = taskScheduler.scheduleAtFixedRate(this::dumpMetrics, dumpRate);
    }

    @PreDestroy
    public void shutdown() {
        log.info("MetricsReporter is shutting down.");
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            dumpMetrics();
            log.info("MetricsReporter is stopped.");
        }
    }

}
