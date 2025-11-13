package io.lettuce.test.metrics;

import io.lettuce.test.config.TestRunProperties;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetricsReporterTest {

    private MetricsReporter metricsReporter;

    private CompositeMeterRegistry compositeMeterRegistry;

    private SimpleMeterRegistry simpleMeterRegistry;

    private TaskScheduler taskScheduler;

    private TestRunProperties testRunProperties;

    private MockClock mockClock;

    @BeforeEach
    void setUp() {
        mockClock = new MockClock();
        compositeMeterRegistry = new CompositeMeterRegistry();
        simpleMeterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, mockClock);
        compositeMeterRegistry.add(simpleMeterRegistry);

        taskScheduler = mock(TaskScheduler.class);

        testRunProperties = new TestRunProperties();
        testRunProperties.setAppName("test-app");
        testRunProperties.setInstanceId("test-instance");
        testRunProperties.setRunId("test-run");

        metricsReporter = new MetricsReporter(compositeMeterRegistry, simpleMeterRegistry, taskScheduler, testRunProperties);
    }

    @Test
    void testGetLatencyStatsContainsMaxLatencyFromRecordCommandLatency() {
        // Given: Record multiple command latencies with different durations
        recordLatency("GET", OperationStatus.SUCCESS, 100);
        recordLatency("SET", OperationStatus.SUCCESS, 50);
        recordLatency("DEL", OperationStatus.SUCCESS, 10);

        // When: Get latency stats
        MetricsReporter.LatencyStats latencyStats = metricsReporter.getLatencyStats();

        // Then: Max latency should be 100ms (the highest recorded latency)
        assertNotNull(latencyStats);
        assertEquals(100.0, latencyStats.max(), 0.1, "Max latency should be 100ms");
    }

    @Test
    void testGetLatencyStatsWithNoCommands() {
        // When: Get latency stats without recording any commands
        MetricsReporter.LatencyStats latencyStats = metricsReporter.getLatencyStats();

        // Then: Should return default values (all 0.0 when no commands recorded)
        assertNotNull(latencyStats);
        assertEquals(0.0, latencyStats.max(), "Max should be 0 when no commands recorded");
        assertEquals(0.0, latencyStats.median(), "Median should be 0 when no commands recorded");
        assertEquals(0.0, latencyStats.p95(), "P95 should be 0 when no commands recorded");
        assertEquals(0.0, latencyStats.p99(), "P99 should be 0 when no commands recorded");
    }

    @Test
    void testGetLatencyStatsMaxIsUpdatedWithNewHigherLatency() {
        // Given: Record initial command with 30ms latency
        recordLatency("GET", OperationStatus.SUCCESS, 30);
        MetricsReporter.LatencyStats stats1 = metricsReporter.getLatencyStats();
        double firstMax = stats1.max();

        // When: Record a new command with higher latency (80ms)
        recordLatency("SET", OperationStatus.SUCCESS, 80);
        MetricsReporter.LatencyStats stats2 = metricsReporter.getLatencyStats();
        double secondMax = stats2.max();

        // Then: Max should be updated to the higher value
        assertEquals(30.0, firstMax, 0.1, "First max should be 30ms");
        assertEquals(80.0, secondMax, 0.1, "Second max should be 80ms");
        assertTrue(secondMax > firstMax, "Second max should be greater than first max");

        // When: Record a new command with lower latency (30ms)
        recordLatency("SET", OperationStatus.SUCCESS, 30);
        MetricsReporter.LatencyStats stats3 = metricsReporter.getLatencyStats();

        assertEquals(80.0, secondMax, 0.1, "Second max should be 80ms");
    }

    @Test
    void testRecordCommandLatencyWithErrorStatus() {
        // Given: Record commands with both SUCCESS and ERROR status
        recordLatency("GET", OperationStatus.SUCCESS, 40);
        recordLatency("SET", OperationStatus.ERROR, 90);

        // When: Get latency stats
        MetricsReporter.LatencyStats latencyStats = metricsReporter.getLatencyStats();

        // Then: Max should include both success and error commands
        assertNotNull(latencyStats);
        assertEquals(90.0, latencyStats.max(), 0.1, "Max latency should include error commands and be 90ms");
    }

    /**
     * Helper method to record a command latency with a specific duration.
     *
     * @param command the command name
     * @param status the operation status
     * @param durationMillis the duration in milliseconds
     */
    private void recordLatency(String command, OperationStatus status, long durationMillis) {
        MetricsReporter.CommandKey commandKey = new MetricsReporter.CommandKey(command, status);
        Timer.Sample sample = Timer.start(simpleMeterRegistry);
        mockClock.add(Duration.ofMillis(durationMillis));
        metricsReporter.recordCommandLatency(commandKey, sample);
    }

}
