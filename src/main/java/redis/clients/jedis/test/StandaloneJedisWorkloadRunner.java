package redis.clients.jedis.test;

import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.DefaultWorkloadOptions;
import io.lettuce.test.ContinuousWorkload;
import io.lettuce.test.config.WorkloadRunnerConfig;
import io.lettuce.test.config.WorkloadRunnerConfig.WorkloadConfig;
import io.lettuce.test.metrics.MetricsReporter;
import io.lettuce.test.workloads.BaseWorkload;
import redis.clients.jedis.test.workloads.JedisRedisCommandsWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.JedisClientConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Standalone Jedis workload runner that mirrors the scheduling/execution behavior of the Lettuce runner.
 */
public class StandaloneJedisWorkloadRunner implements JedisRunner {

    private static final Logger log = LoggerFactory.getLogger(StandaloneJedisWorkloadRunner.class);

    private final WorkloadRunnerConfig config;

    private final MetricsReporter metricsReporter;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final List<UnifiedJedis> clients = new ArrayList<>();

    public StandaloneJedisWorkloadRunner(WorkloadRunnerConfig config, MetricsReporter metricsReporter) {
        this.config = Objects.requireNonNull(config);
        this.metricsReporter = Objects.requireNonNull(metricsReporter);
    }

    public void run() {
        List<List<UnifiedJedis>> connections = new ArrayList<>();

        for (int i = 0; i < config.getTest().getClients(); i++) {
            List<UnifiedJedis> clientConnections = new ArrayList<>();
            for (int j = 0; j < config.getTest().getConnectionsPerClient(); j++) {
                UnifiedJedis uj = createUnified(config.getRedis());
                clients.add(uj);
                clientConnections.add(uj);
            }
            connections.add(clientConnections);
        }

        metricsReporter.recordStartTime();
        try {
            CompletableFuture<Void> all = executeWorkloads(connections);
            all.whenComplete((v, t) -> metricsReporter.recordEndTime())
                    .thenRun(() -> log.info("All Jedis tasks completed. Exiting..."));
        } finally {
            executor.shutdown();
        }
    }

    private CompletableFuture<Void> executeWorkloads(List<List<UnifiedJedis>> connections) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        WorkloadConfig workloadConfig = config.getTest().getWorkload();

        for (int i = 0; i < connections.size(); i++) {
            for (UnifiedJedis conn : connections.get(i)) {
                for (int j = 0; j < config.getTest().getThreadsPerConnection(); j++) {
                    BaseWorkload workload = createWorkload(conn, workloadConfig);
                    workload.metricsReporter(metricsReporter);
                    futures.add(submit(workload, workloadConfig));
                }
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private BaseWorkload createWorkload(UnifiedJedis conn, WorkloadConfig config) {
        CommonWorkloadOptions options = DefaultWorkloadOptions.create(config.getOptions());
        return switch (config.getType()) {
            case "redis_commands" -> new JedisRedisCommandsWorkload(conn, options);
            default -> throw new IllegalArgumentException(
                    "Invalid workload specified for Jedis standalone mode: " + config.getType());
        };
    }

    // no-op helper retained for compatibility

    private CompletableFuture<ContinuousWorkload> submit(BaseWorkload task, WorkloadConfig config) {
        ContinuousWorkload cw = new ContinuousWorkload(task, config);
        return CompletableFuture.runAsync(cw::run, executor).thenApply(v -> cw);
    }

    private UnifiedJedis createUnified(WorkloadRunnerConfig.RedisConfig rc) {
        JedisClientConfig clientCfg = DefaultJedisClientConfig.builder().user(rc.getUsername()).password(rc.getPassword())
                .database(rc.getDatabase()).clientName(rc.getClientName()).ssl(rc.isUseTls())
                .timeoutMillis(rc.getTimeout() != null ? (int) rc.getTimeout().toMillis() : 2000).build();
        return new UnifiedJedis(new HostAndPort(rc.getHost(), rc.getPort()), clientCfg);
    }

    @Override
    public void close() {
        log.info("JedisWorkloadRunner stopping...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        for (UnifiedJedis c : clients) {
            try {
                c.close();
            } catch (Exception ignore) {
            }
        }
        log.info("JedisWorkloadRunner stopped.");
    }

}
