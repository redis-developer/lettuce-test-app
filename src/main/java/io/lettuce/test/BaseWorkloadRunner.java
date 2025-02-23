package io.lettuce.test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.test.workloads.BaseWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class BaseWorkloadRunner<C, Conn extends StatefulConnection<?, ?>> implements AutoCloseable {

    private static final long SHUTDOWN_DELAY = Duration.ofSeconds(1).toSeconds();

    private static final Logger log = LoggerFactory.getLogger(BaseWorkloadRunner.class);

    Config config;

    ExecutorService executor = Executors.newCachedThreadPool();

    Workloads submittedWorkloads = new Workloads();

    public BaseWorkloadRunner(Config config) {
        this.config = config;
    }

    public final void run() {
        RedisURI redisUri = buildRedisUri(config);

        List<C> clients = new ArrayList<>();
        List<List<Conn>> connections = new ArrayList<>();

        // Create the specified number of client instances
        for (int i = 0; i < config.test.clients; i++) {
            C client = createClient(redisUri, config);
            clients.add(client);

            List<Conn> clientConnections = new ArrayList<>();
            for (int j = 0; j < config.test.connectionsPerClient; j++) {
                clientConnections.add(createConnection(client, config));
            }
            connections.add(clientConnections);
        }

        executeWorkloads(clients, connections);
    }

    private void executeWorkloads(List<C> clients, List<List<Conn>> connections) {
        for (int i = 0; i < config.test.clients; i++) {
            for (Conn conn : connections.get(i)) {
                BaseWorkload workload = createWorkload(clients.get(i), conn, config);
                if (workload != null) {
                    submit(workload);
                } else {
                    log.error("Unsupported workload." + config.test.workload.getType());
                    throw new IllegalArgumentException("Unsupported workload." + config.test.workload.getType());
                }
            }
        }
    }

    protected abstract BaseWorkload createWorkload(C client, Conn connection, Config config);

    protected abstract C createClient(RedisURI redisUri, Config config);

    protected abstract Conn createConnection(C client, Config config);

    protected CompletableFuture<?> submit(BaseWorkload task) {
        ContinousWorkload workload = new ContinousWorkload(task, config.test.workload);
        CompletableFuture<Void> future = CompletableFuture.runAsync(workload, executor).whenComplete((result, throwable) -> {
            submittedWorkloads.remove(workload);
            if (throwable != null) {
                log.error("Workload failed: " + throwable.getMessage());
            }
        });
        submittedWorkloads.add(workload);
        return future;
    }

    protected RedisURI buildRedisUri(Config config) {
        RedisURI.Builder builder = RedisURI.builder();
        builder.withHost(config.redis.host).withPort(config.redis.port).withDatabase(config.redis.database)
                .withSsl(config.redis.useTls).withVerifyPeer(config.redis.verifyPeer);

        if (config.redis.password != null) {
            builder.withPassword(config.redis.password.toCharArray());
        }

        if (config.redis.clientName != null) {
            builder.withClientName(config.redis.clientName);
        }

        if (config.redis.timeout != null) {
            builder.withTimeout(Duration.ofMillis(config.redis.timeout));
        }

        return builder.build();
    }

    public void close() {
        submittedWorkloads.stop();

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    public void awaitTermination() {

        long maxTime = config.test.workload.getMaxDuration().toSeconds();
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxTime + SHUTDOWN_DELAY);

        // Wait until the queue is empty or timeout is reached
        while (!submittedWorkloads.isEmpty() && System.currentTimeMillis() < deadline) {
            log.debug("Waiting for tasks to finish and queue to be empty...");
            try {
                Thread.sleep(500); // Check every 500 ms
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for tasks to complete", e);
            }
        }

        executor.shutdownNow();
    }

}