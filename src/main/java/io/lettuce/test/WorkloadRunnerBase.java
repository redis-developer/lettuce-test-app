package io.lettuce.test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.test.Config.ClientOptionsConfig;
import io.lettuce.test.Config.WorkloadConfig;
import io.lettuce.test.workloads.BaseWorkload;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class WorkloadRunnerBase<C , Conn extends StatefulConnection<?, ?>> implements AutoCloseable {

    private static final long SHUTDOWN_DELAY = Duration.ofSeconds(1).toSeconds();

    private static final Logger log = LoggerFactory.getLogger(WorkloadRunnerBase.class);

    private final MeterRegistry meterRegistry;

    Config config;

    ExecutorService executor = Executors.newCachedThreadPool();

    Workloads submittedWorkloads = new Workloads();

    public WorkloadRunnerBase(Config config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;
    }

    public final void run() {
        RedisURI redisUri = buildRedisUri(config);

        List<C> clients = new ArrayList<>();
        List<List<Conn>> connections = new ArrayList<>();

        // Create the specified number of client instances
        for (int i = 0; i < config.test.clients; i++) {
            C client = tryCreateClient(redisUri, config);
            if (client != null) {
                clients.add(client);

                List<Conn> clientConnections = new ArrayList<>();
                for (int j = 0; j < config.test.connectionsPerClient; j++) {
                    Conn conn = tryCreateConnection(client, config);
                    if (conn != null) {
                        clientConnections.add(conn);
                    }
                }
                connections.add(clientConnections);
            }
        }

        executeWorkloads(clients, connections);
    }

    private void executeWorkloads(List<C> clients, List<List<Conn>> connections) {
        for (int i = 0; i < clients.size(); i++) {
            for (Conn conn : connections.get(i)) {
                C client = clients.get(i);
                BaseWorkload workload = createWorkload(client, conn, config.test.workload);
                workload.meterRegistry(meterRegistry);
                BaseWorkload withErrorHandler =  withErrorHandler(workload, client, conn);
                if (workload != null) {
                    submit(withErrorHandler, config.test.workload);
                } else {
                    log.error("Unsupported workload." + config.test.workload.getType());
                    throw new IllegalArgumentException("Unsupported workload." + config.test.workload.getType());
                }
            }
        }
    }

    private  C tryCreateClient(RedisURI redisUri, Config config){
        try {
            return createClient(redisUri, config);
        } catch (Exception e) {
            log.error("Failed to create client: " + e.getMessage());
            return null;
        }
    }

    private  Conn tryCreateConnection(C client, Config config){
        try {
            return createConnection(client, config);
        } catch (Exception e) {
            log.error("Failed to create connection: " + e.getMessage());
            return null;
        }
    }

    protected abstract BaseWorkload createWorkload(C client, Conn connection, WorkloadConfig config);

    protected abstract C createClient(RedisURI redisUri, Config config);

    protected abstract Conn createConnection(C client, Config config);

    protected CompletableFuture<?> submit(BaseWorkload task, WorkloadConfig config) {
        ContinousWorkload workload = new ContinousWorkload(task, config);
        CompletableFuture<Void> future = CompletableFuture.runAsync(workload, executor).whenComplete((result, throwable) -> {
            submittedWorkloads.remove(workload);
            if (throwable != null) {
                log.error("Workload failed: " + throwable.getMessage());
            }
        });
        submittedWorkloads.add(workload);
        return future;
    }

    private BaseWorkload withErrorHandler(BaseWorkload task, C client, Conn conn) {
        return new BaseWorkload() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Exception e) {
                    // Note: Use client and conn reference to track, which client and connection caused the error
                    // could not find other means to identify the client and connection
                    log.error("Error client: {} conn: {}",  client , conn,e);
                    throw e;
                }
            }
        };
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

    protected ClientOptions createClientOptions(ClientOptionsConfig config) {
        ClientOptions.Builder builder =  ClientOptions.builder();
        if (config != null ) {
            if (config.autoReconnect != null) {
                builder.autoReconnect(config.autoReconnect);
            }
            if (config.pingBeforeActivate != null) {
                builder.pingBeforeActivateConnection(config.pingBeforeActivate);
            }

            if (config.socketOptions != null) {
                SocketOptions.Builder socketOptions = SocketOptions.builder();
                applySocketOptions(socketOptions, config.socketOptions);
                builder.socketOptions(socketOptions.build());
            }

            if (config.disconnectedBehavior != null) {
                builder.disconnectedBehavior(ClientOptions.DisconnectedBehavior.valueOf(config.disconnectedBehavior));
            }
        }

        return builder.build();
    }

    private void applySocketOptions(SocketOptions.Builder builder, Config.SocketOptionsConfig config) {
        if (config.keepAliveOptions != null) {
            SocketOptions.KeepAliveOptions.Builder keepAliveOptions = SocketOptions.KeepAliveOptions.builder();
            applyKeepAliveOptions(keepAliveOptions, config.keepAliveOptions);
            builder.keepAlive(keepAliveOptions.build());
        }
        if (config.tcpUserTimeoutOptions != null) {
            SocketOptions.TcpUserTimeoutOptions.Builder tcpUserTimeoutOptions = SocketOptions.TcpUserTimeoutOptions.builder();
            applyTcpUserTimeoutOptions(tcpUserTimeoutOptions, config.tcpUserTimeoutOptions);
            builder.tcpUserTimeout(tcpUserTimeoutOptions.build());
        }
    }

    private void applyTcpUserTimeoutOptions(
            SocketOptions.TcpUserTimeoutOptions.Builder builder, Config.TcpUserTimeoutOptionsConfig config) {
        if (config.tcpUserTimeoutMs != null) {
            builder.tcpUserTimeout(Duration.ofMillis(config.tcpUserTimeoutMs));
        }
        if (config.enabled != null) {
            builder.enable();
        }
    }

    private void applyKeepAliveOptions(SocketOptions.KeepAliveOptions.Builder builder, Config.KeepAliveOptionsConfig config) {
        if (config.intervalMs != null) {
            builder.interval(Duration.ofMillis(config.intervalMs));
        }

        if (config.idleMs != null) {
            builder.idle(Duration.ofMillis(config.idleMs));
        }

        if (config.count != null) {
            builder.count(config.count);
        }
    }

}