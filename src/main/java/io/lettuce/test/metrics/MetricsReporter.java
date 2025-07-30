package io.lettuce.test.metrics;

import io.lettuce.core.LettuceVersion;
import io.lettuce.test.config.TestRunProperties;
import io.lettuce.test.workloads.BaseWorkload;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class MetricsReporter {

    private static final Logger log = LoggerFactory.getLogger("simple-metrics-reporter");

    public static final String REDIS_OPERATION_DURATION_TOTAL = "redis.operation.duration.total";

    public static final String REDIS_OPERATION_DURATION = "redis.operation.duration";

    public static final String REDIS_RECONNECTION_DURATION = "redis.reconnection.duration";

    public static final String REDIS_TEST_DURATION = "redis.test.duration";

    private final MeterRegistry meterRegistry;

    private final SimpleMeterRegistry simpleMeterRegistry;

    private final TaskScheduler taskScheduler;

    private final TestRunProperties testRunProperties;

    @Value("${simple.metrics.dumpRate:PT5S}")
    private Duration dumpRate;

    @Value("${logging.file.path:logs}")
    private String logPath;

    @Value("${runner.test.workload.type}")
    private String workloadType;

    private final Map<CommandKey, Timer> commandLatencyTimers = new ConcurrentHashMap<>();

    private final Timer commandLatencyTotalTimer;

    private final Map<String, Counter> commandErrorCounters = new ConcurrentHashMap<>();

    private final Map<CommandKey, Counter> commandTotalCounter = new ConcurrentHashMap<>();

    private final Counter commandErrorTotalCounter;

    private final Timer connectionSuccessTimer;

    private final Timer connectionFailureTimer;

    private final Map<ConnectionKey, Counter> reconnectAttemptCounter = new ConcurrentHashMap<>();

    private final Map<ReconnectAttemptKey, Counter> redisConnectionsTotal = new ConcurrentHashMap<>();

    private final Map<ConnectionKey, Counter> redisConnectionsDrops = new ConcurrentHashMap<>();

    private final Map<PubSubOpKey, Counter> pubSubOperationCounter = new ConcurrentHashMap<>();

    private final Counter reconnectAttemptTotalCounter;

    private final Map<ConnectionKey, Counter> reconnectFailureCounter = new ConcurrentHashMap<>();

    private final Counter reconnectFailureTotalCounter;

    private ScheduledFuture<?> scheduledFuture;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile Instant testRunStart;

    private volatile Instant testRunEnd;

    public MetricsReporter(MeterRegistry meterRegistry, SimpleMeterRegistry simpleMeterRegistry,
            @Qualifier("metricReporterScheduler") TaskScheduler taskScheduler, TestRunProperties testRunProperties) {
        this.meterRegistry = meterRegistry;
        this.simpleMeterRegistry = simpleMeterRegistry;
        this.testRunProperties = testRunProperties;
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

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    Timer.Sample startCommandTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTestDuration(Instant start, Instant end) {
        this.testRunStart = start;
        this.testRunEnd = end;
        Timer.builder(REDIS_TEST_DURATION).description("Measures the duration of the test run").register(meterRegistry)
                .record(Duration.between(start, end));
    }

    public record CommandKey(String commandName, OperationStatus status) {

    }

    void recordCommandLatency(CommandKey commandKey, Timer.Sample sample) {
        Timer timer = commandLatencyTimers.computeIfAbsent(commandKey, this::createCommandLatencyTimer);
        long timeNs = sample.stop(timer);

        commandLatencyTotalTimer.record(Duration.ofNanos(timeNs));

        Counter counter = commandTotalCounter.computeIfAbsent(commandKey, this::createCommandTotalCounter);
        counter.increment();
    }

    void incrementCommandError(String commandName) {
        commandErrorCounters.computeIfAbsent(commandName, this::createCommandErrorCounter).increment();
        commandErrorTotalCounter.increment();
    }

    public void recordWorkloadExecutionDuration(Timer.Sample sample, String workloadName, BaseWorkload.Status status) {
        Timer workloadDuration = Timer.builder("redis.workload.execution.duration")
                .description("Time taken to complete a workload").tag("workload", workloadName)
                .tag("status", status.name().toLowerCase()).register(meterRegistry);
        sample.stop(workloadDuration);
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

    public void incrementRedisConnectionAttempt(ReconnectAttemptKey reconnectAttempt) {
        redisConnectionsTotal.computeIfAbsent(reconnectAttempt, this::createRedisConnectionsTotal).increment();
    }

    public void incrementRedisConnectionDrops(ConnectionKey connectionKey) {
        redisConnectionsDrops.computeIfAbsent(connectionKey, this::createRedisConnectionsDrops).increment();
    }

    public void incrementReconnectFailure(ConnectionKey connectionKey) {
        reconnectFailureCounter.computeIfAbsent(connectionKey, this::createReconnectFailedAttempCounter).increment();
        reconnectFailureTotalCounter.increment();
    }

    public enum PubSubOperation {
        SUBSCRIBE, UNSUBSCRIBE, PUBLISH, RECEIVE
    }

    public record PubSubOpKey(String channel, PubSubOperation operation, String subsriberId, OperationStatus status) {

    }

    public void incrementPubSubOperation(PubSubOpKey pubSubOpKey) {
        pubSubOperationCounter.computeIfAbsent(pubSubOpKey, this::createPubSubOperationCounter).increment();
    }

    private Counter createPubSubOperationCounter(PubSubOpKey pubSubOpKey) {
        return Counter.builder("redis.pubsub.operations.total")
                .description("Total number of Redis pub/sub operations (publish and receive)")
                .tag("channel", pubSubOpKey.channel).tag("operation_type", pubSubOpKey.operation.name().toLowerCase())
                .tag("subscriber_id", pubSubOpKey.subsriberId).tag("status", pubSubOpKey.status.name().toLowerCase())
                .register(meterRegistry);
    }

    private Timer createCommandLatencyTimer(CommandKey commandKey) {
        return Timer.builder(REDIS_OPERATION_DURATION).description(
                "Measures the execution time of Redis commands from API invocation until command completion per command")
                .tag("command", commandKey.commandName).tag("status", commandKey.status.name().toLowerCase())
                .publishPercentileHistogram(true).publishPercentiles(0.5, 0.95, 0.99).register(meterRegistry);
    }

    private Timer createCommandLatencyTotalTimer() {
        return Timer.builder(REDIS_OPERATION_DURATION_TOTAL)
                .description("Measures the execution time of Redis commands from API invocation until command completion")
                .publishPercentileHistogram(true).publishPercentiles(0.5, 0.95, 0.99).register(meterRegistry);
    }

    private Counter createCommandErrorCounter(String commandName) {
        return Counter.builder("redis.command.errors")
                .description("Counts the number of failed Redis command API calls that completed with an exception per command")
                .tag("command", commandName).register(meterRegistry);
    }

    private Counter createCommandTotalCounter(CommandKey commandKey) {
        return Counter.builder("redis.operations.total")
                .description("Counts the number of total Redis command API calls completed successfully or with an error")
                .tag("command", commandKey.commandName).tag("status", commandKey.status.name().toLowerCase())
                .register(meterRegistry);
    }

    private Counter createCommandErrorTotalCounter() {
        return Counter.builder("redis.command.total.errors")
                .description("Counts the number of failed Redis command API calls that completed with an exception")
                .register(meterRegistry);
    }

    public record ReconnectAttemptKey(ConnectionKey connectionKey, OperationStatus status) {
    }

    private Counter createRedisConnectionsTotal(ReconnectAttemptKey reconnectAttempt) {
        ConnectionKey connectionKey = reconnectAttempt.connectionKey;
        OperationStatus status = reconnectAttempt.status;

        return Counter.builder("redis.connections.total")
                .description("Counts the number of Redis reconnect attempts per connection")
                .tag("status", status.name().toLowerCase()).tag("epid", connectionKey.getEpId())
                .tag("local", connectionKey.getLocalAddress().toString())
                .tag("remote", connectionKey.getRemoteAddress().toString()).register(meterRegistry);
    }

    private Counter createRedisConnectionsDrops(ConnectionKey connectionKey) {
        return Counter.builder("redis.connection.drops.total").description("Counts the number of disconnects")
                .tag("epid", connectionKey.getEpId()).tag("local", connectionKey.getLocalAddress().toString())
                .tag("remote", connectionKey.getRemoteAddress().toString()).register(meterRegistry);
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
        taskScheduler.scheduleAtFixedRate(this::dumpFinalResult, dumpRate);
    }

    @PreDestroy
    public void shutdown() {
        log.info("MetricsReporter is shutting down.");
        dumpFinalResult();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            dumpMetrics();
            log.info("MetricsReporter is stopped.");
        }
    }

    public void dumpFinalResult() {

        try {
            ObjectNode result = buildFinalResultJson();
            String jsonResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

            // Log to console
            log.info("=== FINAL TEST RESULTS ===");
            log.info(jsonResult);
            log.info("=== END FINAL TEST RESULTS ===");

            // Write to file
            writeResultsToFile(jsonResult);

        } catch (Exception e) {
            log.error("Error generating final result", e);
        }
    }

    private void writeResultsToFile(String jsonResult) {
        try {
            // Create logs directory if it doesn't exist
            Path logDir = Paths.get(logPath);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
                log.info("Created log directory: {}", logDir.toAbsolutePath());
            }

            // Write to test-run-summary.json
            Path summaryFile = logDir.resolve("test-run-summary.json");
            Files.write(summaryFile, jsonResult.getBytes());
            log.info("Final test results written to: {}", summaryFile.toAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to write final results to file", e);
        }
    }

    public String getFinalResultAsJson() {
        try {
            ObjectNode result = buildFinalResultJson();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error generating final result JSON", e);
            return "{}";
        }
    }

    private ObjectNode buildFinalResultJson() {
        ObjectNode result = objectMapper.createObjectNode();

        // Basic run information
        result.put("app_name", testRunProperties.getAppName());
        result.put("instance_id", testRunProperties.getInstanceId());
        result.put("run_id", testRunProperties.getRunId());
        result.put("version", LettuceVersion.getVersion());

        // Test duration
        result.put("workload_name", workloadType != null ? workloadType : "unknown");

        // Command counts and success rate
        long totalCommands = getTotalCmdCount();
        long successfulCommands = getSuccessfulCommandCount();
        long failedCommands = getFailedCommandCount();

        result.put("total_commands_count", totalCommands);
        result.put("successful_commands_count", successfulCommands);
        result.put("failed_commands_count", failedCommands);
        result.put("success_rate",
                String.format("%.2f%%", totalCommands > 0 ? (successfulCommands * 100.0 / totalCommands) : 0.0));

        // Reconnection metrics
        result.put("avg_reconnection_duration_ms", getAverageReconnectionDuration().orElseGet(() -> 0.0));

        // Run timestamps (as epoch seconds with decimals)
        result.put("run_start", testRunStart.getEpochSecond());
        result.put("run_end", testRunEnd.getEpochSecond());

        // Latency statistics
        result.put("min_latency_ms", 0);
        result.put("max_latency_ms", getMaxLatency().orElse(0.0));
        result.put("median_latency_ms", getMedianLatency().orElse(0.0));
        result.put("p95_latency_ms", getP95Latency().orElse(0.0));
        result.put("p99_latency_ms", getP99().orElse(0.0));

        return result;
    }

    private OptionalDouble getMedianLatency() {
        return simpleMeterRegistry.find(REDIS_OPERATION_DURATION_TOTAL).timers().stream()
                .mapToDouble(m -> m.percentile(0.5, TimeUnit.MILLISECONDS)).average();
    }

    private OptionalDouble getP95Latency() {
        return simpleMeterRegistry.find(REDIS_OPERATION_DURATION_TOTAL).timers().stream()
                .mapToDouble(m -> m.percentile(0.95, TimeUnit.MILLISECONDS)).average();
    }

    private OptionalDouble getP99() {
        return simpleMeterRegistry.find(REDIS_OPERATION_DURATION_TOTAL).timers().stream()
                .mapToDouble(m -> m.percentile(0.99, TimeUnit.MILLISECONDS)).average();
    }

    private OptionalDouble getAverageReconnectionDuration() {
        return simpleMeterRegistry.find(REDIS_RECONNECTION_DURATION).timers().stream()
                .mapToDouble(m -> m.mean(TimeUnit.MILLISECONDS)).average();

    }

    private OptionalDouble getMaxLatency() {
        return simpleMeterRegistry.find(REDIS_OPERATION_DURATION).timers().stream()
                .mapToDouble(m -> m.max(TimeUnit.MILLISECONDS)).max();
    }

    private long getTotalCmdCount() {
        return simpleMeterRegistry.find(REDIS_OPERATION_DURATION).timers().stream().mapToLong(Timer::count).sum();
    }

    private long getSuccessfulCommandCount() {
        return simpleMeterRegistry.find(REDIS_OPERATION_DURATION).tag("status", "success").timers().stream()
                .mapToLong(Timer::count).sum();
    }

    private long getFailedCommandCount() {
        return simpleMeterRegistry.find(REDIS_OPERATION_DURATION).tag("status", "error").timers().stream()
                .mapToLong(Timer::count).sum();
    }

    public static CommandKey cmdKeyOk(String commandName) {
        return new CommandKey(commandName, OperationStatus.SUCCESS);
    }

    public static CommandKey cmdKeyError(String commandName, Throwable throwable) {
        return new CommandKey(commandName, OperationStatus.ERROR);
    }

}
